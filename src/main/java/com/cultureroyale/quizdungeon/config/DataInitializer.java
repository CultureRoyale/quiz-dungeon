package com.cultureroyale.quizdungeon.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // Ensure columns exist (in case Hibernate ddl-auto didn't run or failed)
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS stolen_gold INT DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS boss_kills INT DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS dungeons_looted INT DEFAULT 0");
        } catch (Exception e) {
            System.out.println("Warning: Could not alter table users: " + e.getMessage());
        }

        // Initialize new columns to 0 if they are null
        jdbcTemplate.update("UPDATE users SET stolen_gold = 0 WHERE stolen_gold IS NULL");
        jdbcTemplate.update("UPDATE users SET boss_kills = 0 WHERE boss_kills IS NULL");
        jdbcTemplate.update("UPDATE users SET dungeons_looted = 0 WHERE dungeons_looted IS NULL");

        System.out.println("User statistics initialized (columns ensured and NULL values set to 0)");
    }
}
