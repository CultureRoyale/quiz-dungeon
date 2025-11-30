package com.cultureroyale.quizdungeon.controller;

import com.cultureroyale.quizdungeon.model.User;
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

    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }

    @GetMapping("/login")
    public String login(Model model,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String registered) {

        if (error != null) {
            model.addAttribute("error", "Identifiant ou mot de passe incorrect");
        }
        if (logout != null) {
            model.addAttribute("message", "Vous avez été déconnecté avec succès");
        }
        if (registered != null) {
            model.addAttribute("message", "Inscription réussie ! Vous pouvez maintenant vous connecter");
        }

        return "login";
    }


    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {
        String username = principal.getName();
        User user = userService.findByUsername(username);
        model.addAttribute("user", user);
        return "user-profile";
    }
}