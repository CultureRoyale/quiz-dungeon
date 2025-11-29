package com.cultureroyale.quizdungeon.repository;

import com.cultureroyale.quizdungeon.model.Boss;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BossRepository extends JpaRepository<Boss, Long> {
    Optional<Boss> findByPosition(int position);
    List<Boss> findAllByOrderByPositionAsc();
}
