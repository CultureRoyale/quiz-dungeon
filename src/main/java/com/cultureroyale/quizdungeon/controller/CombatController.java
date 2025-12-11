package com.cultureroyale.quizdungeon.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import com.cultureroyale.quizdungeon.model.Boss;
import com.cultureroyale.quizdungeon.model.Question;
import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.model.dto.Choice;
import com.cultureroyale.quizdungeon.model.dto.TurnResult;
import com.cultureroyale.quizdungeon.model.enums.HelpLevel;
import com.cultureroyale.quizdungeon.repository.BossRepository;
import com.cultureroyale.quizdungeon.repository.UserRepository;
import com.cultureroyale.quizdungeon.service.CombatService;
import com.cultureroyale.quizdungeon.service.QuestionService;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/combat")
public class CombatController {

    @Autowired
    private CombatService combatService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BossRepository bossRepository;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private QuestionService questionService;

    @GetMapping("/start/{bossId}")
    public String startCombat(@PathVariable Long bossId, Authentication authentication, HttpSession session) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        Boss boss = bossRepository.findById(bossId).orElseThrow();

        // Clear any previous combat status
        session.removeAttribute("combat_status");
        session.removeAttribute("combat_current_choices");
        session.removeAttribute("combat_help_level");

        combatService.startCombat(user, boss, session);

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
        int userAttack = combatService.calculateUserBaseAttack(boss);
        user.setAttack(userAttack);

        model.addAttribute("user", user);

        // Flatten Boss attributes for the view
        model.addAttribute("bossName", boss.getName());
        model.addAttribute("bossHp", session.getAttribute("combat_boss_current_hp"));
        model.addAttribute("bossMaxHp", boss.getMaxHp());
        model.addAttribute("bossAtk", boss.getAttack());
        model.addAttribute("bossImage", boss.getImagePath());

        // Get current question
        Question question = combatService.getCurrentQuestion(session);
        if (question == null) {
            return "redirect:/combat/defeat";
        }

        model.addAttribute("question", question.getQuestionText());

        return "quiz-battle";
    }

    @PostMapping("/submit")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitAnswer(@RequestParam(required = false) String answer,
            @RequestParam(required = false) Long choiceId,
            Authentication authentication, HttpSession session, Locale locale,
            HttpServletRequest request, HttpServletResponse response) {

        Map<String, Object> jsonResponse = new HashMap<>();

        Boss boss = (Boss) session.getAttribute("combat_boss");
        if (boss == null) {
            jsonResponse.put("redirectUrl", "/roadmap");
            return ResponseEntity.ok(jsonResponse);
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        boolean isCorrect;
        Long submittedId = choiceId;

        // Retrieve choices from session to display result
        @SuppressWarnings("unchecked")
        List<Choice> choices = (List<Choice>) session.getAttribute("combat_current_choices");

        if (choices == null) {
            // Try to get question to reconstruct choices for feedback if needed
            Question question = combatService.getCurrentQuestion(session);
            if (question != null) {
                choices = new ArrayList<>();
                choices.add(new Choice(1L, question.getCorrectAnswer()));
                choices.add(new Choice(2L, question.getBadAnswer1()));
                choices.add(new Choice(3L, question.getBadAnswer2()));
                choices.add(new Choice(4L, question.getBadAnswer3()));
            } else {
                jsonResponse.put("redirectUrl", "/combat/battle");
                return ResponseEntity.ok(jsonResponse);
            }
        }

        Question currentQuestion = combatService.getCurrentQuestion(session);

        if (currentQuestion != null) {
            isCorrect = questionService.verifyAnswer(answer, choiceId, currentQuestion.getCorrectAnswer());
        } else {
            // Fallback if question is null
            isCorrect = false;
        }

        // Check for backend abuse: if help was requested, enforce it
        HelpLevel helpLevel = (HelpLevel) session.getAttribute("combat_help_level");
        if (helpLevel == null) {
            helpLevel = HelpLevel.NO_HELP;
        }

        TurnResult result = combatService.processTurn(user, boss, isCorrect, helpLevel, session);

        // Prepare context for rendering fragments
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        IWebExchange exchange = application.buildExchange(request, response);
        WebContext context = new WebContext(exchange, locale);
        context.setVariable("user", user);

        // Update Boss HP Bar
        context.setVariable("bossName", boss.getName());
        context.setVariable("bossHp", result.bossHp);
        context.setVariable("bossMaxHp", boss.getMaxHp());
        context.setVariable("bossAtk", boss.getAttack());
        context.setVariable("bossImage", boss.getImagePath());
        String bossHpBarHtml = templateEngine.process("fragments/boss_hp_bar", Set.of("bossHpBar"), context);
        jsonResponse.put("bossHpBarHtml", bossHpBarHtml);

        // Update User HP Bar
        user.setAttack(combatService.calculateUserBaseAttack(boss));
        context.setVariable("user", user);
        context.setVariable("compact", false);
        String userHpBarHtml = templateEngine.process("fragments/user_hp_bar", Set.of("userHpBar"), context);
        jsonResponse.put("userHpBarHtml", userHpBarHtml);

        // Render Result Feedback
        if (answer != null && !answer.trim().isEmpty() && currentQuestion != null) {
            context.setVariable("userAnswer", answer);
            context.setVariable("correctAnswer", currentQuestion.getCorrectAnswer());
            context.setVariable("correct", isCorrect);
            String resultHtml = templateEngine.process("fragments/quiz-text-result", Set.of("result"),
                    context);
            jsonResponse.put("resultHtml", resultHtml);
        } else {
            context.setVariable("choices", choices);
            context.setVariable("selectedChoiceId", null);
            context.setVariable("submittedChoiceId", submittedId != null ? submittedId : -1L);
            context.setVariable("correctChoiceId", 1L);
            context.setVariable("correct", isCorrect);
            String resultHtml = templateEngine.process("fragments/quiz-choices-list", Set.of("list"),
                    context);
            jsonResponse.put("resultHtml", resultHtml);
        }

        switch (result.status) {
            case VICTOIRE -> {
                session.setAttribute("combat_status", "VICTORY");
                combatService.handleVictory(user, boss);
                jsonResponse.put("status", "VICTORY");
                jsonResponse.put("redirectUrl", "/combat/victory");
            }
            case DEFAITE -> {
                session.setAttribute("combat_status", "DEFEAT");
                combatService.handleDefeat(user, boss);
                jsonResponse.put("status", "DEFEAT");
                jsonResponse.put("redirectUrl", "/combat/defeat");
            }
            default -> {
                jsonResponse.put("status", "ONGOING");
                // Render Next Question
                Question nextQuestion = combatService.getCurrentQuestion(session);
                if (nextQuestion != null) {

                    // Prepare choices for the next question
                    List<Choice> nextChoices = new ArrayList<>();
                    nextChoices.add(new Choice(1L, nextQuestion.getCorrectAnswer()));
                    nextChoices.add(new Choice(2L, nextQuestion.getBadAnswer1()));
                    nextChoices.add(new Choice(3L, nextQuestion.getBadAnswer2()));
                    nextChoices.add(new Choice(4L, nextQuestion.getBadAnswer3()));
                    Collections.shuffle(nextChoices);
                    Collections.shuffle(nextChoices);
                    session.setAttribute("combat_current_choices", nextChoices);
                    session.removeAttribute("combat_help_level"); // Reset help level for new question

                    context.setVariable("question", nextQuestion.getQuestionText());
                    context.setVariable("choices", nextChoices);
                    context.setVariable("selectedChoiceId", null);

                    String nextQuestionHtml = templateEngine.process("fragments/quiz-interface-options",
                            Set.of("choices"), context);
                    jsonResponse.put("nextQuestionHtml", nextQuestionHtml);
                }
            }
        }

        return ResponseEntity.ok(jsonResponse);
    }

    @GetMapping("/choices")
    public String getChoices(@RequestParam(defaultValue = "FOUR_CHOICES") HelpLevel helpLevel, HttpSession session,
            Model model) {
        Question question = combatService.getCurrentQuestion(session);
        if (question == null) {
            return ""; // Or handle error
        }

        List<Choice> choices = new ArrayList<>();
        choices.add(new Choice(1L, question.getCorrectAnswer()));
        choices.add(new Choice(2L, question.getBadAnswer1()));
        if (helpLevel == HelpLevel.FOUR_CHOICES) {
            choices.add(new Choice(3L, question.getBadAnswer2()));
            choices.add(new Choice(4L, question.getBadAnswer3()));
        }
        Collections.shuffle(choices);

        // Store choices in session for result display
        session.setAttribute("combat_current_choices", choices);

        // Track help level usage to prevent abuse
        session.setAttribute("combat_help_level", helpLevel);

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
            model.addAttribute("xpReward", boss.getPosition() * 50);
        } else {
            model.addAttribute("goldReward", 0);
            model.addAttribute("xpReward", 0);
        }
        return "boss-victory";
    }

    @GetMapping("/defeat")
    public String defeat() {
        return "boss-fail";
    }

}
