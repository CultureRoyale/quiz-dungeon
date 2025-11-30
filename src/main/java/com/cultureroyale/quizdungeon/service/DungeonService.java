package com.cultureroyale.quizdungeon.service;

import com.cultureroyale.quizdungeon.model.Dungeon;
import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.repository.DungeonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DungeonService {

    private final DungeonRepository dungeonRepository;

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
}
