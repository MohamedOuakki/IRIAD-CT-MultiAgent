# IRIAD-CT-MultiAgent

Simulation multi-agents du jeu Colored Trails avec JADE.

Le projet suit l'enonce : plusieurs agents joueurs se deplacent sur une grille coloree. Pour entrer dans une case, un agent doit posseder un jeton de la meme couleur. S'il lui manque un jeton, il peut negocier avec les autres agents, proposer une offre, recevoir une acceptation ou un rejet, puis faire un transfert de jetons.

## Etat actuel du projet

Le projet contient maintenant :

- une grille dynamique selon le nombre d'agents ;
- des departs et buts aleatoires, mais avec distances relativement equitables ;
- une distribution strategique des jetons ;
- des negociations forcees mais utiles ;
- des profils strategiques d'agents ;
- des transferts de jetons entre agents ;
- une interface graphique avec animations ;
- un score conforme a l'enonce ;
- des logs detailles pour comprendre la simulation.

## Fonctionnement actuel

Au lancement, l'utilisateur choisit le nombre de joueurs entre 2 et 10.

La taille de la grille respecte l'enonce pour 2 agents puis reste variable :

```text
pour 2 agents : rows = 7, cols = 5
pour plus de 2 agents :
rows = 7 + (nombreDeJoueurs - 2)
cols = 5 + ((nombreDeJoueurs - 2) * 2)
```

Ainsi, la configuration minimale donne bien une grille 7 x 5, et la grille s'agrandit progressivement quand le nombre d'agents augmente.

Chaque joueur recoit :

- une case de depart aleatoire ;
- une case but aleatoire ;
- un ensemble initial de jetons genere strategiquement ;
- une personnalite strategique ;
- une distance depart/but relativement equitable par rapport aux autres joueurs.

Les departs et les buts ne sont pas sur la meme ligne pour un meme joueur. Les positions sont aussi evitees autant que possible pour ne pas donner des situations trop triviales.

## Distribution strategique des jetons

Les jetons ne sont plus donnes de facon purement aleatoire.

La classe `src/main/java/ct/utils/TokenDistributor.java` applique cette logique :

1. Calculer le plus court chemin de chaque agent vers son but.
2. Extraire les couleurs necessaires sur ce chemin.
3. Donner a l'agent environ 60-70% des jetons utiles a son propre chemin.
4. Retirer volontairement au moins un jeton utile pour l'empecher de finir seul.
5. Placer les jetons manquants chez les autres agents.
6. Ajouter 1 ou 2 jetons aleatoires controles pour garder de la variete.
7. Verifier a la fin que chaque agent a encore besoin d'au moins une negociation.

Les logs affichent pour chaque agent :

- le chemin choisi ;
- les couleurs necessaires ;
- les jetons initiaux ;
- les jetons utiles manquants ;
- les agents qui possedent les couleurs utiles ;
- la raison pour laquelle une negociation est necessaire.

Parametres importants dans `TokenDistributor.java` :

```java
private static final double OWN_PATH_RATIO = 0.65;
private static final int RANDOM_TOKENS_MIN = 1;
private static final int RANDOM_TOKENS_MAX = 2;
```

`OWN_PATH_RATIO` controle la part de jetons utiles donnes directement a l'agent.

- Plus la valeur est haute, plus les agents sont autonomes.
- Plus la valeur est basse, plus les negociations sont necessaires.

`RANDOM_TOKENS_MIN` et `RANDOM_TOKENS_MAX` ajoutent une petite variation sans rendre la partie totalement aleatoire.

Important : `MainContainer.java` ne genere plus les jetons directement. Il appelle :

```java
TokenDistributor distributor = new TokenDistributor(config, grid);
List<PlayerState> playerStates = distributor.createInitialPlayerStates();
```

## Profils strategiques des agents

Chaque `PlayerAgent` possede une personnalite stockee dans `PlayerState`.

L'enum se trouve dans :

`src/main/java/ct/models/AgentPersonality.java`

Profils disponibles :

- `COOPERATIVE`
- `SELFISH`
- `OPPORTUNIST`
- `CHEATER`
- `RANDOM`

La personnalite est choisie au lancement quand le `PlayerState` est cree. Elle est affichee :

- dans la console ;
- dans le journal de l'interface ;
- dans le panneau d'etat de l'interface graphique.

### Effet des profils

`COOPERATIVE`

- accepte les offres raisonnables ;
- respecte toujours les transferts.

`SELFISH`

- accepte seulement si l'offre ameliore clairement sa situation ;
- refuse les offres trop faibles ;
- respecte les transferts acceptes.

`OPPORTUNIST`

- analyse ses couleurs manquantes sur son propre chemin ;
- accepte si le jeton recu est utile pour son futur chemin ;
- respecte les transferts acceptes.

`CHEATER`

- peut accepter une offre ;
- peut ensuite ne pas respecter le transfert ;
- la trahison est affichee dans les logs.

`RANDOM`

- accepte ou refuse selon une probabilite controlee ;
- respecte ou non le transfert selon une probabilite controlee.

Parametres importants dans `TransferBehaviour.java` :

```java
private static final double CHEATER_HONOR_PROBABILITY = 0.35;
private static final double RANDOM_HONOR_PROBABILITY = 0.55;
```

Parametre important dans `NegotiationBehaviour.java` :

```java
private static final double RANDOM_ACCEPT_PROBABILITY = 0.50;
```

Pour changer le comportement des profils :

- modifier l'acceptation/rejet dans `NegotiationBehaviour.evaluateOffer` ;
- modifier le respect/trahison dans `TransferBehaviour.shouldHonorAgreement`.

## Interface graphique

Une interface Swing s'ouvre au lancement.

Elle affiche :

- la grille coloree ;
- les agents sur leurs cases ;
- les buts des agents ;
- l'agent actif pendant son tour ;
- les deplacements animes ;
- un bandeau sous la grille avec le dernier mouvement visible ;
- les communications entre agents avec une ligne/fleche temporaire ;
- un historique complet des evenements : tours, mouvements, propositions, acceptations, rejets, transferts, blocages, scores ;
- des listes deroulantes pour filtrer l'historique par type d'evenement et par joueur.

Les filtres disponibles dans l'interface sont :

- `Tous`
- `Mouvements`
- `Negociations`
- `Transferts`
- `Tours`
- `Scores`
- `Autres`

Le filtre par joueur permet de suivre uniquement ce qui concerne un agent precis pendant la simulation.

## Regles implementees

Chaque tour :

1. L'environnement donne la main a un joueur.
2. Le joueur cherche le plus court chemin vers son but.
3. Il regarde la prochaine case du chemin.
4. S'il possede le jeton de la couleur de cette case, il consomme le jeton et avance.
5. Sinon, il negocie avec les autres joueurs.
6. Si une offre est acceptee, les agents transferent les jetons convenus.
7. Apres le transfert, le joueur retente son mouvement.
8. Si aucune negociation ne fonctionne, le joueur est bloque pour ce tour.

Les decisions d'acceptation/rejet dependent du profil strategique de l'agent qui recoit l'offre.

La partie se termine quand :

- un joueur atteint son but ;
- ou un joueur est bloque pendant 3 tours.

## Flux technique simplifie

```text
MainContainer
  -> cree GameConfig
  -> cree Grid
  -> appelle TokenDistributor
  -> cree les PlayerState
  -> lance EnvironmentAgent
  -> lance les PlayerAgent

EnvironmentAgent
  -> gere les tours
  -> envoie YOUR_TURN
  -> attend TURN_DONE ou BLOCKED
  -> termine la partie
  -> calcule les scores

PlayerAgent
  -> recoit YOUR_TURN
  -> calcule le plus court chemin
  -> avance si possible
  -> sinon lance NegotiationBehaviour

NegotiationBehaviour
  -> propose un echange
  -> recoit acceptation ou rejet
  -> lance TransferBehaviour si accepte

TransferBehaviour
  -> transfere les jetons convenus
  -> met a jour les inventaires
  -> relance le mouvement

GameUI / SimulationUI
  -> affichent grille, agents, deplacements et communications
```

## Score

Le score respecte la formule de l'enonce.

Si l'agent atteint son but :

```text
score = 100 + (nombre de jetons restants * 5)
```

Sinon :

```text
score = (nombre de jetons restants * 5) - (nombre de cases restantes * 10)
```

Le detail du calcul est affiche dans la console et dans le journal de l'interface en fin de partie.

## Execution

Ouvrir PowerShell dans le dossier du projet :

```powershell
cd "C:\Users\mtdja\OneDrive\Desktop\ESI\2CS\S2\IRIAD\Projet 3\IRIAD-CT-MultiAgent"
```

Compiler :

```powershell
$sources = Get-ChildItem -Recurse src\main\java -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -cp "lib\jade.jar" -d bin $sources
```

Executer :

```powershell
java -cp "bin;lib\jade.jar" main.java.ct.MainContainer
```

Puis entrer le nombre de joueurs, par exemple :

```text
4
```

## Organisation des fichiers

### Demarrage

`src/main/java/ct/MainContainer.java`

- lit le nombre de joueurs ;
- cree la configuration ;
- cree la grille ;
- appelle `TokenDistributor` ;
- cree les etats initiaux des joueurs avec des jetons strategiques ;
- lance le conteneur JADE ;
- lance `EnvironmentAgent` et les `PlayerAgent`.

Modifier ici si tu veux changer le lancement, les arguments passes aux agents, ou l'affichage console initial.

### Agents

`src/main/java/ct/agents/EnvironmentAgent.java`

- controle les tours ;
- envoie `YOUR_TURN` ;
- attend `TURN_DONE` ou `BLOCKED` ;
- detecte la fin de partie ;
- applique les scores ;
- informe l'interface.

Modifier ici si tu veux changer l'ordre des tours, les conditions d'arret, ou la gestion globale de la partie.

`src/main/java/ct/agents/PlayerAgent.java`

- contient l'etat local d'un joueur ;
- possede une personnalite via `PlayerState` ;
- recoit les messages ;
- choisit quoi faire pendant son tour ;
- lance le deplacement ou la negociation ;
- notifie l'environnement en fin de tour.

Modifier ici si tu veux changer la strategie generale d'un joueur.

### Comportements

`src/main/java/ct/behaviours/MoveBehaviour.java`

- verifie si le joueur possede le bon jeton ;
- consomme le jeton ;
- deplace le joueur ;
- signale la fin du tour ou le but atteint.

Modifier ici pour changer les regles de mouvement.

`src/main/java/ct/behaviours/NegotiationBehaviour.java`

- construit les offres ;
- demande le prochain jeton utile ;
- contacte les partenaires ;
- accepte ou rejette les propositions selon la personnalite ;
- affiche la raison de chaque acceptation ou rejet ;
- lance le transfert si l'offre est acceptee.

Modifier ici pour changer la strategie de negociation : generosite, priorites, choix du partenaire, acceptation/rejet.

`src/main/java/ct/behaviours/TransferBehaviour.java`

- transfere les jetons convenus ;
- respecte ou trahit l'accord selon la personnalite ;
- affiche si le transfert est respecte ou non ;
- met a jour les inventaires ;
- relance le mouvement apres un transfert.

Modifier ici pour simuler des agents honnetes, egoistes, tricheurs ou probabilistes.

### Modele du jeu

`src/main/java/ct/models/GameConfig.java`

- definit le nombre de joueurs ;
- calcule la taille de la grille : 7 x 5 pour 2 agents, puis agrandissement progressif ;
- garde quelques parametres generaux du jeu.

Modifier ici pour changer la taille de la grille ou les bornes du nombre de joueurs.

`src/main/java/ct/models/Grid.java`

- cree les couleurs des cases ;
- choisit les departs et buts aleatoires ;
- impose que depart et but ne soient pas sur la meme ligne ;
- equilibre la distance depart/but entre joueurs.

Modifier ici pour changer la generation de la grille, l'equite des distances, ou les contraintes de position.

`src/main/java/ct/models/PlayerState.java`

- stocke la position du joueur ;
- stocke son but ;
- stocke ses jetons ;
- stocke sa personnalite ;
- stocke son score ;
- stocke son nombre de tours bloques.

Modifier ici si tu veux ajouter des attributs aux agents.

`src/main/java/ct/models/Cell.java`

- represente une case : ligne, colonne, couleur.

`src/main/java/ct/models/Token.java`

- represente un jeton de couleur.

`src/main/java/ct/models/AgentPersonality.java`

- enumere les profils strategiques disponibles ;
- permet aux comportements de negociation et transfert de varier selon l'agent.

`src/main/java/ct/models/Offer.java`

- represente une offre de negociation.

### Utilitaires

`src/main/java/ct/utils/PathFinder.java`

- calcule le plus court chemin ;
- calcule les jetons necessaires pour suivre un chemin ;
- calcule les jetons manquants.

Modifier ici pour changer la logique de chemin, par exemple ajouter des obstacles ou un autre algorithme.

`src/main/java/ct/utils/TokenDistributor.java`

- calcule les chemins initiaux ;
- analyse les couleurs necessaires ;
- donne 60-70% des jetons utiles a chaque agent ;
- force au moins un jeton utile manquant pour chaque agent ;
- place les jetons manquants chez les autres agents ;
- garantit qu'une negociation est necessaire ;
- garantit qu'au moins une negociation utile est possible ;
- ajoute une petite part aleatoire controlee ;
- affiche les logs de distribution.

Modifier ici pour changer l'equilibre entre autonomie et dependance aux negociations.

Exemples de modifications possibles dans ce fichier :

- augmenter `OWN_PATH_RATIO` pour reduire les negociations ;
- diminuer `OWN_PATH_RATIO` pour augmenter les negociations ;
- augmenter `RANDOM_TOKENS_MAX` pour plus de variete ;
- modifier `assignOwnUsefulTokens` pour changer la logique des jetons donnes au depart ;
- modifier `distributeMissingTokensToOtherAgents` pour favoriser certains partenaires.

`src/main/java/ct/utils/ScoreCalculator.java`

- applique la formule du score ;
- calcule les cases restantes ;
- affiche le detail du score.

Modifier ici si l'enseignant demande une autre formule.

### Ontologie JADE

`src/main/java/ct/ontology/CTOntology.java`

- centralise les noms des messages ;
- centralise les conversation IDs ;
- centralise les cles utilisees dans le contenu des messages.

Modifier ici si tu ajoutes de nouveaux types de messages.

### Interface

`src/main/java/ct/gui/GameUI.java`

- dessine la grille ;
- dessine les agents ;
- dessine les buts ;
- anime les deplacements ;
- affiche le dernier mouvement sous la grille ;
- affiche les communications ;
- affiche les etats des joueurs ;
- conserve l'historique complet des evenements ;
- permet de filtrer les logs par type d'evenement et par joueur avec des listes deroulantes.

Modifier ici pour changer l'apparence de l'interface, les categories de logs, les filtres, ou la facon dont les mouvements et negociations sont affiches.

`src/main/java/ct/gui/SimulationUI.java`

- sert de pont entre les agents JADE et l'interface Swing ;
- expose des fonctions simples comme `log`, `updatePlayer`, `logMessage`, `pause`.

Modifier ici pour ajouter de nouveaux evenements visuels.

## Idees de modifications possibles

- Ajouter plusieurs strategies d'agents : cooperatif, egoiste, aleatoire, tricheur.
- Ajouter une selection manuelle des profils au lancement.
- Ajouter une probabilite de ne pas respecter un transfert.
- Afficher les chemins prevus par les agents.
- Ajouter une legende des couleurs et des jetons.
- Ajouter un bouton pause/reprendre dans l'interface.
- Ajouter des statistiques finales : nombre de negociations, acceptations, rejets, transferts.
- Ajouter des obstacles dans la grille.
- Permettre de choisir manuellement le nombre de jetons au lancement.

## Notes

JADE cree parfois des fichiers comme `APDescription.txt` ou `MTPs-Main-Container.txt`. Ils sont ignores par Git.

Le dossier `bin/` contient les fichiers compiles et peut etre regenere avec la commande `javac`.
