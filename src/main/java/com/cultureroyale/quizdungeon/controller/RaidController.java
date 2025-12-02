package com.cultureroyale.quizdungeon.controller;

import com.cultureroyale.quizdungeon.model.Dungeon;
import com.cultureroyale.quizdungeon.model.DungeonQuestion;
import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.model.dto.Choice;
import com.cultureroyale.quizdungeon.model.enums.HelpLevel;
import com.cultureroyale.quizdungeon.repository.DungeonRepository;
import com.cultureroyale.quizdungeon.repository.UserRepository;
import com.cultureroyale.quizdungeon.service.UserService;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.*;

@Controller
@RequestMapping("/raid")
public class RaidController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private DungeonRepository dungeonRepository;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private QuestionController questionController;

    @GetMapping("/start/{dungeonId}")
    public String startRaid(@PathVariable Long dungeonId, Authentication authentication, HttpSession session) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        Dungeon dungeon = dungeonRepository.findById(dungeonId).orElseThrow();

        // Initialize Raid Session
        session.setAttribute("raid_dungeon", dungeon);
        session.setAttribute("raid_status", "ONGOING");

        // Dungeon HP = Number of Questions * 10
        int dungeonMaxHp = dungeon.getDungeonQuestions().size() * 10;
        session.setAttribute("raid_dungeon_max_hp", dungeonMaxHp);
        session.setAttribute("raid_dungeon_current_hp", dungeonMaxHp);

        // Player HP (use user's max HP)
        session.setAttribute("raid_user_current_hp", user.getMaxHp());

        // Load Questions (ordered by position)
        List<DungeonQuestion> questions = new ArrayList<>(dungeon.getDungeonQuestions());
        questions.sort(Comparator.comparingInt(DungeonQuestion::getPosition));
        session.setAttribute("raid_questions", questions);
        session.setAttribute("raid_questions", questions);
        session.setAttribute("raid_current_question_index", 0);
        session.removeAttribute("raid_help_level");

        return "redirect:/raid/battle";
    }

    @GetMapping("/battle")
    public String showBattle(Authentication authentication, HttpSession session, Model model) {
        Dungeon dungeon = (Dungeon) session.getAttribute("raid_dungeon");
        if (dungeon == null)
            return "redirect:/roadmap";

        String status = (String) session.getAttribute("raid_status");
        if ("VICTORY".equals(status))
            return "redirect:/raid/victory";
        if ("DEFEAT".equals(status))
            return "redirect:/raid/defeat";
        if ("DRAW".equals(status))
            return "redirect:/raid/draw";

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        // Set fixed player attack for display
        user.setAttack(10);

        model.addAttribute("user", user);
        model.addAttribute("dungeon", dungeon);
        model.addAttribute("dungeonHp", session.getAttribute("raid_dungeon_current_hp"));
        model.addAttribute("dungeonMaxHp", session.getAttribute("raid_dungeon_max_hp"));

        // Calculate Dungeon Attack for display
        int dungeonAtk = (int) (25 * (1 + dungeon.getDamageBoost() / 100.0));
        model.addAttribute("dungeonAtk", dungeonAtk);

        // Get current question
        DungeonQuestion currentDQ = getCurrentQuestion(session);
        if (currentDQ == null) {
            // Should not happen if logic is correct, but handle as draw/end
            return "redirect:/raid/draw";
        }

        model.addAttribute("question", currentDQ.getQuestion().getQuestionText());
        model.addAttribute("choices", new ArrayList<>()); // Loaded via AJAX
        model.addAttribute("selectedChoiceId", null);

        return "raid";
    }

    @PostMapping("/submit")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitAnswer(@RequestParam(required = false) String answer,
            @RequestParam(required = false) Long choiceId,
            Authentication authentication, HttpSession session, Locale locale,
            HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> jsonResponse = new HashMap<>();
        Dungeon dungeon = (Dungeon) session.getAttribute("raid_dungeon");
        if (dungeon == null) {
            jsonResponse.put("redirectUrl", "/roadmap");
            return ResponseEntity.ok(jsonResponse);
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        user.setAttack(5); // Ensure attack is set

        boolean isCorrect = false;
        Long submittedId = choiceId;

        // Retrieve choices for feedback
        @SuppressWarnings("unchecked")
        List<Choice> choices = (List<Choice>) session.getAttribute("raid_current_choices");

        DungeonQuestion currentDQ = getCurrentQuestion(session);

        // Validate Answer
        if (currentDQ != null) {
            isCorrect = questionController.verifyAnswer(answer, choiceId, currentDQ.getQuestion().getCorrectAnswer());
        } else {
            isCorrect = false;
        }

        // Combat Logic
        int dungeonHp = (int) session.getAttribute("raid_dungeon_current_hp");
        int userHp = (int) session.getAttribute("raid_user_current_hp");
        int dungeonMaxHp = (int) session.getAttribute("raid_dungeon_max_hp");

        // Check for backend abuse: if help was requested, enforce it
        HelpLevel helpLevel = (HelpLevel) session.getAttribute("raid_help_level");
        if (helpLevel == null) {
            helpLevel = HelpLevel.NO_HELP;
        }

        if (isCorrect) {
            // Damage to Dungeon
            double multiplier = 1.0;
            if (helpLevel == HelpLevel.NO_HELP)
                multiplier = 3.0; // Text input
            else if (helpLevel == HelpLevel.TWO_CHOICES)
                multiplier = 0.5;

            int damage = (int) (user.getAttack() * multiplier);
            dungeonHp = Math.max(0, dungeonHp - damage);
        } else {
            // Damage to Player
            int dungeonAtk = (int) (25 * (1 + dungeon.getDamageBoost() / 100.0));
            userHp = Math.max(0, userHp - dungeonAtk);
        }

        session.setAttribute("raid_dungeon_current_hp", dungeonHp);
        session.setAttribute("raid_user_current_hp", userHp);

        // Prepare Context
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        IWebExchange exchange = application.buildExchange(request, response);
        WebContext context = new WebContext(exchange, locale);

        // Update User Object for View (HP)
        user.setCurrentHp(userHp);
        context.setVariable("user", user);

        // Update Dungeon HP Bar (reusing boss_hp_bar but with dungeon stats)
        context.setVariable("bossName", dungeon.getName());
        context.setVariable("bossHp", dungeonHp);
        context.setVariable("bossMaxHp", dungeonMaxHp);
        context.setVariable("bossAtk", (int) (25 * (1 + dungeon.getDamageBoost() / 100.0)));
        context.setVariable("bossImage", "/images/shop/theme_" + dungeon.getCosmeticTheme() + ".png"); // Construct path

        String bossHpBarHtml = templateEngine.process("fragments/boss_hp_bar", Set.of("bossHpBar"), context);
        jsonResponse.put("bossHpBarHtml", bossHpBarHtml);

        // Update User HP Bar
        context.setVariable("compact", false);
        String userHpBarHtml = templateEngine.process("fragments/user_hp_bar", Set.of("userHpBar"), context);
        jsonResponse.put("userHpBarHtml", userHpBarHtml);

        // Result Feedback
        if (answer != null && !answer.trim().isEmpty()) {
            context.setVariable("userAnswer", answer);
            context.setVariable("correctAnswer", currentDQ.getQuestion().getCorrectAnswer());
            context.setVariable("correct", isCorrect);
            String resultHtml = templateEngine.process("fragments/quiz-text-result", Set.of("result"), context);
            jsonResponse.put("resultHtml", resultHtml);
        } else {
            context.setVariable("choices", choices);
            context.setVariable("selectedChoiceId", null);
            context.setVariable("submittedChoiceId", submittedId != null ? submittedId : -1L);
            context.setVariable("correctChoiceId", 1L);
            context.setVariable("correct", isCorrect);
            String resultHtml = templateEngine.process("fragments/quiz-choices-list", Set.of("list"), context);
            jsonResponse.put("resultHtml", resultHtml);
        }

        // Check End Conditions
        if (dungeonHp <= 0) {
            handleVictory(user, dungeon, session);
            jsonResponse.put("status", "VICTORY");
            jsonResponse.put("redirectUrl", "/raid/victory");
        } else if (userHp <= 0) {
            handleDefeat(user, dungeon, session);
            jsonResponse.put("status", "DEFEAT");
            jsonResponse.put("redirectUrl", "/raid/defeat");
        } else {
            // Next Question
            int nextIndex = (int) session.getAttribute("raid_current_question_index") + 1;
            @SuppressWarnings("unchecked")
            List<DungeonQuestion> questions = (List<DungeonQuestion>) session.getAttribute("raid_questions");

            if (nextIndex >= questions.size()) {
                // Out of questions, but Dungeon still alive -> DRAW
                handleDraw(user, dungeon, session);
                jsonResponse.put("status", "DRAW");
                jsonResponse.put("redirectUrl", "/raid/draw");
            } else {
                session.setAttribute("raid_current_question_index", nextIndex);
                DungeonQuestion nextDQ = questions.get(nextIndex);

                // Prepare choices
                List<Choice> nextChoices = new ArrayList<>();
                nextChoices.add(new Choice(1L, nextDQ.getQuestion().getCorrectAnswer()));
                nextChoices.add(new Choice(2L, nextDQ.getQuestion().getBadAnswer1()));
                nextChoices.add(new Choice(3L, nextDQ.getQuestion().getBadAnswer2()));
                nextChoices.add(new Choice(4L, nextDQ.getQuestion().getBadAnswer3()));
                Collections.shuffle(nextChoices);
                Collections.shuffle(nextChoices);
                session.setAttribute("raid_current_choices", nextChoices);
                session.removeAttribute("raid_help_level"); // Reset help level

                context.setVariable("question", nextDQ.getQuestion().getQuestionText());
                context.setVariable("choices", nextChoices);
                context.setVariable("selectedChoiceId", null);

                String nextQuestionHtml = templateEngine.process("fragments/quiz-interface-options", Set.of("choices"),
                        context);
                jsonResponse.put("nextQuestionHtml", nextQuestionHtml);
                jsonResponse.put("status", "ONGOING");
            }
        }

        return ResponseEntity.ok(jsonResponse);
    }

    @GetMapping("/choices")
    public String getChoices(@RequestParam(defaultValue = "FOUR_CHOICES") HelpLevel helpLevel, HttpSession session,
            Model model) {
        DungeonQuestion dq = getCurrentQuestion(session);
        if (dq == null)
            return "";

        List<Choice> choices = new ArrayList<>();
        choices.add(new Choice(1L, dq.getQuestion().getCorrectAnswer()));
        choices.add(new Choice(2L, dq.getQuestion().getBadAnswer1()));
        if (helpLevel == HelpLevel.FOUR_CHOICES) {
            choices.add(new Choice(3L, dq.getQuestion().getBadAnswer2()));
            choices.add(new Choice(4L, dq.getQuestion().getBadAnswer3()));
        }
        Collections.shuffle(choices);
        Collections.shuffle(choices);
        session.setAttribute("raid_current_choices", choices);

        // Track help level usage to prevent abuse
        session.setAttribute("raid_help_level", helpLevel);

        model.addAttribute("choices", choices);
        model.addAttribute("selectedChoiceId", null);
        model.addAttribute("submittedChoiceId", null);
        model.addAttribute("correctChoiceId", null);

        return "fragments/quiz-choices-list :: list";
    }

    @GetMapping("/victory")
    public String victory(HttpSession session, Model model) {
        model.addAttribute("status", "VICTORY");
        model.addAttribute("goldChange", session.getAttribute("raid_gold_change"));
        model.addAttribute("xpReward", session.getAttribute("raid_xp_reward"));
        return "raid-result";
    }

    @GetMapping("/defeat")
    public String defeat(HttpSession session, Model model) {
        model.addAttribute("status", "DEFEAT");
        model.addAttribute("goldChange", session.getAttribute("raid_gold_change"));
        model.addAttribute("xpReward", 0);
        return "raid-result";
    }

    @GetMapping("/draw")
    public String draw(HttpSession session, Model model) {
        model.addAttribute("status", "DRAW");
        model.addAttribute("goldChange", 0);
        model.addAttribute("xpReward", session.getAttribute("raid_xp_reward"));
        return "raid-result";
    }

    private DungeonQuestion getCurrentQuestion(HttpSession session) {
        @SuppressWarnings("unchecked")
        List<DungeonQuestion> questions = (List<DungeonQuestion>) session.getAttribute("raid_questions");
        int index = (int) session.getAttribute("raid_current_question_index");
        if (questions != null && index < questions.size()) {
            return questions.get(index);
        }
        return null;
    }

    private void handleVictory(User attacker, Dungeon dungeon, HttpSession session) {
        session.setAttribute("raid_status", "VICTORY");
        User owner = dungeon.getUser();

        int ownerGold = owner.getGold();
        int transferAmount = (int) (ownerGold * 0.20);

        owner.setGold(ownerGold - transferAmount);
        attacker.setGold(attacker.getGold() + transferAmount);

        userRepository.save(owner);

        session.setAttribute("raid_gold_change", transferAmount);

        // Clear opponent
        attacker.setCurrentOpponentDungeon(null);
        attacker.setCurrentOpponentDungeonAssignedAt(null);

        // XP Reward (25 XP per question)
        int xpReward = dungeon.getDungeonQuestions().size() * 25;
        session.setAttribute("raid_xp_reward", xpReward);
        userService.addXp(attacker, xpReward);
    }

    private void handleDefeat(User attacker, Dungeon dungeon, HttpSession session) {
        session.setAttribute("raid_status", "DEFEAT");
        User owner = dungeon.getUser();

        int attackerGold = attacker.getGold();
        int lostAmount = (int) (attackerGold * 0.50);
        int gainAmount = (int) (attackerGold * 0.10); // Owner gets 10% of original, not 10% of lost

        attacker.setGold(attackerGold - lostAmount);
        owner.setGold(owner.getGold() + gainAmount);

        userRepository.save(attacker);
        userRepository.save(owner);

        session.setAttribute("raid_gold_change", -lostAmount);

        // Restore HP to max
        attacker.setCurrentHp(attacker.getMaxHp());

        // Clear opponent
        attacker.setCurrentOpponentDungeon(null);
        attacker.setCurrentOpponentDungeonAssignedAt(null);

        userRepository.save(attacker);
        userRepository.save(owner);
    }

    private void handleDraw(User attacker, Dungeon dungeon, HttpSession session) {
        session.setAttribute("raid_status", "DRAW");

        // Clear opponent
        attacker.setCurrentOpponentDungeon(null);
        attacker.setCurrentOpponentDungeonAssignedAt(null);

        // XP Reward (10 XP per question)
        int xpReward = dungeon.getDungeonQuestions().size() * 10;
        session.setAttribute("raid_xp_reward", xpReward);
        userService.addXp(attacker, xpReward);
    }

}
