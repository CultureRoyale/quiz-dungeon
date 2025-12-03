package com.cultureroyale.quizdungeon.controller;

import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class LeaderboardController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/leaderboard")
    public String leaderboard(Model model) {
        List<User> topUsers = userRepository.findTop10ByOrderByGoldDesc();
        model.addAttribute("users", topUsers);
        return "leaderboard";
    }
}
