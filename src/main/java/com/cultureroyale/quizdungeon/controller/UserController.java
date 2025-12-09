package com.cultureroyale.quizdungeon.controller;

import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/update")
    public String updatePassword(@RequestParam String newPassword, @RequestParam String confirmPassword,
            Authentication authentication, RedirectAttributes redirectAttributes) {

        if (newPassword == null || newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Le mot de passe doit contenir au moins 6 caractères.");
            return "redirect:/profile";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Les mots de passe ne correspondent pas.");
            return "redirect:/profile";
        }

        String username = authentication.getName();
        User user = userService.findByUsername(username);
        userService.updatePassword(user, newPassword);

        redirectAttributes.addFlashAttribute("message", "Mot de passe mis à jour avec succès.");
        return "redirect:/profile";
    }

    @DeleteMapping("/delete")
    public String deleteAccount(Authentication authentication, HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);

        userService.deleteUser(user);

        // Logout manually
        SecurityContextHolder.clearContext();
        request.getSession().invalidate();

        redirectAttributes.addFlashAttribute("message", "Votre compte a été supprimé.");
        return "redirect:/login";
    }
}
