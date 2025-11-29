package com.cultureroyale.quizdungeon.controller;

import com.cultureroyale.quizdungeon.model.Boss;
import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.service.BossService;
import com.cultureroyale.quizdungeon.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class RoadmapController {

    private final UserService userService;
    private final BossService bossService;

    @GetMapping("/roadmap")
    public String roadmap(Model model, Principal principal) {
        String username = principal.getName();
        User user = userService.findByUsername(username);

        List<Boss> bosses = bossService.getAllBosses();

        Map<Long, Boolean> unlockStatus = bossService.getBossUnlockStatus(user.getId());
        Map<Long, Boolean> defeatedStatus = new HashMap<>();

        for (Boss boss : bosses) {
            defeatedStatus.put(boss.getId(), bossService.isBossDefeated(user.getId(), boss));
        }

        model.addAttribute("user", user);
        model.addAttribute("bosses", bosses);
        model.addAttribute("unlockStatus", unlockStatus);
        model.addAttribute("defeatedStatus", defeatedStatus);

        return "roadmap";
    }
}
