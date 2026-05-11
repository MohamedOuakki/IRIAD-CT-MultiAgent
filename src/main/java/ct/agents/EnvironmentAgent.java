package main.java.ct.agents;

import main.java.ct.models.*;
import main.java.ct.ontology.CTOntology;
import main.java.ct.utils.PathFinder;
import main.java.ct.utils.ScoreCalculator;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.behaviours.CyclicBehaviour;

import java.util.*;

public class EnvironmentAgent extends Agent {

    private Grid grid;
    private PathFinder pathFinder;
    private ScoreCalculator scoreCalculator;

    private List<AID> players;
    private Map<AID, PlayerState> playerStates;

    private int currentPlayerIndex;
    private boolean gameOver;

    // ─── Setup ───────────────────────────────────────────────────

    @Override
    protected void setup() {
        System.out.println("EnvironmentAgent " + getLocalName() + " started.");

        // Initialize grid
        grid = new Grid();
        pathFinder = new PathFinder(grid);
        scoreCalculator = new ScoreCalculator(pathFinder);

        // Initialize players list
        players = new ArrayList<>();
        playerStates = new HashMap<>();
        gameOver = false;
        currentPlayerIndex = 0;

        // Wait for player agents to register
        doWait(2000);

        // Register players (hardcoded AIDs for now)
        AID player1 = new AID("Player1", AID.ISLOCALNAME);
        AID player2 = new AID("Player2", AID.ISLOCALNAME);
        players.add(player1);
        players.add(player2);

        // Initialize player states
        initializePlayerStates();

        // Start the game
        broadcastMessage(CTOntology.GAME_START, CTOntology.CONV_GAME,
                         "Game started! Grid is ready.");

        // Add behaviour to manage game turns
        addBehaviour(new GameManagerBehaviour());
    }

    // ─── Initialize Player States ────────────────────────────────

    private void initializePlayerStates() {
        // Player 1: starts top-left, goal top-right
        Cell start1 = grid.getCell(0, 0);
        Cell goal1  = grid.getCell(0, 6);
        List<Token> tokens1 = generateRandomTokens(5);
        playerStates.put(players.get(0),
            new PlayerState("Player1", start1, goal1, tokens1));

        // Player 2: starts bottom-right, goal bottom-left
        Cell start2 = grid.getCell(4, 6);
        Cell goal2  = grid.getCell(4, 0);
        List<Token> tokens2 = generateRandomTokens(5);
        playerStates.put(players.get(1),
            new PlayerState("Player2", start2, goal2, tokens2));

        System.out.println("Player states initialized.");
        System.out.println(playerStates.get(players.get(0)));
        System.out.println(playerStates.get(players.get(1)));
    }

    // ─── Generate Random Tokens ──────────────────────────────────

    private List<Token> generateRandomTokens(int count) {
        List<Token> tokens = new ArrayList<>();
        Token.Color[] colors = Token.Color.values();
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            tokens.add(new Token(colors[random.nextInt(colors.length)]));
        }
        return tokens;
    }

    // ─── Game Manager Behaviour ──────────────────────────────────

    private class GameManagerBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            if (gameOver) return;

            // Notify current player it's their turn
            AID currentPlayer = players.get(currentPlayerIndex);
            sendMessage(currentPlayer, CTOntology.YOUR_TURN,
                        CTOntology.CONV_GAME, "Your turn.");

            // Wait for player to finish their turn
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchConversationId(CTOntology.CONV_GAME),
                MessageTemplate.MatchContent(CTOntology.TURN_DONE)
            );

            ACLMessage reply = blockingReceive(mt, 10000);

            if (reply != null) {
                handleTurnDone(reply);
            } else {
                // Player didn't respond in time, treat as blocked
                PlayerState state = playerStates.get(currentPlayer);
                state.incrementBlockedTurns();
                System.out.println(state.getPlayerName()
                                 + " did not respond. Blocked turns: "
                                 + state.getBlockedTurns());
                checkBlockedCondition(currentPlayer, state);
            }

            // Switch to next player
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        }
    }

    // ─── Handle Turn Done ────────────────────────────────────────

    private void handleTurnDone(ACLMessage message) {
        AID sender = message.getSender();
        PlayerState state = playerStates.get(sender);

        if (state == null) return;

        // Check if player reached goal
        if (state.hasReachedGoal()) {
            System.out.println(state.getPlayerName() + " reached the goal!");
            endGame(sender);
            return;
        }

        // Check if player is blocked
        checkBlockedCondition(sender, state);
    }

    // ─── Check Blocked Condition ─────────────────────────────────

    private void checkBlockedCondition(AID player, PlayerState state) {
        if (state.isBlocked()) {
            System.out.println(state.getPlayerName()
                             + " is blocked for 3 turns. Game over.");
            endGame(player);
        }
    }

    // ─── End Game ────────────────────────────────────────────────

    private void endGame(AID triggeringPlayer) {
        gameOver = true;

        // Calculate and apply scores
        for (AID player : players) {
            PlayerState state = playerStates.get(player);
            scoreCalculator.applyFinalScore(state);
            scoreCalculator.printScoreBreakdown(state);
        }

        // Notify all players
        broadcastMessage(CTOntology.GAME_OVER, CTOntology.CONV_GAME,
                         "Game over!");

        // Shutdown
        doDelete();
    }

    // ─── Messaging Helpers ───────────────────────────────────────

    private void sendMessage(AID receiver, String content,
                             String convId, String userContent) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(receiver);
        msg.setOntology(CTOntology.ONTOLOGY_NAME);
        msg.setConversationId(convId);
        msg.setContent(content + ";" + userContent);
        send(msg);
    }

    private void broadcastMessage(String content, String convId,
                                  String userContent) {
        for (AID player : players) {
            sendMessage(player, content, convId, userContent);
        }
    }

    // ─── Takedown ────────────────────────────────────────────────

    @Override
    protected void takeDown() {
        System.out.println("EnvironmentAgent " + getLocalName()
                         + " shutting down.");
    }
}