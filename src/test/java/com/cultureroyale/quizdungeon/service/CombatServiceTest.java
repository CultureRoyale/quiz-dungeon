package com.cultureroyale.quizdungeon.service;

import com.cultureroyale.quizdungeon.model.Boss;
import com.cultureroyale.quizdungeon.model.Question;
import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.model.enums.HelpLevel;
import com.cultureroyale.quizdungeon.repository.QuestionRepository;
import com.cultureroyale.quizdungeon.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class CombatServiceTest {

    @InjectMocks
    private CombatService combatService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuestionRepository questionRepository;

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
        boss.setCategories("History, Science");

        when(questionRepository.findRandomByCategoryIn(anyList())).thenReturn(Optional.of(new Question()));
    }

    @Test
    void testProcessTurn_CorrectAnswer_NoHelp() {
        when(session.getAttribute("combat_boss_current_hp")).thenReturn(100);

        CombatService.CombatResult result = combatService.processTurn(user, boss, true, HelpLevel.NO_HELP, session);

        // Damage = (100 / 20) * 2.0 = 10
        // Boss HP = 100 - 10 = 90
        assertEquals(90, result.bossHp);
        assertEquals(CombatService.CombatStatus.ONGOING, result.status);
        verify(session).setAttribute("combat_boss_current_hp", 90);
        verify(questionRepository, atLeastOnce()).findRandomByCategoryIn(anyList());
    }

    @Test
    void testProcessTurn_CorrectAnswer_FourChoices() {
        when(session.getAttribute("combat_boss_current_hp")).thenReturn(100);

        CombatService.CombatResult result = combatService.processTurn(user, boss, true, HelpLevel.FOUR_CHOICES,
                session);

        // Damage = (100 / 20) * 1.0 = 5
        // Boss HP = 100 - 5 = 95
        assertEquals(95, result.bossHp);
        verify(session).setAttribute("combat_boss_current_hp", 95);
    }

    @Test
    void testProcessTurn_WrongAnswer() {
        when(session.getAttribute("combat_boss_current_hp")).thenReturn(100);

        CombatService.CombatResult result = combatService.processTurn(user, boss, false, HelpLevel.FOUR_CHOICES,
                session);

        // User takes boss attack damage (10)
        // User HP = 100 - 10 = 90
        assertEquals(90, user.getCurrentHp());
        assertEquals(CombatService.CombatStatus.ONGOING, result.status);
    }

    @Test
    void testVictory() {
        when(session.getAttribute("combat_boss_current_hp")).thenReturn(5); // 1 hit to kill

        CombatService.CombatResult result = combatService.processTurn(user, boss, true, HelpLevel.FOUR_CHOICES,
                session);

        assertEquals(CombatService.CombatStatus.VICTORY, result.status);
        assertEquals(0, result.bossHp);
    }

    @Test
    void testDefeat() {
        user.setCurrentHp(5); // 1 hit to die
        when(session.getAttribute("combat_boss_current_hp")).thenReturn(100);

        CombatService.CombatResult result = combatService.processTurn(user, boss, false, HelpLevel.FOUR_CHOICES,
                session);

        assertEquals(CombatService.CombatStatus.DEFEAT, result.status);
        assertEquals(-5, user.getCurrentHp());
    }
}
