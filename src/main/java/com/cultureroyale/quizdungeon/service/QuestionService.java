package com.cultureroyale.quizdungeon.service;

import com.cultureroyale.quizdungeon.model.Boss;
import com.cultureroyale.quizdungeon.model.Question;
import com.cultureroyale.quizdungeon.model.enums.Difficulty;
import com.cultureroyale.quizdungeon.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    public Optional<Question> getQuestionForBoss(Boss boss, List<Long> usedQuestionIds) {
        // Parse categories
        String[] cats = boss.getCategories().split(",");
        List<String> categories = java.util.Arrays.stream(cats)
                .map(String::trim)
                .collect(Collectors.toList());

        // Try to find questions with matching difficulty first
        List<Question> candidates = questionRepository.findByCategoryInAndDifficulty(categories, boss.getDifficulty());

        // Filter out used questions
        List<Question> available = candidates.stream()
                .filter(q -> !usedQuestionIds.contains(q.getId()))
                .collect(Collectors.toList());

        if (!available.isEmpty()) {
            Collections.shuffle(available);
            return Optional.of(available.get(0));
        }

        // Fallback: Try any difficulty if no matching difficulty questions are
        // available (or all used)
        candidates = questionRepository.findByCategoryIn(categories);
        available = candidates.stream()
                .filter(q -> !usedQuestionIds.contains(q.getId()))
                .collect(Collectors.toList());

        if (!available.isEmpty()) {
            Collections.shuffle(available);
            return Optional.of(available.get(0));
        }

        return Optional.empty();
    }
}
