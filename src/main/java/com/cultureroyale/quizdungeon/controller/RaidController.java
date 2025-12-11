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

import com.cultureroyale.quizdungeon.model.Dungeon;
import com.cultureroyale.quizdungeon.model.DungeonQuestion;
import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.model.dto.Choice;
import com.cultureroyale.quizdungeon.model.dto.TurnResult;
import com.cultureroyale.quizdungeon.model.enums.HelpLevel;
import com.cultureroyale.quizdungeon.repository.DungeonRepository;
import com.cultureroyale.quizdungeon.repository.UserRepository;
import com.cultureroyale.quizdungeon.service.QuestionService;
import com.cultureroyale.quizdungeon.service.RaidService;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/raid")
public class RaidController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RaidService raidService;

    @Autowired
    private DungeonRepository dungeonRepository;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private QuestionService questionService;

    @GetMapping("/start/{dungeonId}")
    public String startRaid(@PathVariable Long dungeonId, Authentication authentication, HttpSession session) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        Dungeon dungeon = dungeonRepository.findById(dungeonId).orElseThrow();

        // Initialize raid session
        raidService.startRaid(user, dungeon, session);

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

        // Calculate Dungeon attack for display
        int dungeonAtk = (int) (25 * (1 + dungeon.getDamageBoost() / 100.0));
        model.addAttribute("dungeonAtk", dungeonAtk);

        // Get current question
        DungeonQuestion currentDQ = raidService.getCurrentRaidQuestion(session);
        if (currentDQ == null) {
            // Should not happen if logic is correct, but handle as draw/end
            return "redirect:/raid/draw";
        }

        model.addAttribute("question", currentDQ.getQuestion().getQuestionText());

        // Question count
        @SuppressWarnings("unchecked")
        List<DungeonQuestion> questions = (List<DungeonQuestion>) session.getAttribute("raid_questions");
        int currentIndex = (int) session.getAttribute("raid_current_question_index");
        model.addAttribute("currentQuestionNumber", currentIndex + 1);
        model.addAttribute("totalQuestions", questions.size());

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
        user.setAttack(10); // Base attack to 10

        boolean isCorrect = false;
        Long submittedId = choiceId;

        // Retrieve choices for feedback
        @SuppressWarnings("unchecked")
        List<Choice> choices = (List<Choice>) session.getAttribute("raid_current_choices");

        DungeonQuestion currentDQ = raidService.getCurrentRaidQuestion(session);

        // Validate Answer
        if (currentDQ != null) {
            isCorrect = questionService.verifyAnswer(answer, choiceId, currentDQ.getQuestion().getCorrectAnswer());
        } else {
            isCorrect = false;
        }

        // Check for backend abuse: if help was requested, enforce it
        HelpLevel helpLevel = (HelpLevel) session.getAttribute("raid_help_level");
        if (helpLevel == null) {
            helpLevel = HelpLevel.NO_HELP;
        }

        TurnResult result = raidService.processRaidTurn(user, isCorrect, helpLevel, session);

        // Prepare Context
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        IWebExchange exchange = application.buildExchange(request, response);
        WebContext context = new WebContext(exchange, locale);

        // Update User Object for View (HP)
        context.setVariable("user", user);

        // Update Dungeon HP Bar (reusing boss_hp_bar but with dungeon stats)
        context.setVariable("bossName", dungeon.getName());
        context.setVariable("bossHp", result.bossHp); // bossHp in result holds dungeonHp
        int dungeonMaxHp = (int) session.getAttribute("raid_dungeon_max_hp");
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

            // Pass question counts
            @SuppressWarnings("unchecked")
            List<DungeonQuestion> qList = (List<DungeonQuestion>) session.getAttribute("raid_questions");
            int cIndex = (int) session.getAttribute("raid_current_question_index");
            context.setVariable("currentQuestionNumber", cIndex + 1);
            context.setVariable("totalQuestions", qList.size());

            String resultHtml = templateEngine.process("fragments/quiz-choices-list", Set.of("list"), context);
            jsonResponse.put("resultHtml", resultHtml);
        }

        // Check end conditions
        switch (result.status) {
            case VICTOIRE:
                raidService.handleRaidVictory(user, dungeon, session);
                jsonResponse.put("status", "VICTORY");
                jsonResponse.put("redirectUrl", "/raid/victory");
                break;
            case DEFAITE:
                raidService.handleRaidDefeat(user, dungeon, session);
                jsonResponse.put("status", "DEFEAT");
                jsonResponse.put("redirectUrl", "/raid/defeat");
                break;
            default:
                // Next Question
                int nextIndex = (int) session.getAttribute("raid_current_question_index") + 1;
                @SuppressWarnings("unchecked")
                List<DungeonQuestion> questions = (List<DungeonQuestion>) session.getAttribute("raid_questions");

                if (nextIndex >= questions.size()) {
                    // Out of questions, but Dungeon still alive -> DRAW
                    raidService.handleRaidDraw(user, dungeon, session);
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
                    context.setVariable("currentQuestionNumber", nextIndex + 1);
                    context.setVariable("totalQuestions", questions.size());

                    String nextQuestionHtml = templateEngine.process("fragments/quiz-interface-options",
                            Set.of("choices"), context);
                    jsonResponse.put("nextQuestionHtml", nextQuestionHtml);
                    jsonResponse.put("status", "ONGOING");
                }
                break;
        }

        return ResponseEntity.ok(jsonResponse);
    }

    @GetMapping("/choices")
    public String getChoices(@RequestParam(defaultValue = "FOUR_CHOICES") HelpLevel helpLevel, HttpSession session,
            Model model) {
        DungeonQuestion dq = raidService.getCurrentRaidQuestion(session);
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
        model.addAttribute("submittedChoiceId", null);
        model.addAttribute("correctChoiceId", null);

        @SuppressWarnings("unchecked")
        List<DungeonQuestion> questions = (List<DungeonQuestion>) session.getAttribute("raid_questions");
        int currentIndex = (int) session.getAttribute("raid_current_question_index");
        model.addAttribute("currentQuestionNumber", currentIndex + 1);
        model.addAttribute("totalQuestions", questions.size());

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

}
