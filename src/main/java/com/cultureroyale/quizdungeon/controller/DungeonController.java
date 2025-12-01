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

// Gestion donjon : GET /dungeon/edit (édition), POST /dungeon/add-question, DELETE /dungeon/remove-question
// PvP : GET /dungeon/attack (sélection cible), GET /dungeon/attack/refresh (nouvelle cible)

@Controller
@RequiredArgsConstructor
public class DungeonController {

    private final DungeonService dungeonService;
    private final UserService userService;

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
                currentUser.setCurrentOpponentDungeon(randomDungeon);
                currentUser.setCurrentOpponentDungeonAssignedAt(java.time.LocalDateTime.now());
                userService.save(currentUser);
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

}