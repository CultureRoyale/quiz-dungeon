package com.cultureroyale.quizdungeon.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.cultureroyale.quizdungeon.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model) {

        // Validation manuelle
        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("error", "L'identifiant est requis");
            model.addAttribute("username", username);
            return "register";
        }

        if (username.trim().length() < 3) {
            model.addAttribute("error", "L'identifiant doit contenir au moins 3 caractères");
            model.addAttribute("username", username);
            return "register";
        }

        if (password == null || password.isEmpty()) {
            model.addAttribute("error", "Le mot de passe est requis");
            model.addAttribute("username", username);
            return "register";
        }

        if (password.length() < 6) {
            model.addAttribute("error", "Le mot de passe doit contenir au moins 6 caractères");
            model.addAttribute("username", username);
            return "register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Les mots de passe ne correspondent pas");
            model.addAttribute("username", username);
            return "register";
        }

        // Enregistrement de l'utilisateur
        try {
            userService.registerUser(username.trim(), password);
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", username);
            return "register";
        }
    }
}
