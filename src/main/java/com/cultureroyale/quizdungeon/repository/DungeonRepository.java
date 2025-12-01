package com.cultureroyale.quizdungeon.repository;

import com.cultureroyale.quizdungeon.model.Dungeon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface DungeonRepository extends JpaRepository<Dungeon, Long> {
    Optional<Dungeon> findByUserId(Long userId);

    @Query(value = "SELECT * FROM dungeons WHERE user_id != :userId ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<Dungeon> findRandomDungeonExceptUser(@Param("userId") Long userId);
}
