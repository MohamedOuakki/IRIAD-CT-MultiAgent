package main.java.ct.agents;

import main.java.ct.models.*;
import main.java.ct.ontology.CTOntology;
import main.java.ct.utils.PathFinder;
import main.java.ct.utils.ScoreCalculator;
import main.java.ct.gui.SimulationUI;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.behaviours.CyclicBehaviour;

import java.util.*;

public class EnvironmentAgent extends Agent {

    private GameConfig config;
    private Grid grid;
    private PathFinder pathFinder;
    private ScoreCalculator scoreCalculator;

    private List<AID> players;
    private Map<AID, PlayerState> playerStates;

    private int currentPlayerIndex;
    private boolean gameOver;

    // Setup
    @Override
    protected void setup() {
        System.out.println("EnvironmentAgent " + getLocalName() + " started.");

        // Get shared game data from arguments
        Object[] args = getArguments();
        if (args != null && args.length >= 3) {
            config = (GameConfig) args[0];
            grid   = (Grid) args[1];
        } else {
            System.out.println("No config provided. Using default (2 players).");
            config = new GameConfig(2);
            grid   = new Grid(config);
        }

        System.out.println("Game configuration: " + config);

        pathFinder    = new PathFinder(grid);
        scoreCalculator = new ScoreCalculator(pathFinder);

        System.out.println(grid);

        // Initialize players list
        players            = new ArrayList<>();
        playerStates       = new LinkedHashMap<>();
        gameOver           = false;
        currentPlayerIndex = 0;

        // Wait for player agents to initialize
        doWait(2000);

        // Register N players dynamically
        for (int i = 0; i < config.getNumberOfPlayers(); i++) {
            AID player = new AID("Player" + (i + 1), AID.ISLOCALNAME);
            players.add(player);
        }

        // Initialize player states dynamically
        if (args != null && args.length >= 3) {
            initializePlayerStatesFromArgs(args[2]);
        } else {
            initializePlayerStates();
        }

        SimulationUI.show(grid, playerStates.values());
        SimulationUI.log("Simulation demarree avec " + players.size()
                       + " agents sur une grille " + config.getRows()
                       + "x" + config.getCols());

        // Broadcast game start
        broadcastMessage(CTOntology.GAME_START, CTOntology.CONV_GAME,
                         "Game started! Grid is " + config.getRows()
                         + "x" + config.getCols());

        // Start game manager
        addBehaviour(new GameManagerBehaviour());
    }

    // Initialize Player States
    private void initializePlayerStates() {
        for (int i = 0; i < config.getNumberOfPlayers(); i++) {
            AID playerAID = players.get(i);

            // Get dynamic start and goal from grid
            Cell startCell = grid.getStartCell(i);
            Cell goalCell  = grid.getGoalCell(i);

            // Generate random tokens
            List<Token> tokens = generateRandomTokens(
                config.getTokensPerPlayer()
            );

            PlayerState state = new PlayerState(
                "Player" + (i + 1),
                startCell,
                goalCell,
                tokens
            );

            playerStates.put(playerAID, state);

            System.out.println("Initialized: " + state);
        }
    }

    @SuppressWarnings("unchecked")
    private void initializePlayerStatesFromArgs(Object stateArg) {
        List<PlayerState> states = (List<PlayerState>) stateArg;
        for (int i = 0; i < states.size(); i++) {
            PlayerState state = states.get(i);
            AID playerAID = players.get(i);
            playerStates.put(playerAID, state);
            System.out.println("Initialized: " + state);
        }
    }

    // Generate Random Tokens
    private List<Token> generateRandomTokens(int count) {
        List<Token> tokens   = new ArrayList<>();
        Token.Color[] colors = Token.Color.values();
        Random random        = new Random();

        for (int i = 0; i < count; i++) {
            tokens.add(new Token(colors[random.nextInt(colors.length)]));
        }
        return tokens;
    }

    // Game Manager Behaviour
    private class GameManagerBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            if (gameOver) return;

            // Get current player
            AID currentPlayer = players.get(currentPlayerIndex);
            PlayerState state = playerStates.get(currentPlayer);

            System.out.println("=== Turn of " + state.getPlayerName()
                             + " ===");
            SimulationUI.log("Tour de " + state.getPlayerName());
            SimulationUI.setActivePlayer(state.getPlayerName());
            SimulationUI.pause(500);

            // Notify current player it's their turn
            sendMessage(currentPlayer, CTOntology.YOUR_TURN,
                        CTOntology.CONV_GAME, "Your turn.");
            SimulationUI.pause(450);

            // Wait for player to finish their turn
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchConversationId(CTOntology.CONV_GAME),
                MessageTemplate.MatchOntology(CTOntology.ONTOLOGY_NAME)
            );

            ACLMessage reply = SimulationUI.blockingReceive(
                EnvironmentAgent.this, mt, 10000
            );

            if (reply != null) {
                handleTurnDone(reply);
            } else {
                // Player didn't respond in time
                state.incrementBlockedTurns();
                System.out.println(state.getPlayerName()
                                 + " did not respond. Blocked turns: "
                                 + state.getBlockedTurns());
                SimulationUI.updatePlayer(state);
                SimulationUI.log(state.getPlayerName()
                               + " ne repond pas. Blocage "
                               + state.getBlockedTurns() + "/3");
                checkBlockedCondition(currentPlayer, state);
            }

            // Move to next player if game still running
            if (!gameOver) {
                currentPlayerIndex =
                    (currentPlayerIndex + 1) % players.size();
            }
        }
    }

    // Handle Turn Done
    private void handleTurnDone(ACLMessage message) {
        AID sender     = message.getSender();
        String content = message.getContent();
        PlayerState state = playerStates.get(sender);

        if (state == null) return;

        // Ignore unrelated messages
        if (!content.startsWith(CTOntology.TURN_DONE)
         && !content.startsWith(CTOntology.BLOCKED)) {
            return;
        }

        // Check if player reached goal
        if (content.contains("goalReached=true") || state.hasReachedGoal()) {
            System.out.println(state.getPlayerName() + " reached the goal!");
            SimulationUI.updatePlayer(state);
            SimulationUI.log(state.getPlayerName() + " a atteint le but");
            endGame(sender);
            return;
        }

        // Check if player is blocked
        if (content.startsWith(CTOntology.BLOCKED)) {
            checkBlockedCondition(sender, state);
            return;
        }

        // Successful turn
        state.resetBlockedTurns();
        SimulationUI.updatePlayer(state);
        System.out.println(state.getPlayerName()
                         + ": completed turn successfully.");
    }

    // Check Blocked Condition
    private void checkBlockedCondition(AID player, PlayerState state) {
        if (state.isBlocked()) {
            System.out.println(state.getPlayerName()
                             + " is blocked for 3 turns. Game over.");
            SimulationUI.log(state.getPlayerName()
                           + " est bloque pendant 3 tours");
            endGame(player);
        }
    }

    // End Game
    private void endGame(AID triggeringPlayer) {
        gameOver = true;

        System.out.println("\n========== GAME OVER ==========");

        // Calculate and display scores for all players
        for (AID playerAID : players) {
            PlayerState state = playerStates.get(playerAID);
            scoreCalculator.applyFinalScore(state);
            SimulationUI.updatePlayer(state);
            scoreCalculator.printScoreBreakdown(state);
            SimulationUI.log(scoreCalculator.buildScoreBreakdown(state));
        }

        // Find winner (highest score)
        announceWinner();
        SimulationUI.log(SimulationUI.getStatsSummary());

        // Notify all players
        broadcastMessage(CTOntology.GAME_OVER, CTOntology.CONV_GAME,
                         "Game over!");
        SimulationUI.log("Fin du jeu");
        doDelete();
    }

    // Announce Winner
    private void announceWinner() {
        AID winner         = null;
        int highestScore   = Integer.MIN_VALUE;

        for (AID playerAID : players) {
            PlayerState state = playerStates.get(playerAID);
            if (state.getScore() > highestScore) {
                highestScore = state.getScore();
                winner       = playerAID;
            }
        }

        if (winner != null) {
            System.out.println("🏆 Winner: "
                             + playerStates.get(winner).getPlayerName()
                             + " with score: " + highestScore);
            SimulationUI.log("Gagnant: "
                           + playerStates.get(winner).getPlayerName()
                           + " avec " + highestScore + " points");
        }
    }

    // Messaging Helpers
    private void sendMessage(AID receiver, String content,
                             String convId, String userContent) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(receiver);
        msg.setOntology(CTOntology.ONTOLOGY_NAME);
        msg.setConversationId(convId);
        msg.setContent(content + ";" + userContent);
        send(msg);
        SimulationUI.logMessage(getLocalName(), receiver.getLocalName(),
                                convId, msg.getContent());
    }

    private void broadcastMessage(String content, String convId,
                                  String userContent) {
        for (AID player : players) {
            sendMessage(player, content, convId, userContent);
        }
    }

    // Takedown
    @Override
    protected void takeDown() {
        System.out.println("EnvironmentAgent " + getLocalName()
                         + " shutting down.");
    }
}
