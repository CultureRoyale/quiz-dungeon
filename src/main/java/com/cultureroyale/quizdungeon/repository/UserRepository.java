package com.cultureroyale.quizdungeon.repository;

import com.cultureroyale.quizdungeon.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    java.util.List<User> findTop10ByOrderByGoldDesc();
}
