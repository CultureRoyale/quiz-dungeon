package com.cultureroyale.quizdungeon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests((requests) -> requests
                // 1. On autorise l'accès public à l'accueil ("/") et aux ressources statiques
                .requestMatchers("/", "/css/**", "/images/**", "/js/**").permitAll()
                // 2. Tout le reste nécessite d'être connecté
                .anyRequest().authenticated()
            )
            // 3. On active le formulaire de login par défaut pour les pages protégées
            .formLogin((form) -> form
                .permitAll()
            );

        return http.build();
    }
}