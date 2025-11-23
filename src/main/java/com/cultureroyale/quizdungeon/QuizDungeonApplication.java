package com.cultureroyale.quizdungeon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QuizDungeonApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuizDungeonApplication.class, args);

		System.out.println("Quiz Dungeon fonctionne correctement.");
	}

}
