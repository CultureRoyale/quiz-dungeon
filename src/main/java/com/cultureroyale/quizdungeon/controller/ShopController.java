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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String buyItem(@RequestParam("item") String item, Principal principal,
            RedirectAttributes redirectAttributes) {
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
                if (user.getGold() >= 50) {
                    user.setGold(user.getGold() - 50);
                    user.setCurrentHp(Math.min(user.getCurrentHp() + 20, user.getMaxHp()));
                    success = true;
                    message = "Potion achetée ! (+20 PV Max)";
                } else {
                    message = "Pas assez d'or !";
                }
                break;
            case "dagger":
                if (user.getGold() >= 100) {
                    user.setGold(user.getGold() - 100);
                    dungeon.setDamageBoost(dungeon.getDamageBoost() + 20);
                    success = true;
                    message = "Dague achetée ! (+20% Dégâts Donjon)";
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
            redirectAttributes.addFlashAttribute("success", message);
        } else {
            redirectAttributes.addFlashAttribute("error", message);
        }

        return "redirect:/shop";
    }
}
