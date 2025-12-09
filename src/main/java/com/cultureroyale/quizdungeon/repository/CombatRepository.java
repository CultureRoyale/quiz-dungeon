package com.cultureroyale.quizdungeon.repository;

import com.cultureroyale.quizdungeon.model.Combat;
import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.model.enums.CombatType;
import com.cultureroyale.quizdungeon.model.enums.CombatResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CombatRepository extends JpaRepository<Combat, Long> {
    List<Combat> findByUserIdOrderByTimestampDesc(Long userId);

    boolean existsByUserIdAndTypeAndBossIdAndResult(
            Long userId,
            CombatType type,
            Long bossId,
            CombatResult result);

    List<Combat> findByTargetUser(User targetUser);
}
