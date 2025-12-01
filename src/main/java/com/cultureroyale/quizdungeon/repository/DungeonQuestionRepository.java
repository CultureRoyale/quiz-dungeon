package com.cultureroyale.quizdungeon.repository;

import com.cultureroyale.quizdungeon.model.DungeonQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DungeonQuestionRepository extends JpaRepository<DungeonQuestion, Long> {
    List<DungeonQuestion> findByDungeonIdOrderByPositionAsc(Long dungeonId);

    void deleteByDungeonId(Long dungeonId);
}
