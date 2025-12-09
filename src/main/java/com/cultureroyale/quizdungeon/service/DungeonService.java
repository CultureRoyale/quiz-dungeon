package com.cultureroyale.quizdungeon.service;

import com.cultureroyale.quizdungeon.model.Dungeon;
import com.cultureroyale.quizdungeon.model.DungeonQuestion;
import com.cultureroyale.quizdungeon.model.Question;
import com.cultureroyale.quizdungeon.model.User;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DungeonService {

    private final com.cultureroyale.quizdungeon.repository.DungeonRepository dungeonRepository;

    private final com.cultureroyale.quizdungeon.repository.QuestionRepository questionRepository;

    public Dungeon createDungeon(User user) {
        Dungeon dungeon = Dungeon.builder()
                .name("Donjon de " + user.getUsername())
                .user(user)
                .build();
        return dungeonRepository.save(dungeon);
    }

    public Dungeon save(Dungeon dungeon) {
        return dungeonRepository.save(dungeon);
    }

    public Dungeon getRandomDungeon(User excludedUser) {
        return dungeonRepository.findRandomDungeonExceptUser(excludedUser.getId()).orElse(null);
    }

    public Dungeon getDungeonByUser(User user) {
        return dungeonRepository.findByUserId(user.getId()).orElseGet(() -> createDungeon(user));
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateDungeonQuestions(Dungeon dungeon, List<Long> questionIds) {
        // Clear existing questions (triggers orphanRemoval)
        dungeon.getDungeonQuestions().clear();
        dungeonRepository.saveAndFlush(dungeon); // Force delete to happen before insert

        // Add new questions
        int position = 1;
        Set<Long> addedQuestionIds = new HashSet<>();

        for (Long qId : questionIds) {
            // Prevent duplicates
            if (addedQuestionIds.contains(qId)) {
                continue;
            }

            Question q = questionRepository.findById(qId).orElse(null);
            if (q != null) {
                DungeonQuestion dq = DungeonQuestion
                        .builder()
                        .dungeon(dungeon)
                        .question(q)
                        .position(position++)
                        .build();
                dungeon.getDungeonQuestions().add(dq);
                addedQuestionIds.add(qId);
            }
        }

        dungeonRepository.save(dungeon);
    }
}
