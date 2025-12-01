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
import java.text.Normalizer;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import org.springframework.http.ResponseEntity;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.context.WebContext;

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

    @GetMapping("/start/{bossId}")
    public String startCombat(@PathVariable Long bossId, Authentication authentication, HttpSession session) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        Boss boss = bossRepository.findById(bossId).orElseThrow();

        // Clear any previous combat status
        session.removeAttribute("combat_status");
        session.removeAttribute("combat_current_choices");

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
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitAnswer(@RequestParam(required = false) String answer,
            @RequestParam(required = false) Long choiceId,
            @RequestParam(defaultValue = "FOUR_CHOICES") HelpLevel helpLevel,
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
                jsonResponse.put("redirectUrl", "/combat/battle");
                return ResponseEntity.ok(jsonResponse);
            }
        }

        com.cultureroyale.quizdungeon.model.Question currentQuestion = combatService.getCurrentQuestion(session);

        if (answer != null && !answer.trim().isEmpty()) {
            // Text submission
            if (currentQuestion != null) {
                // Compare answer (case insensitive and accent insensitive)
                String correctAnswer = normalize(currentQuestion.getCorrectAnswer().trim());
                String userAnswer = normalize(answer.trim());

                // direct comparison (case insensitive and accent insensitive)
                if (correctAnswer.equalsIgnoreCase(userAnswer)) {
                    isCorrect = true;
                } else {
                    // forgiveness: Levenshtein distance check if length > 3
                    if (userAnswer.length() > 3) {
                        int distance = calculateLevenshteinDistance(userAnswer, correctAnswer);
                        if (distance <= 3) {
                            isCorrect = true;
                        }
                    }

                    // VERY VERY forgiving
                    if (!isCorrect) {
                        String[] userWords = userAnswer.split("\\s+");
                        String[] correctWords = correctAnswer.split("\\s+");

                        for (String uWord : userWords) {
                            for (String cWord : correctWords) {
                                if (uWord.equalsIgnoreCase(cWord)) {
                                    isCorrect = true;
                                    break;
                                }
                                // SUPER forgiving
                                if (uWord.length() > 3) {
                                    int dist = calculateLevenshteinDistance(uWord, cWord);
                                    if (dist <= 3) {
                                        isCorrect = true;
                                        break;
                                    }
                                }
                            }
                            if (isCorrect)
                                break;
                        }
                    }
                }
            }
        } else {
            // Choice submission
            isCorrect = (choiceId != null && choiceId == 1L);
        }

        CombatService.CombatResult result = combatService.processTurn(user, boss, isCorrect, helpLevel, session);

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
        String bossHpBarHtml = templateEngine.process("fragments/boss_hp_bar", java.util.Set.of("bossHpBar"), context);
        jsonResponse.put("bossHpBarHtml", bossHpBarHtml);

        // Update User HP Bar
        // Re-fetch user to ensure latest state if needed, but 'user' object should be
        // updated by processTurn
        // Ensure user attack is set for display
        user.setAttack(combatService.calculateUserBaseAttack(boss));
        context.setVariable("user", user);
        context.setVariable("compact", false);
        String userHpBarHtml = templateEngine.process("fragments/user_hp_bar", java.util.Set.of("userHpBar"), context);
        jsonResponse.put("userHpBarHtml", userHpBarHtml);

        // Render Result Feedback
        if (answer != null && !answer.trim().isEmpty()) {
            context.setVariable("userAnswer", answer);
            context.setVariable("correctAnswer", currentQuestion.getCorrectAnswer());
            context.setVariable("correct", isCorrect);
            String resultHtml = templateEngine.process("fragments/quiz-text-result", java.util.Set.of("result"),
                    context);
            jsonResponse.put("resultHtml", resultHtml);
        } else {
            context.setVariable("choices", choices);
            context.setVariable("selectedChoiceId", null);
            context.setVariable("submittedChoiceId", submittedId != null ? submittedId : -1L);
            context.setVariable("correctChoiceId", 1L);
            context.setVariable("correct", isCorrect);
            String resultHtml = templateEngine.process("fragments/quiz-choices-list", java.util.Set.of("list"),
                    context);
            jsonResponse.put("resultHtml", resultHtml);
        }

        if (result.status == CombatService.CombatStatus.VICTORY) {
            session.setAttribute("combat_status", "VICTORY");
            combatService.handleVictory(user, boss);
            jsonResponse.put("status", "VICTORY");
            jsonResponse.put("redirectUrl", "/combat/victory");
        } else if (result.status == CombatService.CombatStatus.DEFEAT) {
            session.setAttribute("combat_status", "DEFEAT");
            combatService.handleDefeat(user, boss);
            jsonResponse.put("status", "DEFEAT");
            jsonResponse.put("redirectUrl", "/combat/defeat");
        } else {
            jsonResponse.put("status", "ONGOING");
            // Render Next Question
            com.cultureroyale.quizdungeon.model.Question nextQuestion = combatService.getCurrentQuestion(session);
            if (nextQuestion != null) {

                // Prepare choices for the next question
                java.util.List<Choice> nextChoices = new java.util.ArrayList<>();
                nextChoices.add(new Choice(1L, nextQuestion.getCorrectAnswer()));
                nextChoices.add(new Choice(2L, nextQuestion.getBadAnswer1()));
                nextChoices.add(new Choice(3L, nextQuestion.getBadAnswer2()));
                nextChoices.add(new Choice(4L, nextQuestion.getBadAnswer3()));
                java.util.Collections.shuffle(nextChoices);
                session.setAttribute("combat_current_choices", nextChoices);

                context.setVariable("question", nextQuestion.getQuestionText());
                context.setVariable("choices", nextChoices);
                context.setVariable("selectedChoiceId", null);

                String nextQuestionHtml = templateEngine.process("fragments/quiz-interface-options",
                        java.util.Set.of("choices"), context);
                jsonResponse.put("nextQuestionHtml", nextQuestionHtml);
            }
        }

        return ResponseEntity.ok(jsonResponse);
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

    // Levenshtein distance
    // https://fr.wikipedia.org/wiki/Distance_de_Levenshtein
    private int calculateLevenshteinDistance(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = min(dp[i - 1][j - 1] + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)),
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1);
                }
            }
        }

        return dp[x.length()][y.length()];
    }

    private int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 1;
    }

    private int min(int... numbers) {
        return java.util.Arrays.stream(numbers).min().orElse(Integer.MAX_VALUE);
    }

    private String normalize(String input) {
        if (input == null)
            return null;
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("").toLowerCase();
    }

}
