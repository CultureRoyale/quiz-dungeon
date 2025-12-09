package com.cultureroyale.quizdungeon.controller;

import org.springframework.stereotype.Controller;
import java.text.Normalizer;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Controller
public class QuestionController {

    public boolean verifyAnswer(String userAnswer, Long choiceId, String correctAnswerText) {
        if (userAnswer != null && !userAnswer.trim().isEmpty()) {
            if (correctAnswerText == null)
                return false;
            return checkTextAnswer(userAnswer, correctAnswerText);
        } else {
            return choiceId != null && choiceId == 1L;
        }
    }

    private boolean checkTextAnswer(String answer, String correctAnswerRaw) {
        String correctAnswer = normalize(correctAnswerRaw.trim());
        String userAnswer = normalize(answer.trim());

        // direct comparison (case insensitive and accent insensitive)
        if (correctAnswer.equalsIgnoreCase(userAnswer)) {
            return true;
        }

        // forgiveness: Levenshtein distance check if length > 3
        if (userAnswer.length() > 3) {
            int distance = calculateLevenshteinDistance(userAnswer, correctAnswer);
            if (distance <= 2) {
                return true;
            }
        }

        // VERY VERY forgiving
        String[] userWords = userAnswer.split("\\s+");
        String[] correctWords = correctAnswer.split("\\s+");

        Set<String> determinants = new HashSet<>(Arrays.asList(
                "le", "la", "les", "l", "un", "une", "des", "du", "de", "d", "au", "aux",
                "ce", "cet", "cette", "ces", "mon", "ton", "son", "ma", "ta", "sa",
                "mes", "tes", "ses", "notre", "votre", "leur", "nos", "vos", "leurs"));

        for (String uWord : userWords) {
            if (determinants.contains(uWord))
                continue;

            for (String cWord : correctWords) {
                if (determinants.contains(cWord))
                    continue;

                if (uWord.equalsIgnoreCase(cWord)) {
                    return true;
                }
                // SUPER forgiving
                if (uWord.length() > 3) {
                    int dist = calculateLevenshteinDistance(uWord, cWord);
                    if (dist <= 3) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // Levenshtein distance
    // https://fr.wikipedia.org/wiki/Distance_de_Levenshtein
    private int calculateLevenshteinDistance(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = min(dp[i - 1][j - 1] + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)),
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1);
                }
            }
        }

        return dp[x.length()][y.length()];
    }

    private int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 1;
    }

    private int min(int... numbers) {
        return Arrays.stream(numbers).min().orElse(Integer.MAX_VALUE);
    }

    private String normalize(String input) {
        if (input == null)
            return null;
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("").toLowerCase();
    }
}
