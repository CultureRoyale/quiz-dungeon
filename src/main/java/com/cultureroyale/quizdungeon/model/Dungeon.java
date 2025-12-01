package com.cultureroyale.quizdungeon.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dungeons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dungeon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Builder.Default
    private String cosmeticTheme = "default";

    @Builder.Default
    private int damageBoost = 0;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @ToString.Exclude
    private User user;

    @OneToMany(mappedBy = "dungeon", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DungeonQuestion> dungeonQuestions = new ArrayList<>();
}