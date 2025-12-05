package com.cultureroyale.quizdungeon.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String iconPath;

    // Type of action required to unlock (e.g., "BOSS_KILLS", "QUESTIONS_ANSWERED")
    private String conditionType;

    // Value required to unlock (e.g., 5, 100)
    private int conditionValue;
}
