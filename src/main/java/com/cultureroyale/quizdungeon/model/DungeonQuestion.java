package com.cultureroyale.quizdungeon.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dungeon_questions", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "dungeon_id", "question_id" }),
        @UniqueConstraint(columnNames = { "dungeon_id", "position" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DungeonQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dungeon_id", nullable = false)
    private Dungeon dungeon;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private int position;
}
