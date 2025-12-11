package com.cultureroyale.quizdungeon.service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.cultureroyale.quizdungeon.model.Boss;
import com.cultureroyale.quizdungeon.model.Question;
import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.model.dto.TurnResult;
import com.cultureroyale.quizdungeon.model.enums.Category;
import com.cultureroyale.quizdungeon.model.enums.CombatResult;
import com.cultureroyale.quizdungeon.model.enums.Difficulty;
import com.cultureroyale.quizdungeon.model.enums.HelpLevel;
import com.cultureroyale.quizdungeon.repository.QuestionRepository;
import com.cultureroyale.quizdungeon.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

class CombatServiceTest {

    @InjectMocks
    private CombatService combatService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionService questionService;

    @Mock
    private HttpSession session;

    private User user;
    private Boss boss;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User();
        user.setCurrentHp(100);
        user.setMaxHp(100);
        user.setGold(100);

        boss = new Boss();
        boss.setMaxHp(100);
        boss.setAttack(10);
        boss.setGoldReward(50);
        Set<Category> categories = new HashSet<>();
        categories.add(Category.HISTOIRE);
        categories.add(Category.SCIENCE);
        boss.setCategories(categories);
        boss.setDifficulty(Difficulty.FACILE);

        when(questionService.getQuestionForBoss(any(), anyList())).thenReturn(Optional.of(new Question()));
    }

    @Test
    void testProcessTurn_CorrectAnswer_NoHelp() {
        when(session.getAttribute("combat_boss_current_hp")).thenReturn(100);

        TurnResult result = combatService.processTurn(user, boss, true, HelpLevel.NO_HELP, session);

        // Damage = (100 / 20) * 2.0 (FACILE) * 3.0 (NO_HELP) = 30
        // Boss HP = 100 - 30 = 70
        assertEquals(70, result.bossHp);
        assertEquals(CombatResult.EN_COURS, result.status);
        verify(session).setAttribute("combat_boss_current_hp", 70);
        verify(questionService, atLeastOnce()).getQuestionForBoss(any(), anyList());
    }

    @Test
    void testProcessTurn_CorrectAnswer_FourChoices() {
        when(session.getAttribute("combat_boss_current_hp")).thenReturn(100);

        TurnResult result = combatService.processTurn(user, boss, true, HelpLevel.FOUR_CHOICES,
                session);

        // Damage = (100 / 20) * 2.0 (FACILE) * 1.0 (FOUR_CHOICES) = 10
        // Boss HP = 100 - 10 = 90
        assertEquals(90, result.bossHp);
        verify(session).setAttribute("combat_boss_current_hp", 90);
    }

    @Test
    void testProcessTurn_WrongAnswer() {
        when(session.getAttribute("combat_boss_current_hp")).thenReturn(100);

        TurnResult result = combatService.processTurn(user, boss, false, HelpLevel.FOUR_CHOICES, session);

        // User takes boss attack damage (10)
        // User HP = 100 - 10 = 90
        assertEquals(90, user.getCurrentHp());
        assertEquals(CombatResult.EN_COURS, result.status);
    }

    @Test
    void testVictory() {
        when(session.getAttribute("combat_boss_current_hp")).thenReturn(5); // 1 hit to kill

        TurnResult result = combatService.processTurn(user, boss, true, HelpLevel.FOUR_CHOICES, session);

        assertEquals(CombatResult.VICTOIRE, result.status);
        assertEquals(-5, result.bossHp);
    }

    @Test
    void testDefeat() {
        user.setCurrentHp(5); // 1 hit to die
        when(session.getAttribute("combat_boss_current_hp")).thenReturn(100);

        TurnResult result = combatService.processTurn(user, boss, false, HelpLevel.FOUR_CHOICES, session);

        assertEquals(CombatResult.DEFAITE, result.status);
        assertEquals(-5, user.getCurrentHp());
    }
}
