package com.cultureroyale.quizdungeon.repository;

import com.cultureroyale.quizdungeon.model.Question;
import com.cultureroyale.quizdungeon.model.enums.Difficulty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    Optional<Question> findByExternalId(String externalId);
    List<Question> findByCategoryAndDifficulty(String category, Difficulty difficulty);
}
