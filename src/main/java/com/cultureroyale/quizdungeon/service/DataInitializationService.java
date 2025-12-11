package com.cultureroyale.quizdungeon.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.cultureroyale.quizdungeon.model.Boss;
import com.cultureroyale.quizdungeon.model.enums.Category;
import com.cultureroyale.quizdungeon.model.enums.Difficulty;
import com.cultureroyale.quizdungeon.repository.BossRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DataInitializationService {

    private final BossRepository bossRepository;
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initializeBosses() {
        initializeUserStats();
        if (bossRepository.count() > 0) {
            updateExistingBosses();
            return;
        }

        createBoss(1, "Gobelin", Difficulty.FACILE, 100, 5, 50, "gobelin.png");
        createBoss(2, "Barbare", Difficulty.FACILE, 150, 8, 75, "barbare.png");
        createBoss(3, "Chevalier", Difficulty.FACILE, 200, 12, 100, "chevalier.png");
        createBoss(4, "Géant", Difficulty.MOYEN, 300, 15, 150, "geant.png");
        createBoss(5, "Valkyrie", Difficulty.MOYEN, 400, 20, 200, "valkyrie.png");
        createBoss(6, "Bébé Dragon", Difficulty.MOYEN, 500, 25, 250, "bebe_dragon.png");
        createBoss(7, "Squelette Géant", Difficulty.DIFFICILE, 600, 30, 300, "squelette_geant.png");
        createBoss(8, "P.E.K.K.A", Difficulty.DIFFICILE, 800, 35, 400, "pekka.png");
        createBoss(9, "Méga Chevalier", Difficulty.DIFFICILE, 1000, 40, 500, "mega_chevalier.png");

    }

    private void initializeUserStats() {
        // Ensure columns exist
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS stolen_gold INT DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS boss_kills INT DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS dungeons_looted INT DEFAULT 0");
        } catch (DataAccessException e) {
            System.out.println("Warning: Could not alter table users: " + e.getMessage());
        }

        // Initialize new columns to 0 if they are null
        jdbcTemplate.update("UPDATE users SET stolen_gold = 0 WHERE stolen_gold IS NULL");
        jdbcTemplate.update("UPDATE users SET boss_kills = 0 WHERE boss_kills IS NULL");
        jdbcTemplate.update("UPDATE users SET dungeons_looted = 0 WHERE dungeons_looted IS NULL");
    }

    private void updateExistingBosses() {
        List<Boss> bosses = bossRepository.findAll();
        boolean updated = false;
        for (Boss boss : bosses) {
            int newAttack = 0;
            switch (boss.getPosition()) {
                case 1 -> newAttack = 5;
                case 2 -> newAttack = 8;
                case 3 -> newAttack = 12;
                case 4 -> newAttack = 15;
                case 5 -> newAttack = 20;
                case 6 -> newAttack = 25;
                case 7 -> newAttack = 30;
                case 8 -> newAttack = 35;
                case 9 -> newAttack = 40;
            }
            if (boss.getAttack() != newAttack) {
                boss.setAttack(newAttack);
                updated = true;
            }
        }
        if (updated) {
            bossRepository.saveAll(bosses);
        }

        // Migration: Check for bosses with no categories (due to schema change)
        boolean migrationNeeded = false;
        for (Boss boss : bosses) {
            if (boss.getCategories().isEmpty()) {
                int nbCategories = getCategoryCountForDifficulty(boss.getDifficulty());
                boss.setCategories(selectRandomCategories(nbCategories));
                migrationNeeded = true;
            }
        }
        if (migrationNeeded) {
            bossRepository.saveAll(bosses);
        }
    }

    private void createBoss(int position, String name, Difficulty difficulty, int maxHp, int attack,
            int goldReward, String imageFilename) {
        Boss boss = new Boss();
        boss.setPosition(position);
        boss.setName(name);
        boss.setDifficulty(difficulty);
        boss.setMaxHp(maxHp);
        boss.setAttack(attack);
        boss.setGoldReward(goldReward);
        boss.setImagePath("/images/boss/" + imageFilename);

        int nbCategories = getCategoryCountForDifficulty(difficulty);
        Set<Category> categories = selectRandomCategories(nbCategories);
        boss.setCategories(categories);

        bossRepository.save(boss);
    }

    private int getCategoryCountForDifficulty(Difficulty difficulty) {
        return switch (difficulty) {
            case FACILE -> 3;
            case MOYEN -> 6;
            case DIFFICILE -> 9;
            default -> 3;
        };
    }

    private Set<Category> selectRandomCategories(int count) {
        List<Category> allCategories = new ArrayList<>(Arrays.asList(Category.values()));
        Collections.shuffle(allCategories);

        return allCategories.stream()
                .limit(count)
                .collect(Collectors.toSet());
    }
}
