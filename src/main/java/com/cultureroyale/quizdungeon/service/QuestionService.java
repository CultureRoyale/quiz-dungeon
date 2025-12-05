package com.cultureroyale.quizdungeon.service;

import com.cultureroyale.quizdungeon.model.Boss;
import com.cultureroyale.quizdungeon.model.Question;
import com.cultureroyale.quizdungeon.repository.QuestionRepository;
import com.cultureroyale.quizdungeon.repository.UserQuestionRepository;
import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.model.UserQuestion;
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

    @Autowired
    private UserQuestionRepository userQuestionRepository;

    @Autowired
    private AchievementService achievementService;

    public void unlockQuestion(User user, Question question) {
        if (!userQuestionRepository.existsByUserIdAndQuestionId(user.getId(), question.getId())) {
            UserQuestion uq = UserQuestion
                    .builder()
                    .user(user)
                    .question(question)
                    .build();
            userQuestionRepository.save(uq);

            // Check achievement
            int unlockedCount = userQuestionRepository.countByUserId(user.getId());
            achievementService.checkAndUnlock(user, "QUESTIONS_ANSWERED", unlockedCount);
        }
    }

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
