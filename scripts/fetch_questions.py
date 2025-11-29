#!/usr/bin/env python3
"""
Script pour récupérer les questions depuis l'API QuizzAPI
et générer un fichier SQL pour import dans H2.

Usage:
    python scripts/fetch_questions.py

Output:
    scripts/import_questions.sql
"""

import requests
import json

# URL de l'API QuizzAPI
API_URL = 'https://quizzapi.jomoreschi.fr/api/v2/quiz?limit=1000'

# Mapping des difficultés
DIFFICULTY_MAP = {
    'facile': 'FACILE',
    'moyen': 'MOYEN',
    'difficile': 'DIFFICILE'
}

def escape_sql(text):
    """Échappe les guillemets simples pour SQL"""
    if text is None:
        return ''
    return text.replace("'", "''")

def fetch_questions():
    """Récupère toutes les questions depuis l'API"""
    print(f"📡 Récupération des questions depuis {API_URL}...")
    response = requests.get(API_URL)
    response.raise_for_status()
    data = response.json()

    quizzes = data.get('quizzes', [])
    print(f"✅ {len(quizzes)} questions récupérées")

    return quizzes

def generate_sql(quizzes, output_file):
    """Génère le fichier SQL d'import"""
    print(f"📝 Génération du fichier SQL...")

    with open(output_file, 'w', encoding='utf-8') as f:
        # Header
        f.write("-- Import des questions depuis QuizzAPI\n")
        f.write(f"-- Total: {len(quizzes)} questions\n")
        f.write("-- Généré automatiquement par fetch_questions.py\n\n")

        skipped = 0

        for quiz in quizzes:
            # Vérifier que tous les champs requis existent
            if not all(key in quiz for key in ['id', 'question', 'answer', 'badAnswers', 'category', 'difficulty']):
                skipped += 1
                continue

            # Vérifier qu'il y a bien 3 mauvaises réponses
            if len(quiz.get('badAnswers', [])) != 3:
                skipped += 1
                continue

            external_id = escape_sql(quiz['id'])
            question = escape_sql(quiz['question'])
            answer = escape_sql(quiz['answer'])
            bad1 = escape_sql(quiz['badAnswers'][0])
            bad2 = escape_sql(quiz['badAnswers'][1])
            bad3 = escape_sql(quiz['badAnswers'][2])
            category = quiz['category']
            difficulty = DIFFICULTY_MAP.get(quiz.get('difficulty'), 'FACILE')

            sql = (
                f"INSERT INTO questions (external_id, question_text, correct_answer, "
                f"bad_answer1, bad_answer2, bad_answer3, category, difficulty) "
                f"VALUES ('{external_id}', '{question}', '{answer}', '{bad1}', '{bad2}', '{bad3}', "
                f"'{category}', '{difficulty}');\n"
            )

            f.write(sql)

        if skipped > 0:
            f.write(f"\n-- {skipped} questions ignorées (champs manquants)\n")

    print(f"✅ Fichier généré: {output_file}")
    print(f"📊 {len(quizzes) - skipped} questions valides / {len(quizzes)} total")
    if skipped > 0:
        print(f"⚠️  {skipped} questions ignorées")

def main():
    """Point d'entrée principal"""
    output_file = 'import_questions.sql'

    try:
        # Récupérer les questions
        quizzes = fetch_questions()

        # Générer le SQL
        generate_sql(quizzes, output_file)

        print("\n✨ Terminé!")
        print(f"\n📌 Prochaines étapes:")
        print(f"   1. Démarrer l'application Spring Boot")
        print(f"   2. Ouvrir http://localhost:8080/h2-console")
        print(f"   3. Se connecter avec: jdbc:h2:file:./data/quizzroyaldb")
        print(f"   4. Copier-coller le contenu de {output_file}")
        print(f"   5. Exécuter")

    except requests.RequestException as e:
        print(f"❌ Erreur lors de la récupération: {e}")
        return 1
    except Exception as e:
        print(f"❌ Erreur: {e}")
        return 1

    return 0

if __name__ == '__main__':
    exit(main())
