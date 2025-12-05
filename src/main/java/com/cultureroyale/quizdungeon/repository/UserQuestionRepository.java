package com.cultureroyale.quizdungeon.repository;

import com.cultureroyale.quizdungeon.model.UserQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserQuestionRepository extends JpaRepository<UserQuestion, Long> {
    List<UserQuestion> findByUserId(Long userId);

    boolean existsByUserIdAndQuestionId(Long userId, Long questionId);

    int countByUserId(Long userId);
}
