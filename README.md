<img height="64" alt="Logo" src="https://github.com/user-attachments/assets/272531aa-52aa-43b9-bcee-1ff1533f0986" />

# Quiz Dungeon
Quiz Dungeon est un jeu RPG de quiz où vous affrontez des boss en répondant à des questions de culture générale.
Créer votre propre donjon avec vos questions pour mettre à l'épreuve d'autres joueurs !

## Lancement du projet en local

### Requis :
- Java 17 ou +
- Maven 3.5+

### Dans le terminal :
Effectuez la commande suivante à la racine du projet.

```console
./mvnw spring-boot:run
```

## Fonctionnalités
- Un chemin embuché de boss, avec différentes difficultés et catégories de questions !
- Votre propre donjon, personnalisable avec les questions que vous avez débloqués !
- Une boutique pour acheter des potions, des boosts ou des cosmétiques de donjon !
- Un leaderboard pour vous comparer à vos rivaux !

## Régles du jeu
### Conditions de victoire et de défaite
- Une victoire vous permet de récolter de l'or et de l'expérience (gagner des niveaux augmente vos points de vie max.)
- Une défaite vous retire la moitié (50% TTC) de votre portefeuille, préparez-vous bien avant de combattre !
- Dans un donjon, le match nul vous rapportera seulement de l'exp. car le donjon rival tient toujours !

### Mécaniques du jeu
<img width="1245" height="706" alt="image" src="https://github.com/user-attachments/assets/39036a6f-a08e-4547-aa64-6665e555dfeb" />

- Une réponse correcte vous permet d'infliger des dégats à l'adversaire, inversement une réponse fausse permet à l'adversaire de vous attaquer !
- Sûr de vous ? Répondre à une question au clavier (snipe) vous permet de tripler (x3) votre attaque !
- Langue au chat ? Répondez à une question avec l'option 50/50, votre attaque seront néanmoins réduit de moitié !
