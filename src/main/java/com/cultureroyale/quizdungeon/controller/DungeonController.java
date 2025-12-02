package com.cultureroyale.quizdungeon.controller;

import java.security.Principal;

import com.cultureroyale.quizdungeon.model.Dungeon;
import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.service.DungeonService;
import com.cultureroyale.quizdungeon.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import com.cultureroyale.quizdungeon.model.UserQuestion;
import com.cultureroyale.quizdungeon.repository.UserRepository;
import com.cultureroyale.quizdungeon.repository.UserQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DungeonController {

    @Autowired
    private DungeonService dungeonService;

    private final UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserQuestionRepository userQuestionRepository;

    @GetMapping("/dungeon/attack")
    public String attackDungeon(Model model, Principal principal) {
        User currentUser = userService.findByUsername(principal.getName());

        boolean isExpired = false;
        if (currentUser.getCurrentOpponentDungeonAssignedAt() != null) {
            if (currentUser.getCurrentOpponentDungeonAssignedAt().plusMinutes(10)
                    .isBefore(java.time.LocalDateTime.now())) {
                isExpired = true;
            }
        }

        if (currentUser.getCurrentOpponentDungeon() == null || isExpired) {
            Dungeon randomDungeon = dungeonService.getRandomDungeon(currentUser);
            if (randomDungeon != null) {
                System.out.println("ASSIGNMENT: Assigning new dungeon " + randomDungeon.getName() + " to user "
                        + currentUser.getUsername());
                currentUser.setCurrentOpponentDungeon(randomDungeon);
                currentUser.setCurrentOpponentDungeonAssignedAt(java.time.LocalDateTime.now());
                userService.save(currentUser);
            } else {
                System.out.println("ASSIGNMENT: No dungeon found for user " + currentUser.getUsername());
            }
        }

        model.addAttribute("dungeon", currentUser.getCurrentOpponentDungeon());
        return "dungeon-attack";
    }

    @PostMapping("/dungeon/attack/buy")
    public String buyDungeon(Principal principal) {
        User currentUser = userService.findByUsername(principal.getName());
        if (currentUser.getGold() >= 80) {
            currentUser.setGold(currentUser.getGold() - 80);

            Dungeon newDungeon = dungeonService.getRandomDungeon(currentUser);
            currentUser.setCurrentOpponentDungeon(newDungeon);
            currentUser.setCurrentOpponentDungeonAssignedAt(java.time.LocalDateTime.now());
            userService.save(currentUser);

            return "redirect:/dungeon/attack";
        } else {
            return "redirect:/dungeon/attack?error=not_enough_gold";
        }
    }

    @GetMapping("/dungeon/edit")
    public String editDungeon(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        Dungeon dungeon = dungeonService.getDungeonByUser(user);

        List<UserQuestion> unlockedQuestions = userQuestionRepository.findByUserId(user.getId());

        model.addAttribute("dungeon", dungeon);
        model.addAttribute("unlockedQuestions", unlockedQuestions);
        model.addAttribute("dungeonQuestions", dungeon.getDungeonQuestions());

        return "dungeon-edit";
    }

    @PostMapping("/dungeon/save")
    @ResponseBody
    public String saveDungeon(@RequestBody List<Long> questionIds, Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        Dungeon dungeon = dungeonService.getDungeonByUser(user);

        dungeonService.updateDungeonQuestions(dungeon, questionIds);

        return "OK";
    }

}
