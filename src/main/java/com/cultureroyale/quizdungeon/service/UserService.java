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

        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
    }
}
