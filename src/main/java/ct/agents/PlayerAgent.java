package main.java.ct.agents;

import main.java.ct.behaviours.MoveBehaviour;
import main.java.ct.behaviours.NegotiationBehaviour;
import main.java.ct.behaviours.TransferBehaviour;
import main.java.ct.gui.SimulationUI;
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
    private GameConfig config;
    private AID environmentAgent;
    private List<AID> partnerAgents;

    // Setup
    @Override
    protected void setup() {
        System.out.println("PlayerAgent " + getLocalName() + " started.");

        // Get arguments passed when agent is created
        Object[] args = getArguments();

        if (args != null && args.length >= 3 && args[0] instanceof PlayerState) {
            state  = (PlayerState) args[0];
            grid   = (Grid) args[1];
            config = (GameConfig) args[2];
        } else if (args != null && args.length >= 5) {
            String playerName      = (String)     args[0];
            Cell startPosition     = (Cell)       args[1];
            Cell goalPosition      = (Cell)       args[2];

            @SuppressWarnings("unchecked")
            List<Token> tokens     = (List<Token>) args[3];
            config                 = (GameConfig)  args[4];

            // Initialize player state
            state = new PlayerState(playerName, startPosition,
                                    goalPosition, tokens);
        } else {
            System.err.println(getLocalName()
                             + ": Missing arguments. Using defaults.");
            config = new GameConfig(2);
            grid   = new Grid(config);
            state  = new PlayerState(
                getLocalName(),
                grid.getCell(0, 0),
                grid.getCell(0, config.getCols() - 1),
                new ArrayList<>()
            );
        }

        // Initialize grid and pathfinder using config
        if (grid == null) {
            grid = new Grid(config);
        }
        pathFinder = new PathFinder(grid);

        // Set environment agent
        environmentAgent = new AID("EnvironmentAgent", AID.ISLOCALNAME);

        // Build list of all partner agents dynamically
        partnerAgents = new ArrayList<>();
        for (int i = 0; i < config.getNumberOfPlayers(); i++) {
            String partnerName = "Player" + (i + 1);
            if (!partnerName.equals(getLocalName())) {
                partnerAgents.add(new AID(partnerName, AID.ISLOCALNAME));
            }
        }

        System.out.println("PlayerAgent initialized: " + state);
        System.out.println(state.getPlayerName()
                         + " personality: " + state.getPersonality());
        SimulationUI.log(state.getPlayerName()
                       + " personality: " + state.getPersonality());
        System.out.println("Partners: " + partnerAgents);

        // Start listening for messages
        addBehaviour(new ListenBehaviour());
    }

    // Listen Behaviour
    private class ListenBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
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

    // Message Handler
    private void handleMessage(ACLMessage msg) {
        String content = msg.getContent();
        String convId  = msg.getConversationId();
        SimulationUI.logMessage(msg.getSender().getLocalName(),
                                state.getPlayerName(), convId, content);

        // Game control messages
        if (convId.equals(CTOntology.CONV_GAME)) {
            handleGameMessage(content);
        }

        // Negotiation messages
        else if (convId.equals(CTOntology.CONV_NEGOTIATION)) {
            addBehaviour(new NegotiationBehaviour(
                PlayerAgent.this, msg, state,
                pathFinder, partnerAgents
            ));
        }

        // Transfer messages
        else if (convId.equals(CTOntology.CONV_TRANSFER)) {
            addBehaviour(new TransferBehaviour(
                PlayerAgent.this, msg, state, null
            ));
        }
    }

    // Handle Game Messages
    private void handleGameMessage(String content) {

        if (content.startsWith(CTOntology.GAME_START)) {
            System.out.println(state.getPlayerName()
                             + ": Game started! My state: " + state);
            SimulationUI.updatePlayer(state);
        }

        else if (content.startsWith(CTOntology.YOUR_TURN)) {
            System.out.println(state.getPlayerName() + ": It's my turn!");
            SimulationUI.log(state.getPlayerName() + " joue son tour");
            playTurn();
        }

        else if (content.startsWith(CTOntology.GAME_OVER)) {
            System.out.println(state.getPlayerName()
                             + ": Game over! My final score: "
                             + state.getScore());
            SimulationUI.updatePlayer(state);
            doDelete();
        }
    }

    // Play Turn
    private void playTurn() {
        List<Cell> path = pathFinder.findShortestPath(
            state.getCurrentPosition(),
            state.getGoalPosition()
        );

        if (path.isEmpty() || path.size() == 1) {
            // Already at goal or no path found
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
            SimulationUI.log(state.getPlayerName()
                           + " manque un jeton " + nextCell.getColor()
                           + " et lance une negociation");
            addBehaviour(new NegotiationBehaviour(
                PlayerAgent.this, null, state,
                pathFinder, partnerAgents
            ));
        }
    }

    // Notify Environment Turn Done
    public void notifyTurnDone() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(environmentAgent);
        msg.setOntology(CTOntology.ONTOLOGY_NAME);
        msg.setConversationId(CTOntology.CONV_GAME);
        msg.setContent(CTOntology.TURN_DONE);
        send(msg);
        SimulationUI.logMessage(state.getPlayerName(), "EnvironmentAgent",
                                CTOntology.CONV_GAME, msg.getContent());
        System.out.println(state.getPlayerName()
                         + ": Turn done. Notified environment.");
    }

    // Getters
    public void finishTurnAfterNegotiation() {
        List<Cell> path = pathFinder.findShortestPath(
            state.getCurrentPosition(),
            state.getGoalPosition()
        );

        if (path.isEmpty()) {
            notifyBlocked("No path to goal");
            return;
        }

        if (path.size() == 1) {
            notifyTurnDone();
            return;
        }

        Cell nextCell = path.get(1);
        if (state.hasToken(nextCell.getColor())) {
            addBehaviour(new MoveBehaviour(
                PlayerAgent.this, state, nextCell, environmentAgent
            ));
        } else {
            notifyBlocked("Missing token " + nextCell.getColor());
        }
    }

    public void notifyBlocked(String reason) {
        state.incrementBlockedTurns();
        SimulationUI.updatePlayer(state);
        SimulationUI.log(state.getPlayerName() + " bloque: " + reason);

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(environmentAgent);
        msg.setOntology(CTOntology.ONTOLOGY_NAME);
        msg.setConversationId(CTOntology.CONV_GAME);
        msg.setContent(CTOntology.BLOCKED + ";"
                     + CTOntology.KEY_PLAYER_NAME + "="
                     + state.getPlayerName()
                     + ";reason=" + reason);
        send(msg);
        SimulationUI.logMessage(state.getPlayerName(), "EnvironmentAgent",
                                CTOntology.CONV_GAME, msg.getContent());
    }

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

    public List<AID> getPartnerAgents() {
        return partnerAgents;
    }

    public GameConfig getConfig() {
        return config;
    }

    // Takedown
    @Override
    protected void takeDown() {
        System.out.println("PlayerAgent " + getLocalName()
                         + " shutting down.");
    }
}
