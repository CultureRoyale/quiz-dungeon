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

    @ElementCollection(targetClass = com.cultureroyale.quizdungeon.model.enums.Category.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "boss_categories", joinColumns = @JoinColumn(name = "boss_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private java.util.Set<com.cultureroyale.quizdungeon.model.enums.Category> categories = new java.util.HashSet<>();

    @Column(nullable = false)
    private int goldReward;

    @Column(nullable = false)
    private String imagePath;

    @Column(nullable = false)
    private int attack;
}
