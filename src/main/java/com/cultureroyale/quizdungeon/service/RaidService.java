package com.cultureroyale.quizdungeon.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cultureroyale.quizdungeon.model.Combat;
import com.cultureroyale.quizdungeon.model.Dungeon;
import com.cultureroyale.quizdungeon.model.DungeonQuestion;
import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.model.dto.TurnResult;
import com.cultureroyale.quizdungeon.model.enums.CombatResult;
import com.cultureroyale.quizdungeon.model.enums.CombatType;
import com.cultureroyale.quizdungeon.model.enums.HelpLevel;
import com.cultureroyale.quizdungeon.repository.CombatRepository;
import com.cultureroyale.quizdungeon.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@Service
public class RaidService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private UserService userService;

    @Autowired
    private AchievementService achievementService;

    @Autowired
    private CombatRepository combatRepository;

    public void startRaid(User user, Dungeon dungeon, HttpSession session) {
        // Initialize Raid Session
        session.setAttribute("raid_dungeon", dungeon);
        session.setAttribute("raid_status", "ONGOING");

        // Dungeon HP = Number of questions * 10
        int dungeonMaxHp = dungeon.getDungeonQuestions().size() * 10;
        session.setAttribute("raid_dungeon_max_hp", dungeonMaxHp);
        session.setAttribute("raid_dungeon_current_hp", dungeonMaxHp);

        // Player HP (use user's current HP)
        session.setAttribute("raid_user_current_hp", user.getCurrentHp());

        // Load Questions (ordered by position)
        List<DungeonQuestion> questions = new ArrayList<>(dungeon.getDungeonQuestions());
        questions.sort(Comparator.comparingInt(DungeonQuestion::getPosition));
        session.setAttribute("raid_questions", questions);
        session.setAttribute("raid_current_question_index", 0);
        session.removeAttribute("raid_help_level");
    }

    public DungeonQuestion getCurrentRaidQuestion(HttpSession session) {
        @SuppressWarnings("unchecked")
        List<DungeonQuestion> questions = (List<DungeonQuestion>) session.getAttribute("raid_questions");
        Integer index = (Integer) session.getAttribute("raid_current_question_index");
        if (questions != null && index != null && index < questions.size()) {
            return questions.get(index);
        }
        return null;
    }

    public TurnResult processRaidTurn(User user, boolean isCorrect, HelpLevel helpLevel,
            HttpSession session) {
        int dungeonHp = (int) session.getAttribute("raid_dungeon_current_hp");
        int userHp = (int) session.getAttribute("raid_user_current_hp");

        String message;

        if (isCorrect) {
            // Unlock question and check achievement
            DungeonQuestion currentDQ = getCurrentRaidQuestion(session);
            if (currentDQ != null) {
                questionService.unlockQuestion(user, currentDQ.getQuestion());
            }

            // Damage to dungeon
            int damage = (int) (user.getAttack() * helpLevel.getDamageMultiplier());
            dungeonHp = Math.max(0, dungeonHp - damage);
            message = "Bonne réponse ! Vous infligez " + damage + " dégâts au donjon.";
        } else {
            // Damage to player
            // Dungeon parameter is no longer available in this method, so we need to
            // retrieve it from session or pass it.
            // Assuming dungeon is needed for calculateDungeonDamage, let's retrieve it from
            // session.
            Dungeon dungeon = (Dungeon) session.getAttribute("raid_dungeon");
            int dungeonAtk = calculateDungeonDamage(dungeon);
            userHp = Math.max(0, userHp - dungeonAtk);
            message = "Mauvaise réponse ! Vous subissez " + dungeonAtk + " dégâts.";
        }

        session.setAttribute("raid_dungeon_current_hp", dungeonHp);
        session.setAttribute("raid_user_current_hp", userHp);

        // Update User in database
        user.setCurrentHp(userHp);
        userRepository.save(user);

        if (dungeonHp <= 0) {
            return new TurnResult(CombatResult.VICTOIRE, message, dungeonHp, userHp);
        } else if (userHp <= 0) {
            return new TurnResult(CombatResult.DEFAITE, message, dungeonHp, userHp);
        }

        return new TurnResult(CombatResult.EN_COURS, message, dungeonHp, userHp);
    }

    public void handleRaidVictory(User attacker, Dungeon dungeon, HttpSession session) {
        session.setAttribute("raid_status", "VICTORY");
        User owner = dungeon.getUser();

        int ownerGold = owner.getGold();
        int transferAmount = (int) (ownerGold * 0.20);

        // Gold transfer between owner and attacker
        owner.setGold(ownerGold - transferAmount);
        attacker.setGold(attacker.getGold() + transferAmount);

        // Update stats & achievements
        attacker.setDungeonsLooted(attacker.getDungeonsLooted() + 1);
        attacker.setStolenGold(attacker.getStolenGold() + transferAmount);

        achievementService.checkAndUnlock(attacker, "DUNGEONS_LOOTED", attacker.getDungeonsLooted());
        achievementService.checkAndUnlock(attacker, "GOLD_COLLECTED", attacker.getStolenGold());

        userRepository.save(owner);
        userRepository.save(attacker);

        session.setAttribute("raid_gold_change", transferAmount);

        // Clear opponent
        clearRaidOpponent(attacker);

        // XP reward (25 XP per question)
        awardRaidXp(attacker, dungeon, 25, session);

        // Save Combat History
        Combat combat = new Combat();
        combat.setUser(attacker);
        combat.setTargetUser(owner);
        combat.setType(CombatType.RAID);
        combat.setResult(CombatResult.VICTOIRE);
        combat.setGoldEarned(transferAmount);
        combat.setXpEarned((int) session.getAttribute("raid_xp_reward"));
        combatRepository.save(combat);
    }

    public void handleRaidDefeat(User attacker, Dungeon dungeon, HttpSession session) {
        session.setAttribute("raid_status", "DEFEAT");
        User owner = dungeon.getUser();

        int attackerGold = attacker.getGold();
        int lostAmount = (int) (attackerGold * 0.50);
        int gainAmount = (int) (attackerGold * 0.10); // Owner gets 10% of original, not 10% of lost

        attacker.setGold(attackerGold - lostAmount);
        owner.setGold(owner.getGold() + gainAmount);

        // Restore HP to max
        attacker.setCurrentHp(attacker.getMaxHp());

        // Clear opponent
        clearRaidOpponent(attacker);

        userRepository.save(attacker);
        userRepository.save(owner);

        session.setAttribute("raid_gold_change", -lostAmount);

        // Save Combat History
        Combat combat = new Combat();
        combat.setUser(attacker);
        combat.setTargetUser(owner);
        combat.setType(CombatType.RAID);
        combat.setResult(CombatResult.DEFAITE);
        combat.setGoldEarned(0);
        combat.setXpEarned(0); // No XP on defeat usually? CombatService gives 10 XP on defeat. RaidService
                               // doesn't seem to award XP on defeat in the code I see.
        combatRepository.save(combat);
    }

    public void handleRaidDraw(User attacker, Dungeon dungeon, HttpSession session) {
        session.setAttribute("raid_status", "DRAW");

        // Clear opponent
        clearRaidOpponent(attacker);

        // XP reward (with 10 XP per question)
        awardRaidXp(attacker, dungeon, 10, session);

        // Save Combat History
        Combat combat = new Combat();
        combat.setUser(attacker);
        combat.setTargetUser(dungeon.getUser());
        combat.setType(CombatType.RAID);
        combat.setResult(CombatResult.NULLE);
        combat.setGoldEarned(0);
        combat.setXpEarned((int) session.getAttribute("raid_xp_reward"));
        combatRepository.save(combat);
    }

    // --- Helper Methods ---

    private void clearRaidOpponent(User attacker) {
        attacker.setCurrentOpponentDungeon(null);
        attacker.setCurrentOpponentDungeonAssignedAt(null);
        userRepository.save(attacker);
    }

    private int calculateDungeonDamage(Dungeon dungeon) {
        return (int) (25 * (1 + dungeon.getDamageBoost() / 100.0));
    }

    private void awardRaidXp(User attacker, Dungeon dungeon, int xpPerQuestion, HttpSession session) {
        int xpReward = dungeon.getDungeonQuestions().size() * xpPerQuestion;
        session.setAttribute("raid_xp_reward", xpReward);
        userService.addXp(attacker, xpReward);
    }
}
