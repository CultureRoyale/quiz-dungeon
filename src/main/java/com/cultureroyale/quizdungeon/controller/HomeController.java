package com.cultureroyale.quizdungeon.controller;

import com.cultureroyale.quizdungeon.model.Dungeon;
import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.repository.DungeonRepository;
import com.cultureroyale.quizdungeon.service.DungeonService;
import com.cultureroyale.quizdungeon.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;
    private final DungeonRepository dungeonRepository;
    private final DungeonService dungeonService;

    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }

    @GetMapping("/login")
    public String login(Model model,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String registered,
            jakarta.servlet.http.HttpServletRequest request) {

        // Force session creation to ensure CSRF token can be generated
        request.getSession(true);

        if (error != null) {
            model.addAttribute("error", "Identifiant ou mot de passe incorrect");
        }
        if (logout != null) {
            model.addAttribute("message", "Déconnecté avec succès");
        }
        if (registered != null) {
            model.addAttribute("message", "Inscription effectuée avec succès");
        }

        return "login";
    }

    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {
        String username = principal.getName();
        User user = userService.findByUsername(username);
        Dungeon dungeon = dungeonRepository.findByUserId(user.getId()).orElse(null);

        if (dungeon == null) {
            dungeon = dungeonService.createDungeon(user);
        }

        model.addAttribute("user", user);
        model.addAttribute("dungeon", dungeon);
        return "user-profile";
    }
}