package com.cultureroyale.quizdungeon.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

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

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Dungeon dungeon;

    @ManyToOne
    private Dungeon currentOpponentDungeon;

    private LocalDateTime currentOpponentDungeonAssignedAt;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserQuestion> unlockedQuestions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserAchievement> achievements = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Combat> combats = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
