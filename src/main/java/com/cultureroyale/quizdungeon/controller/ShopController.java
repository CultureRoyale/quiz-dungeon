package com.cultureroyale.quizdungeon.controller;

import com.cultureroyale.quizdungeon.model.Dungeon;
import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.repository.DungeonRepository;
import com.cultureroyale.quizdungeon.repository.UserRepository;
import com.cultureroyale.quizdungeon.service.DungeonService;
import com.cultureroyale.quizdungeon.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ShopController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final DungeonRepository dungeonRepository;
    private final DungeonService dungeonService;

    @GetMapping("/shop")
    public String shop(Model model, Principal principal) {
        String username = principal.getName();
        User user = userService.findByUsername(username);
        Dungeon dungeon = dungeonRepository.findByUserId(user.getId()).orElse(null);

        if (dungeon == null) {
            dungeon = dungeonService.createDungeon(user);
        }

        model.addAttribute("user", user);
        model.addAttribute("dungeon", dungeon);
        return "shop";
    }

    @PostMapping("/shop/buy")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> buyItem(
            @RequestParam("item") String item, Principal principal) {
        String username = principal.getName();
        User user = userService.findByUsername(username);
        Dungeon dungeon = dungeonRepository.findByUserId(user.getId()).orElse(null);

        if (dungeon == null) {
            dungeon = dungeonService.createDungeon(user);
        }

        boolean success = false;
        String message = "";

        switch (item) {
            case "potion":
                if (user.getGold() >= 30) {
                    user.setGold(user.getGold() - 30);
                    user.setCurrentHp(Math.min(user.getCurrentHp() + 20, user.getMaxHp()));
                    success = true;
                    message = "Potion achetée ! (+20 PV Max)";
                } else {
                    message = "Pas assez d'or !";
                }
                break;
            case "super_potion":
                if (user.getGold() >= 67) {
                    user.setGold(user.getGold() - 67);
                    user.setCurrentHp(Math.min(user.getCurrentHp() + 60, user.getMaxHp()));
                    success = true;
                    message = "Super Potion achetée ! (+60 PV Max)";
                } else {
                    message = "Pas assez d'or !";
                }
                break;
            case "cannon":
                if (user.getGold() >= 100) {
                    user.setGold(user.getGold() - 100);
                    dungeon.setDamageBoost(dungeon.getDamageBoost() + 20);
                    success = true;
                    message = "Cannon acheté ! (+20% Dégâts Donjon)";
                } else {
                    message = "Pas assez d'or !";
                }
                break;
            case "chili":
                if (user.getGold() >= 200) {
                    if (user.getCurrentHp() > 50) {

                        user.setGold(user.getGold() - 200);
                        user.setCurrentHp(user.getCurrentHp() - 50);

                        dungeon.setDamageBoost(dungeon.getDamageBoost() + 50);
                        success = true;
                        message = "Piment acheté ! (-50 PV, +50% Dégâts Donjon)";
                    } else {
                        message = "Pas assez de PV !";
                    }
                } else {
                    message = "Pas assez d'or !";
                }
                break;
            case "theme_tiki":
            case "theme_lava":
            case "theme_gold":
                if (user.getGold() >= 1000) {
                    user.setGold(user.getGold() - 1000);
                    dungeon.setCosmeticTheme(item.replace("theme_", ""));
                    success = true;
                    message = "Thème " + item.replace("theme_", "") + " acheté !";
                } else {
                    message = "Pas assez d'or !";
                }
                break;
            default:
                message = "Item inconnu";
        }

        if (success) {
            userRepository.save(user);
            dungeonRepository.save(dungeon);
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("gold", user.getGold());
        response.put("currentHp", user.getCurrentHp());
        response.put("maxHp", user.getMaxHp());
        response.put("damageBoost", dungeon.getDamageBoost());

        return org.springframework.http.ResponseEntity.ok(response);
    }
}
