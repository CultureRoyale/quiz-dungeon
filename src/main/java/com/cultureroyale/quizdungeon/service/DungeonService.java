package com.cultureroyale.quizdungeon.service;

import com.cultureroyale.quizdungeon.model.Dungeon;
import com.cultureroyale.quizdungeon.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DungeonService {

    private final com.cultureroyale.quizdungeon.repository.DungeonRepository dungeonRepository;
    private final com.cultureroyale.quizdungeon.repository.DungeonQuestionRepository dungeonQuestionRepository;
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
        // Clear existing questions
        dungeonQuestionRepository.deleteByDungeonId(dungeon.getId());
        dungeon.getDungeonQuestions().clear();
        dungeonRepository.save(dungeon);

        // Add new questions
        int position = 1;
        for (Long qId : questionIds) {
            com.cultureroyale.quizdungeon.model.Question q = questionRepository.findById(qId).orElse(null);
            if (q != null) {
                com.cultureroyale.quizdungeon.model.DungeonQuestion dq = com.cultureroyale.quizdungeon.model.DungeonQuestion
                        .builder()
                        .dungeon(dungeon)
                        .question(q)
                        .position(position++)
                        .build();
                dungeonQuestionRepository.save(dq);
            }
        }
    }
}
