package com.cultureroyale.quizdungeon.model;

import com.cultureroyale.quizdungeon.model.enums.CombatType;
import com.cultureroyale.quizdungeon.model.enums.CombatResult;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "combats")
@Data
@NoArgsConstructor
public class Combat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CombatType type;

    @ManyToOne
    @JoinColumn(name = "boss_id")
    private Boss boss;

    @ManyToOne
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CombatResult result;

    private int damageDealt;
    private int hpLost;
    private int goldEarned;
    private int xpEarned;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
