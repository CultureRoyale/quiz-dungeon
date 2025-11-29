package com.cultureroyale.quizdungeon.service;

import com.cultureroyale.quizdungeon.model.Boss;
import com.cultureroyale.quizdungeon.model.enums.Category;
import com.cultureroyale.quizdungeon.model.enums.Difficulty;
import com.cultureroyale.quizdungeon.repository.BossRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataInitializationService {

    private final BossRepository bossRepository;

    @PostConstruct
    public void initializeBosses() {
        if (bossRepository.count() > 0) {
            System.out.println("✅ Boss déjà initialisés (" + bossRepository.count() + " boss)");
            return;
        }

        System.out.println("📡 Initialisation des 9 boss...");

        createBoss(1, "Gobelin", Difficulty.FACILE, 100, 3, 50, "gobelin.png");
        createBoss(2, "Barbare", Difficulty.FACILE, 150, 3, 75, "barbare.png");
        createBoss(3, "Chevalier", Difficulty.FACILE, 200, 3, 100, "chevalier.png");
        createBoss(4, "Géant", Difficulty.MOYEN, 300, 6, 150, "geant.png");
        createBoss(5, "Valkyrie", Difficulty.MOYEN, 400, 6, 200, "valkyrie.png");
        createBoss(6, "Bébé Dragon", Difficulty.MOYEN, 500, 6, 250, "bebe_dragon.png");
        createBoss(7, "Squelette Géant", Difficulty.DIFFICILE, 600, 9, 300, "squelette_geant.png");
        createBoss(8, "P.E.K.K.A", Difficulty.DIFFICILE, 800, 9, 400, "pekka.png");
        createBoss(9, "Méga Chevalier", Difficulty.DIFFICILE, 1000, 9, 500, "mega_chevalier.png");

        System.out.println("✅ 9 boss initialisés avec succès");
    }

    private void createBoss(int position, String name, Difficulty difficulty, int maxHp,
                           int nbCategories, int goldReward, String imageFilename) {
        Boss boss = new Boss();
        boss.setPosition(position);
        boss.setName(name);
        boss.setDifficulty(difficulty);
        boss.setMaxHp(maxHp);
        boss.setNbCategories(nbCategories);
        boss.setGoldReward(goldReward);
        boss.setImagePath("/images/boss/" + imageFilename);

        String categories = selectRandomCategories(nbCategories);
        boss.setCategories(categories);

        bossRepository.save(boss);
    }

    private String selectRandomCategories(int count) {
        List<Category> allCategories = new ArrayList<>(Arrays.asList(Category.values()));
        Collections.shuffle(allCategories);

        return allCategories.stream()
                .limit(count)
                .map(Category::getValue)
                .collect(Collectors.joining(","));
    }
}
