package com.cultureroyale.quizdungeon.service;

import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.repository.UserRepository;
import com.cultureroyale.quizdungeon.model.Combat;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DungeonService dungeonService;
    private final com.cultureroyale.quizdungeon.repository.CombatRepository combatRepository;

    public User registerUser(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("L'identifiant est déjà utilisé");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .level(1)
                .xp(0)
                .maxHp(100)
                .currentHp(100)
                .gold(0)
                .stolenGold(0)
                .bossKills(0)
                .dungeonsLooted(0)
                .build();

        user = userRepository.save(user);
        dungeonService.createDungeon(user);
        return user;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public void addXp(User user, int amount) {
        user.setXp(user.getXp() + amount);
        checkLevelUp(user);
        userRepository.save(user);
    }

    public void checkLevelUp(User user) {
        while (user.getXp() >= getRequiredXp(user.getLevel())) {
            user.setXp(user.getXp() - getRequiredXp(user.getLevel()));
            user.setLevel(user.getLevel() + 1);
            user.setMaxHp(user.getMaxHp() + 10);
            user.setCurrentHp(user.getMaxHp());
            user.setGold(user.getGold() + 100 * user.getLevel());
        }
    }

    public int getRequiredXp(int level) {
        return level * 100;
    }

    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(User user) {
        // Break link to current opponent dungeon (TransientObjectException fix)
        user.setCurrentOpponentDungeon(null);
        // No explicit save needed, managed by transaction

        // Break links where user is target in other combats (FK constraint fix)
        List<Combat> targetCombats = combatRepository
                .findByTargetUser(user);
        for (Combat c : targetCombats) {
            c.setTargetUser(null);
        }

        // Break links where other users are targeting this user's dungeon (FK
        // constraint fix)
        if (user.getDungeon() != null) {
            List<User> opponents = userRepository.findByCurrentOpponentDungeon(user.getDungeon());
            for (User opponent : opponents) {
                opponent.setCurrentOpponentDungeon(null);
            }
        }

        // Delete user (Cascade will handle own dungeon and own combats)
        userRepository.delete(user);
    }
}
