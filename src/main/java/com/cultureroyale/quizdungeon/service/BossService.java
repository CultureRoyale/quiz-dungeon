package com.cultureroyale.quizdungeon.service;

import com.cultureroyale.quizdungeon.model.Boss;
import com.cultureroyale.quizdungeon.model.enums.CombatType;
import com.cultureroyale.quizdungeon.model.enums.CombatResult;
import com.cultureroyale.quizdungeon.repository.BossRepository;
import com.cultureroyale.quizdungeon.repository.CombatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BossService {

    private final BossRepository bossRepository;
    private final CombatRepository combatRepository;

    public List<Boss> getAllBosses() {
        return bossRepository.findAllByOrderByPositionAsc();
    }

    public Boss getBossById(Long id) {
        return bossRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Boss non trouvé"));
    }

    public Boss getBossByPosition(int position) {
        return bossRepository.findByPosition(position)
                .orElseThrow(() -> new IllegalArgumentException("Boss position " + position + " non trouvé"));
    }

    public boolean isBossUnlocked(Long userId, Boss boss) {
        if (boss.getPosition() == 1) {
            return true;
        }

        Boss previousBoss = getBossByPosition(boss.getPosition() - 1);

        return combatRepository.existsByUserIdAndTypeAndBossIdAndResult(
                userId,
                CombatType.BOSS,
                previousBoss.getId(),
                CombatResult.VICTOIRE);
    }

    public Map<Long, Boolean> getBossUnlockStatus(Long userId) {
        List<Boss> allBosses = getAllBosses();
        Map<Long, Boolean> statusMap = new HashMap<>();

        for (Boss boss : allBosses) {
            statusMap.put(boss.getId(), isBossUnlocked(userId, boss));
        }

        return statusMap;
    }

    public boolean isBossDefeated(Long userId, Boss boss) {
        return combatRepository.existsByUserIdAndTypeAndBossIdAndResult(
                userId,
                CombatType.BOSS,
                boss.getId(),
                CombatResult.VICTOIRE);
    }
}
