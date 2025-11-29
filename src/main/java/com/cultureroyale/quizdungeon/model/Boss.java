package com.cultureroyale.quizdungeon.model;

import com.cultureroyale.quizdungeon.model.enums.Difficulty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bosses")
@Data
@NoArgsConstructor
public class Boss {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(nullable = false)
    private int maxHp;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String categories;

    @Column(nullable = false)
    private int nbCategories;

    @Column(nullable = false)
    private int goldReward;

    @Column(nullable = false)
    private String imagePath;
}
