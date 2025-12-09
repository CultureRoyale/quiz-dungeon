package com.cultureroyale.quizdungeon.repository;

import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.model.Dungeon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findTop10ByOrderByGoldDesc();

    List<User> findByCurrentOpponentDungeon(Dungeon dungeon);
}
