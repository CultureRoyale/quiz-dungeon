package com.cultureroyale.quizdungeon.service;

import com.cultureroyale.quizdungeon.model.Dungeon;
import com.cultureroyale.quizdungeon.model.User;
import lombok.RequiredArgsConstructor;
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

    public Dungeon getDungeonByUser(User user) {
        return dungeonRepository.findByUserId(user.getId()).orElseGet(() -> createDungeon(user));
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateDungeonQuestions(Dungeon dungeon, java.util.List<Long> questionIds) {
        // Clear existing questions (triggers orphanRemoval)
        dungeon.getDungeonQuestions().clear();
        dungeonRepository.saveAndFlush(dungeon); // Force delete to happen before insert

        // Add new questions
        int position = 1;
        java.util.Set<Long> addedQuestionIds = new java.util.HashSet<>();

        for (Long qId : questionIds) {
            // Prevent duplicates
            if (addedQuestionIds.contains(qId)) {
                continue;
            }

            com.cultureroyale.quizdungeon.model.Question q = questionRepository.findById(qId).orElse(null);
            if (q != null) {
                com.cultureroyale.quizdungeon.model.DungeonQuestion dq = com.cultureroyale.quizdungeon.model.DungeonQuestion
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
