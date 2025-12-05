package com.cultureroyale.quizdungeon.service;

import com.cultureroyale.quizdungeon.model.Achievement;
import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.model.UserAchievement;
import com.cultureroyale.quizdungeon.repository.AchievementRepository;
import com.cultureroyale.quizdungeon.repository.UserAchievementRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;

    @PostConstruct
    public void initAchievements() {
        createOrUpdateAchievement("Tueur de Boss", "Vaincre 5 boss", "/images/achievements/boss_killer5.png",
                "BOSS_KILLS", 5);
        createOrUpdateAchievement("Encyclopédie", "Répondre à 100 questions",
                "/images/achievements/encyclopedia100.png", "QUESTIONS_ANSWERED", 100);
        createOrUpdateAchievement("Pilleur", "Piller 1000 pièces d'or", "/images/achievements/looter1000.png",
                "GOLD_COLLECTED", 1000);
        createOrUpdateAchievement("Explorateur", "Piller 10 donjons", "/images/achievements/explorer10.png",
                "DUNGEONS_LOOTED", 10);
    }

    private void createOrUpdateAchievement(String name, String description, String iconPath, String conditionType,
            int conditionValue) {
        Achievement achievement = achievementRepository.findByName(name)
                .orElse(Achievement.builder()
                        .name(name)
                        .description(description)
                        .conditionType(conditionType)
                        .conditionValue(conditionValue)
                        .build());

        achievement.setIconPath(iconPath);
        // Ensure other fields are also up to date if needed, but iconPath is the main
        // request
        achievement.setDescription(description);
        achievement.setConditionType(conditionType);
        achievement.setConditionValue(conditionValue);

        achievementRepository.save(achievement);
    }

    @Transactional
    public void checkAndUnlock(User user, String type, int currentValue) {
        List<Achievement> achievements = achievementRepository.findAll();
        for (Achievement achievement : achievements) {
            if (achievement.getConditionType().equals(type) && currentValue >= achievement.getConditionValue()) {
                if (!userAchievementRepository.existsByUserIdAndAchievementId(user.getId(), achievement.getId())) {
                    UserAchievement userAchievement = UserAchievement.builder()
                            .user(user)
                            .achievement(achievement)
                            .unlockedAt(LocalDateTime.now())
                            .build();
                    userAchievementRepository.save(userAchievement);
                }
            }
        }
    }

    public List<Achievement> getAllAchievements() {
        return achievementRepository.findAll();
    }

    public List<UserAchievement> getUserAchievements(User user) {
        return userAchievementRepository.findByUserId(user.getId());
    }
}
