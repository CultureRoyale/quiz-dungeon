package com.cultureroyale.quizdungeon.repository;

import com.cultureroyale.quizdungeon.model.Question;
import com.cultureroyale.quizdungeon.model.enums.Difficulty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    Optional<Question> findByExternalId(String externalId);

    List<Question> findByCategoryAndDifficulty(String category, Difficulty difficulty);

    // Find all questions by categories and difficulty
    List<Question> findByCategoryInAndDifficulty(List<String> categories, Difficulty difficulty);

    // Find all questions by categories
    List<Question> findByCategoryIn(List<String> categories);

    @Query(value = "SELECT * FROM questions q WHERE q.category IN :categories ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<Question> findRandomByCategoryIn(@Param("categories") List<String> categories);
}
