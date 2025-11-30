package com.cultureroyale.quizdungeon.repository;

import com.cultureroyale.quizdungeon.model.Dungeon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DungeonRepository extends JpaRepository<Dungeon, Long> {
    Optional<Dungeon> findByUserId(Long userId);
}
