package main.java.ct.agents;

import main.java.ct.behaviours.MoveBehaviour;
import main.java.ct.behaviours.NegotiationBehaviour;
import main.java.ct.behaviours.TransferBehaviour;
import main.java.ct.models.*;
import main.java.ct.ontology.CTOntology;
import main.java.ct.utils.PathFinder;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.behaviours.CyclicBehaviour;

import java.util.ArrayList;
import java.util.List;

public class PlayerAgent extends Agent {

    private PlayerState state;
    private Grid grid;
    private PathFinder pathFinder;
    private AID environmentAgent;
    private AID partnerAgent;

    // ─── Setup ───────────────────────────────────────────────────

    @Override
    protected void setup() {
        System.out.println("PlayerAgent " + getLocalName() + " started.");

        // Get arguments passed when agent is created
        Object[] args = getArguments();

        if (args != null && args.length >= 4) {
            String playerName     = (String) args[0];
            Cell startPosition    = (Cell)   args[1];
            Cell goalPosition     = (Cell)   args[2];

            @SuppressWarnings("unchecked")
            List<Token> tokens    = (List<Token>) args[3];

            // Initialize player state
            state = new PlayerState(playerName, startPosition,
                                    goalPosition, tokens);
        } else {
            System.err.println(getLocalName()
                             + ": Missing arguments. Using defaults.");
            // Default fallback state
            grid = new Grid();
            state = new PlayerState(
                getLocalName(),
                grid.getCell(0, 0),
                grid.getCell(0, 6),
                new ArrayList<>()
            );
        }

        // Initialize grid and pathfinder
        grid       = new Grid();
        pathFinder = new PathFinder(grid);

        // Set known agents
        environmentAgent = new AID("EnvironmentAgent", AID.ISLOCALNAME);
        partnerAgent     = new AID(
            getLocalName().equals("Player1") ? "Player2" : "Player1",
            AID.ISLOCALNAME
        );

        System.out.println("PlayerAgent initialized: " + state);

        // Start listening for messages
        addBehaviour(new ListenBehaviour());
    }

    // ─── Listen Behaviour ────────────────────────────────────────

    private class ListenBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            // Listen for any incoming message
            MessageTemplate mt = MessageTemplate.MatchOntology(
                CTOntology.ONTOLOGY_NAME
            );

            ACLMessage msg = receive(mt);

            if (msg != null) {
                handleMessage(msg);
            } else {
                block();
            }
        }
    }

    // ─── Message Handler ─────────────────────────────────────────

    private void handleMessage(ACLMessage msg) {
        String content = msg.getContent();
        String convId  = msg.getConversationId();

        // Game control messages
        if (convId.equals(CTOntology.CONV_GAME)) {
            handleGameMessage(content);
        }

        // Negotiation messages
        else if (convId.equals(CTOntology.CONV_NEGOTIATION)) {
            addBehaviour(new NegotiationBehaviour(
                PlayerAgent.this, msg, state, pathFinder, partnerAgent
            ));
        }

        // Transfer messages
        else if (convId.equals(CTOntology.CONV_TRANSFER)) {
            addBehaviour(new TransferBehaviour(
                PlayerAgent.this, msg, state
            ));
        }
    }

    // ─── Handle Game Messages ────────────────────────────────────

    private void handleGameMessage(String content) {

        if (content.startsWith(CTOntology.GAME_START)) {
            System.out.println(state.getPlayerName()
                             + ": Game started! My state: " + state);
        }

        else if (content.startsWith(CTOntology.YOUR_TURN)) {
            System.out.println(state.getPlayerName() + ": It's my turn!");
            playTurn();
        }

        else if (content.startsWith(CTOntology.GAME_OVER)) {
            System.out.println(state.getPlayerName()
                             + ": Game over! My final score: "
                             + state.getScore());
            doDelete();
        }
    }

    // ─── Play Turn ───────────────────────────────────────────────

    private void playTurn() {
        List<Cell> path = pathFinder.findShortestPath(
            state.getCurrentPosition(),
            state.getGoalPosition()
        );

        if (path.isEmpty() || path.size() == 1) {
            // Already at goal or no path
            notifyTurnDone();
            return;
        }

        // Next cell to move to
        Cell nextCell = path.get(1);

        // Check if player has the required token
        if (state.hasToken(nextCell.getColor())) {
            // Move directly
            addBehaviour(new MoveBehaviour(
                PlayerAgent.this, state, nextCell, environmentAgent
            ));
        } else {
            // Need to negotiate first
            System.out.println(state.getPlayerName()
                             + ": Missing token "
                             + nextCell.getColor()
                             + ". Starting negotiation...");
            addBehaviour(new NegotiationBehaviour(
                PlayerAgent.this, null, state, pathFinder, partnerAgent
            ));
        }
    }

    // ─── Notify Environment Turn Done ────────────────────────────

    public void notifyTurnDone() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(environmentAgent);
        msg.setOntology(CTOntology.ONTOLOGY_NAME);
        msg.setConversationId(CTOntology.CONV_GAME);
        msg.setContent(CTOntology.TURN_DONE);
        send(msg);
        System.out.println(state.getPlayerName()
                         + ": Turn done. Notified environment.");
    }

    // ─── Getters ─────────────────────────────────────────────────

    public PlayerState getPlayerState() {
        return state;
    }

    public Grid getGrid() {
        return grid;
    }

    public PathFinder getPathFinder() {
        return pathFinder;
    }

    public AID getEnvironmentAgent() {
        return environmentAgent;
    }

    public AID getPartnerAgent() {
        return partnerAgent;
    }

    // ─── Takedown ────────────────────────────────────────────────

    @Override
    protected void takeDown() {
        System.out.println("PlayerAgent " + getLocalName()
                         + " shutting down.");
    }
}