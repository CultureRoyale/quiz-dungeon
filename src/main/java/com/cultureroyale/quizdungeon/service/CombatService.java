package com.cultureroyale.quizdungeon.service;

import com.cultureroyale.quizdungeon.model.Boss;
import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.model.enums.HelpLevel;
import com.cultureroyale.quizdungeon.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import com.cultureroyale.quizdungeon.model.Combat;
import com.cultureroyale.quizdungeon.repository.CombatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CombatService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CombatRepository combatRepository;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private com.cultureroyale.quizdungeon.repository.UserQuestionRepository userQuestionRepository;

    public void startCombat(User user, Boss boss, HttpSession session) {
        session.setAttribute("combat_boss", boss);
        session.setAttribute("combat_boss_current_hp", boss.getMaxHp());
        session.setAttribute("combat_user_current_hp", user.getCurrentHp());
        session.setAttribute("combat_used_questions", new java.util.ArrayList<Long>());

        // Initial question
        loadNextQuestion(boss, session);
    }

    private void loadNextQuestion(Boss boss, HttpSession session) {
        @SuppressWarnings("unchecked")
        java.util.List<Long> usedQuestionIds = (java.util.List<Long>) session.getAttribute("combat_used_questions");
        if (usedQuestionIds == null) {
            usedQuestionIds = new java.util.ArrayList<>();
            session.setAttribute("combat_used_questions", usedQuestionIds);
        }

        java.util.Optional<com.cultureroyale.quizdungeon.model.Question> questionOpt = questionService
                .getQuestionForBoss(boss, usedQuestionIds);

        if (questionOpt.isPresent()) {
            com.cultureroyale.quizdungeon.model.Question question = questionOpt.get();
            session.setAttribute("combat_current_question", question);
            usedQuestionIds.add(question.getId());
        } else {
            // Handle case where no questions are available (e.g. all used)
            // If still empty, it means we used ALL questions for these categories.
            // Let's clear the used list and try again (looping).
            usedQuestionIds.clear();
            questionOpt = questionService.getQuestionForBoss(boss, usedQuestionIds);
            if (questionOpt.isPresent()) {
                com.cultureroyale.quizdungeon.model.Question question = questionOpt.get();
                session.setAttribute("combat_current_question", question);
                usedQuestionIds.add(question.getId());
            } else {
                throw new RuntimeException("No questions found for boss categories even after reset.");
            }
        }
    }

    public com.cultureroyale.quizdungeon.model.Question getCurrentQuestion(HttpSession session) {
        return (com.cultureroyale.quizdungeon.model.Question) session.getAttribute("combat_current_question");
    }

    public void unlockQuestion(User user, com.cultureroyale.quizdungeon.model.Question question) {
        if (!userQuestionRepository.existsByUserIdAndQuestionId(user.getId(), question.getId())) {
            com.cultureroyale.quizdungeon.model.UserQuestion uq = com.cultureroyale.quizdungeon.model.UserQuestion
                    .builder()
                    .user(user)
                    .question(question)
                    .build();
            userQuestionRepository.save(uq);
        }
    }

    public int calculateUserBaseAttack(Boss boss) {
        double difficultyMultiplier = 1.0;
        switch (boss.getDifficulty()) {
            case FACILE:
                difficultyMultiplier = 2.0;
                break;
            case MOYEN:
                difficultyMultiplier = 1.0;
                break;
            case DIFFICILE:
                difficultyMultiplier = 0.5;
                break;
        }
        return (int) ((boss.getMaxHp() / 20.0) * difficultyMultiplier);
    }

    public CombatResult processTurn(User user, Boss boss, boolean isCorrect, HelpLevel helpLevel, HttpSession session) {
        int bossHp = (int) session.getAttribute("combat_boss_current_hp");
        int userHp = user.getCurrentHp();

        String message;

        if (isCorrect) {
            // Unlock question
            com.cultureroyale.quizdungeon.model.Question currentQuestion = getCurrentQuestion(session);
            if (currentQuestion != null) {
                unlockQuestion(user, currentQuestion);
            }

            // User attacks
            int baseDamage = calculateUserBaseAttack(boss);
            int damageDealt = (int) (baseDamage * helpLevel.getDamageMultiplier());
            bossHp -= damageDealt;
            message = "Bonne réponse ! Vous infligez " + damageDealt + " dégâts.";
        } else {
            // Boss attacks
            int damageTaken = boss.getAttack();
            userHp -= damageTaken;
            message = "Mauvaise réponse ! Vous subissez " + damageTaken + " dégâts.";
        }

        // Update state
        session.setAttribute("combat_boss_current_hp", bossHp);
        user.setCurrentHp(userHp);
        userRepository.save(user);

        // Check win/loss
        if (bossHp <= 0) {
            return new CombatResult(CombatStatus.VICTORY, message, bossHp, userHp);
        } else if (userHp <= 0) {
            return new CombatResult(CombatStatus.DEFEAT, message, bossHp, userHp);
        }

        // Load next question
        loadNextQuestion(boss, session);

        return new CombatResult(CombatStatus.ONGOING, message, bossHp, userHp);
    }

    public void handleVictory(User user, Boss boss) {
        user.setGold(user.getGold() + boss.getGoldReward());
        userRepository.save(user);

        Combat combat = new Combat();
        combat.setUser(user);
        combat.setBoss(boss);
        combat.setType(com.cultureroyale.quizdungeon.model.enums.CombatType.BOSS);
        combat.setResult(com.cultureroyale.quizdungeon.model.enums.CombatResult.VICTOIRE);
        combat.setGoldEarned(boss.getGoldReward());
        combat.setXpEarned(100); // Valeur arbitraire d'XP
        combatRepository.save(combat);
    }

    public void handleDefeat(User user, Boss boss) {
        user.setGold(user.getGold() / 2);
        user.setCurrentHp(user.getMaxHp());
        userRepository.save(user);

        Combat combat = new Combat();
        combat.setUser(user);
        combat.setBoss(boss); // Peut être null si pas de boss, mais ici on est dans le contexte boss
        combat.setType(com.cultureroyale.quizdungeon.model.enums.CombatType.BOSS);
        combat.setResult(com.cultureroyale.quizdungeon.model.enums.CombatResult.DEFAITE);
        combat.setGoldEarned(0);
        combat.setXpEarned(10);
        combatRepository.save(combat);
    }

    // Helper classes
    public enum CombatStatus {
        ONGOING, VICTORY, DEFEAT
    }

    public static class CombatResult {
        public CombatStatus status;
        public String message;
        public int bossHp;
        public int userHp;

        public CombatResult(CombatStatus status, String message, int bossHp, int userHp) {
            this.status = status;
            this.message = message;
            this.bossHp = bossHp;
            this.userHp = userHp;
        }
    }
}
