package com.cultureroyale.quizdungeon.service;

import com.cultureroyale.quizdungeon.model.User;
import com.cultureroyale.quizdungeon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DungeonService dungeonService;

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
}
