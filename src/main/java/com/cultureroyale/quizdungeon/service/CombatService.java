package com.cultureroyale.quizdungeon.service;

import com.cultureroyale.quizdungeon.model.Boss;
import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.model.enums.HelpLevel;
import com.cultureroyale.quizdungeon.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import com.cultureroyale.quizdungeon.model.Combat;
import com.cultureroyale.quizdungeon.model.Question;
import com.cultureroyale.quizdungeon.repository.CombatRepository;
import com.cultureroyale.quizdungeon.model.dto.TurnResult;
import com.cultureroyale.quizdungeon.model.enums.CombatResult;

import java.util.ArrayList;

import java.util.List;
import java.util.Optional;

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
    private UserService userService;

    @Autowired
    private AchievementService achievementService;

    public void startCombat(User user, Boss boss, HttpSession session) {
        session.setAttribute("combat_boss", boss);
        session.setAttribute("combat_boss_current_hp", boss.getMaxHp());
        session.setAttribute("combat_user_current_hp", user.getCurrentHp());
        session.setAttribute("combat_used_questions", new ArrayList<Long>());

        // Initial question
        loadNextQuestion(boss, session);
    }

    private void loadNextQuestion(Boss boss, HttpSession session) {
        @SuppressWarnings("unchecked")
        List<Long> usedQuestionIds = (List<Long>) session.getAttribute("combat_used_questions");
        if (usedQuestionIds == null) {
            usedQuestionIds = new ArrayList<>();
            session.setAttribute("combat_used_questions", usedQuestionIds);
        }

        Optional<Question> questionOpt = questionService.getQuestionForBoss(boss, usedQuestionIds);

        if (questionOpt.isPresent()) {
            Question question = questionOpt.get();
            session.setAttribute("combat_current_question", question);
            usedQuestionIds.add(question.getId());
        } else {
            usedQuestionIds.clear();
            questionOpt = questionService.getQuestionForBoss(boss, usedQuestionIds);
            if (questionOpt.isPresent()) {
                Question question = questionOpt.get();
                session.setAttribute("combat_current_question", question);
                usedQuestionIds.add(question.getId());
            } else {
                throw new RuntimeException("No questions found for boss categories even after reset.");
            }
        }
    }

    public Question getCurrentQuestion(HttpSession session) {
        return (Question) session.getAttribute("combat_current_question");
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

    public TurnResult processTurn(User user, Boss boss, boolean isCorrect, HelpLevel helpLevel,
            HttpSession session) {
        int bossHp = (int) session.getAttribute("combat_boss_current_hp");
        int userHp = user.getCurrentHp();

        String message;

        if (isCorrect) {
            // Unlock question
            Question currentQuestion = getCurrentQuestion(session);
            if (currentQuestion != null) {
                questionService.unlockQuestion(user, currentQuestion);
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
            return new TurnResult(CombatResult.VICTOIRE, message, bossHp, userHp);
        } else if (userHp <= 0) {
            return new TurnResult(CombatResult.DEFAITE, message, bossHp, userHp);
        }

        // Load next question
        loadNextQuestion(boss, session);

        return new TurnResult(CombatResult.EN_COURS, message, bossHp, userHp);
    }

    public void handleVictory(User user, Boss boss) {
        // Gold reward from boss
        user.setGold(user.getGold() + boss.getGoldReward());

        // XP reward based on boss position
        int xpReward = boss.getPosition() * 50;
        userService.addXp(user, xpReward); // This handles level up and saving

        // Update Stats & Achievements
        user.setBossKills(user.getBossKills() + 1);
        achievementService.checkAndUnlock(user, "BOSS_KILLS", user.getBossKills());

        Combat combat = new Combat();
        combat.setUser(user);
        combat.setBoss(boss);
        combat.setType(com.cultureroyale.quizdungeon.model.enums.CombatType.BOSS);
        combat.setResult(com.cultureroyale.quizdungeon.model.enums.CombatResult.VICTOIRE);
        combat.setGoldEarned(boss.getGoldReward());
        combat.setXpEarned(xpReward);
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
}
