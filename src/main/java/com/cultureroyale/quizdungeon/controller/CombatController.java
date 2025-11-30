package com.cultureroyale.quizdungeon.controller;

import com.cultureroyale.quizdungeon.model.Boss;
import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.model.dto.Choice;
import com.cultureroyale.quizdungeon.model.enums.HelpLevel;
import com.cultureroyale.quizdungeon.repository.BossRepository;
import com.cultureroyale.quizdungeon.repository.UserRepository;
import com.cultureroyale.quizdungeon.service.CombatService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/combat")
public class CombatController {

    @Autowired
    private CombatService combatService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BossRepository bossRepository;

    @GetMapping("/start/{bossId}")
    public String startCombat(@PathVariable Long bossId, Authentication authentication, HttpSession session) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        Boss boss = bossRepository.findById(bossId).orElseThrow();

        // Clear any previous combat status
        session.removeAttribute("combat_status");
        session.removeAttribute("combat_current_choices");

        combatService.startCombat(user, boss, session);

        // Redirect to the battle page (using existing quiz-battle template for now, or
        // modified one)
        // We need to make sure the view has the data it needs.
        // For now, let's redirect to a method that populates the view.
        return "redirect:/combat/battle";
    }

    @GetMapping("/battle")
    public String showBattle(Authentication authentication, HttpSession session, Model model) {
        Boss boss = (Boss) session.getAttribute("combat_boss");
        if (boss == null)
            return "redirect:/roadmap"; // Session expired or invalid

        // Check combat status from session
        String status = (String) session.getAttribute("combat_status");
        if ("VICTORY".equals(status)) {
            return "redirect:/combat/victory";
        }
        if ("DEFEAT".equals(status)) {
            return "redirect:/combat/defeat";
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        // Calculate user attack based on boss max HP (logic from CombatService)
        // baseDamage = boss.getMaxHp() / 20.0
        int userAttack = (int) (boss.getMaxHp() / 20.0);
        user.setAttack(userAttack);

        // Check if combat is already over (from previous turn)
        // We can check HP in session
        // Integer bossHp = (Integer) session.getAttribute("combat_boss_current_hp");
        // if (bossHp != null && bossHp <= 0) {
        // return "redirect:/combat/victory";
        // }
        // if (user.getCurrentHp() <= 0) {
        // return "redirect:/combat/defeat";
        // }

        model.addAttribute("user", user);

        // Flatten Boss attributes for the view
        model.addAttribute("bossName", boss.getName());
        model.addAttribute("bossHp", session.getAttribute("combat_boss_current_hp"));
        model.addAttribute("bossMaxHp", boss.getMaxHp());
        model.addAttribute("bossAtk", boss.getAttack());
        model.addAttribute("bossImage", boss.getImagePath());

        // Get current question
        com.cultureroyale.quizdungeon.model.Question question = combatService.getCurrentQuestion(session);
        if (question == null) {
            return "redirect:/combat/defeat";
        }

        model.addAttribute("question", question.getQuestionText());

        // Choices are loaded via AJAX now, so we don't need to pass them here
        model.addAttribute("choices", new java.util.ArrayList<>());
        model.addAttribute("selectedChoiceId", null);

        return "quiz-battle";
    }

    @PostMapping("/submit")
    public String submitAnswer(@RequestParam(required = false) String answer,
            @RequestParam(required = false) Long choiceId,
            @RequestParam(defaultValue = "FOUR_CHOICES") HelpLevel helpLevel,
            Authentication authentication, HttpSession session, Model model) {

        Boss boss = (Boss) session.getAttribute("combat_boss");
        if (boss == null)
            return "redirect:/roadmap";

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        boolean isCorrect = false;
        Long submittedId = choiceId;

        // Retrieve choices from session to display result
        @SuppressWarnings("unchecked")
        java.util.List<Choice> choices = (java.util.List<Choice>) session.getAttribute("combat_current_choices");

        if (choices == null) {
            // Try to get question to reconstruct choices for feedback if needed
            com.cultureroyale.quizdungeon.model.Question question = combatService.getCurrentQuestion(session);
            if (question != null) {
                choices = new java.util.ArrayList<>();
                choices.add(new Choice(1L, question.getCorrectAnswer()));
                choices.add(new Choice(2L, question.getBadAnswer1()));
                choices.add(new Choice(3L, question.getBadAnswer2()));
                choices.add(new Choice(4L, question.getBadAnswer3()));
            } else {
                return "redirect:/combat/battle";
            }
        }

        if (answer != null && !answer.trim().isEmpty()) {
            // Text submission
            com.cultureroyale.quizdungeon.model.Question question = combatService.getCurrentQuestion(session);
            if (question != null) {
                // Compare answer (case insensitive)
                if (question.getCorrectAnswer().equalsIgnoreCase(answer.trim())) {
                    isCorrect = true;
                } else {
                    isCorrect = false;
                }

                CombatService.CombatResult result = combatService.processTurn(user, boss, isCorrect, helpLevel,
                        session);

                if (result.status == CombatService.CombatStatus.VICTORY) {
                    session.setAttribute("combat_status", "VICTORY");
                    combatService.handleVictory(user, boss);
                } else if (result.status == CombatService.CombatStatus.DEFEAT) {
                    session.setAttribute("combat_status", "DEFEAT");
                    combatService.handleDefeat(user);
                }

                model.addAttribute("userAnswer", answer);
                model.addAttribute("correctAnswer", question.getCorrectAnswer());
                model.addAttribute("correct", isCorrect);

                return "fragments/quiz-text-result :: result";
            }
        } else {
            // Choice submission
            isCorrect = (choiceId != null && choiceId == 1L);

            CombatService.CombatResult result = combatService.processTurn(user, boss, isCorrect, helpLevel, session);

            if (result.status == CombatService.CombatStatus.VICTORY) {
                session.setAttribute("combat_status", "VICTORY");
                combatService.handleVictory(user, boss);
            } else if (result.status == CombatService.CombatStatus.DEFEAT) {
                session.setAttribute("combat_status", "DEFEAT");
                combatService.handleDefeat(user);
            }

            model.addAttribute("choices", choices);
            model.addAttribute("selectedChoiceId", null);
            model.addAttribute("submittedChoiceId", submittedId != null ? submittedId : -1L);
            model.addAttribute("correctChoiceId", 1L);
            model.addAttribute("correct", isCorrect);

            return "fragments/quiz-choices-list :: list";
        }
        return "redirect:/combat/battle"; // Fallback in case question is null for text submission
    }

    @GetMapping("/choices")
    public String getChoices(@RequestParam(defaultValue = "4") int count, HttpSession session, Model model) {
        com.cultureroyale.quizdungeon.model.Question question = combatService.getCurrentQuestion(session);
        if (question == null) {
            return ""; // Or handle error
        }

        java.util.List<Choice> choices = new java.util.ArrayList<>();
        choices.add(new Choice(1L, question.getCorrectAnswer()));
        choices.add(new Choice(2L, question.getBadAnswer1()));
        if (count > 2) {
            choices.add(new Choice(3L, question.getBadAnswer2()));
            choices.add(new Choice(4L, question.getBadAnswer3()));
        }
        java.util.Collections.shuffle(choices);

        // Store choices in session for result display
        session.setAttribute("combat_current_choices", choices);

        model.addAttribute("choices", choices);
        model.addAttribute("selectedChoiceId", null);
        model.addAttribute("submittedChoiceId", null); // Ensure these are null for initial load
        model.addAttribute("correctChoiceId", null);

        return "fragments/quiz-choices-list :: list";
    }

    @GetMapping("/victory")
    public String victory(HttpSession session, Model model) {
        Boss boss = (Boss) session.getAttribute("combat_boss");
        if (boss != null) {
            model.addAttribute("goldReward", boss.getGoldReward());
        } else {
            model.addAttribute("goldReward", 0);
        }
        return "boss-victory";
    }

    @GetMapping("/defeat")
    public String defeat() {
        return "boss-fail";
    }
}
