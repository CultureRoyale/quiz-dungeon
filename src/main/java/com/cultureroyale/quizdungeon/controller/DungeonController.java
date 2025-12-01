package com.cultureroyale.quizdungeon.controller;

import com.cultureroyale.quizdungeon.model.Dungeon;
import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.model.UserQuestion;
import com.cultureroyale.quizdungeon.repository.UserRepository;
import com.cultureroyale.quizdungeon.repository.UserQuestionRepository;
import com.cultureroyale.quizdungeon.service.DungeonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/dungeon")
public class DungeonController {

    @Autowired
    private DungeonService dungeonService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserQuestionRepository userQuestionRepository;

    @GetMapping("/edit")
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

    @PostMapping("/save")
    @ResponseBody
    public String saveDungeon(@RequestBody List<Long> questionIds, Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        Dungeon dungeon = dungeonService.getDungeonByUser(user);

        dungeonService.updateDungeonQuestions(dungeon, questionIds);

        return "OK";
    }
}
