package com.cultureroyale.quizdungeon.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false, length = 60)
    private String password; // BCrypt hash

    private int level;
    private int xp;
    private int maxHp;
    private int currentHp;
    private int gold;
    private int stolenGold;
    private int bossKills;
    private int dungeonsLooted;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Transient
    private int attack;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Dungeon dungeon;

    @ManyToOne
    private Dungeon currentOpponentDungeon;

    private LocalDateTime currentOpponentDungeonAssignedAt;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<UserQuestion> unlockedQuestions = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<UserAchievement> achievements = new java.util.ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
