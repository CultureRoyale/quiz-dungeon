package com.cultureroyale.quizdungeon.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.cultureroyale.quizdungeon.model.User;

import java.util.ArrayList;
import java.util.List;

@Controller
public class TestController {

    @GetMapping("/test-quiz/choices")
    public String getChoices(@RequestParam int count, Model model) {
        List<Choice> allChoices = new ArrayList<>();
        allChoices.add(new Choice(1L, "Nenäveren vuotokuume"));
        allChoices.add(new Choice(2L, "Hippopotomonstrosesquipedaliophobie"));
        allChoices.add(new Choice(3L, "Prijestolonasljednikovičičinima"));
        allChoices.add(new Choice(4L, "Esternocleidomastoideo"));

        List<Choice> filteredChoices;
        if (count == 2) {
            // Mock logic: keep correct answer (id 2) and one wrong answer
            filteredChoices = new ArrayList<>();
            filteredChoices.add(allChoices.get(1)); // Correct
            filteredChoices.add(allChoices.get(0)); // Wrong
        } else {
            filteredChoices = allChoices;
        }

        model.addAttribute("choices", filteredChoices);
        model.addAttribute("selectedChoiceId", null); // Reset selection

        return "fragments/quiz-choices-list :: list";
    }

    @GetMapping("/test-battle")
    public String testBattle(Model model) {
        // Mock User
        User user = new User();
        user.setUsername("User12583");
        user.setLevel(5);
        user.setGold(523854);
        user.setCurrentHp(50);
        user.setMaxHp(80);
        user.setXp(100);

        model.addAttribute("user", user);

        // Boss Data
        model.addAttribute("bossName", "Bebe dragon");
        model.addAttribute("bossHp", 50);
        model.addAttribute("bossMaxHp", 80);
        model.addAttribute("bossAtk", 2.5);
        model.addAttribute("bossImage", "/images/boss/bebe_dragon.png");

        // Quiz Data
        model.addAttribute("question",
                "Quel est le mot le plus long du dictionnaire à part anticonstitutionnellement ?");
        model.addAttribute("choices", null); // Not needed for initial load
        model.addAttribute("selectedChoiceId", null);

        return "quiz-battle";
    }

    @PostMapping("/test-quiz/submit")
    public String submitQuiz(@RequestParam(required = false) String answer,
            @RequestParam(required = false) Long choiceId,
            Model model) {

        boolean correct = false;
        Long correctChoiceId = 2L; // Mock correct ID

        if (choiceId != null) {
            correct = choiceId.equals(correctChoiceId);
        } else if (answer != null) {
            correct = answer.equalsIgnoreCase("Hippopotomonstrosesquipedaliophobie");
        }

        // Re-create choices for the view
        List<Choice> allChoices = new ArrayList<>();
        allChoices.add(new Choice(1L, "Nenäveren vuotokuume"));
        allChoices.add(new Choice(2L, "Hippopotomonstrosesquipedaliophobie"));
        allChoices.add(new Choice(3L, "Prijestolonasljednikovičičinima"));
        allChoices.add(new Choice(4L, "Esternocleidomastoideo"));

        model.addAttribute("choices", allChoices);

        model.addAttribute("correct", correct);
        model.addAttribute("submittedChoiceId", choiceId);
        model.addAttribute("correctChoiceId", correctChoiceId);

        return "fragments/quiz-choices-list :: list";
    }

    // Simple inner class for choices
    public static class Choice {
        private Long id;
        private String text;

        public Choice(Long id, String text) {
            this.id = id;
            this.text = text;
        }

        public Long getId() {
            return id;
        }

        public String getText() {
            return text;
        }
    }
}
