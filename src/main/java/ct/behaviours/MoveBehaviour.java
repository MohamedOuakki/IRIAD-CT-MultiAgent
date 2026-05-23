package main.java.ct.behaviours;

import main.java.ct.models.Cell;
import main.java.ct.models.PlayerState;
import main.java.ct.models.Token;
import main.java.ct.ontology.CTOntology;
import main.java.ct.gui.SimulationUI;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class MoveBehaviour extends OneShotBehaviour {

    private PlayerState playerState;
    private Cell targetCell;
    private AID environmentAgent;

    // ─── Constructor ─────────────────────────────────────────────

    public MoveBehaviour(Agent agent, PlayerState playerState,
                         Cell targetCell, AID environmentAgent) {
        super(agent);
        this.playerState      = playerState;
        this.targetCell       = targetCell;
        this.environmentAgent = environmentAgent;
    }

    // ─── Action ──────────────────────────────────────────────────

    @Override
    public void action() {
        Token.Color requiredColor = targetCell.getColor();

        // Check if player has the required token
        if (!playerState.hasToken(requiredColor)) {
            System.out.println(playerState.getPlayerName()
                             + ": Cannot move to " + targetCell
                             + ". Missing token: " + requiredColor);
            SimulationUI.log(playerState.getPlayerName()
                           + " est bloque: jeton " + requiredColor
                           + " manquant");
            handleCannotMove();
            return;
        }

        // Consume the token
        playerState.removeToken(requiredColor);
        System.out.println(playerState.getPlayerName()
                         + ": Used token " + requiredColor
                         + " to move to " + targetCell);

        // Update position
        playerState.setCurrentPosition(targetCell);
        System.out.println(playerState.getPlayerName()
                         + ": Moved to " + targetCell);
        SimulationUI.updatePlayer(playerState);
        SimulationUI.log(playerState.getPlayerName()
                       + " se deplace vers (" + targetCell.getRow()
                       + "," + targetCell.getCol() + ") "
                       + targetCell.getColor());
        SimulationUI.pause(850);

        // Reset blocked turns since player successfully moved
        playerState.resetBlockedTurns();

        // Check if goal reached
        if (playerState.hasReachedGoal()) {
            System.out.println(playerState.getPlayerName()
                             + ": GOAL REACHED!");
            notifyGoalReached();
        } else {
            notifyMoveDone();
        }
    }

    // ─── Notify Environment: Move Done ───────────────────────────

    private void notifyMoveDone() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(environmentAgent);
        msg.setOntology(CTOntology.ONTOLOGY_NAME);
        msg.setConversationId(CTOntology.CONV_GAME);
        msg.setContent(CTOntology.TURN_DONE + ";"
                     + CTOntology.KEY_POSITION + "="
                     + playerState.getCurrentPosition());
        myAgent.send(msg);
        SimulationUI.logMessage(playerState.getPlayerName(),
                                environmentAgent.getLocalName(),
                                CTOntology.CONV_GAME, msg.getContent());
        SimulationUI.pause(350);
    }

    // ─── Notify Environment: Goal Reached ────────────────────────

    private void notifyGoalReached() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(environmentAgent);
        msg.setOntology(CTOntology.ONTOLOGY_NAME);
        msg.setConversationId(CTOntology.CONV_GAME);
        msg.setContent(CTOntology.TURN_DONE + ";"
                     + CTOntology.KEY_POSITION + "="
                     + playerState.getCurrentPosition()
                     + ";goalReached=true");
        myAgent.send(msg);
        SimulationUI.logMessage(playerState.getPlayerName(),
                                environmentAgent.getLocalName(),
                                CTOntology.CONV_GAME, msg.getContent());
        SimulationUI.pause(350);
    }

    // ─── Handle Cannot Move ──────────────────────────────────────

    private void handleCannotMove() {
        playerState.incrementBlockedTurns();
        System.out.println(playerState.getPlayerName()
                         + ": Blocked! Turns blocked: "
                         + playerState.getBlockedTurns());

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(environmentAgent);
        msg.setOntology(CTOntology.ONTOLOGY_NAME);
        msg.setConversationId(CTOntology.CONV_GAME);
        msg.setContent(CTOntology.BLOCKED + ";"
                     + CTOntology.KEY_PLAYER_NAME + "="
                     + playerState.getPlayerName());
        myAgent.send(msg);
        SimulationUI.updatePlayer(playerState);
        SimulationUI.logMessage(playerState.getPlayerName(),
                                environmentAgent.getLocalName(),
                                CTOntology.CONV_GAME, msg.getContent());
        SimulationUI.pause(350);
    }
}
