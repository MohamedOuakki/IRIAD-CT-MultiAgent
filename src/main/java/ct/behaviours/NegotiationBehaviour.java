package main.java.ct.behaviours;

import main.java.ct.models.*;
import main.java.ct.ontology.CTOntology;
import main.java.ct.utils.PathFinder;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.List;

public class NegotiationBehaviour extends OneShotBehaviour {

    private ACLMessage incomingMessage;
    private PlayerState playerState;
    private PathFinder pathFinder;
    private List<AID> partnerAgents;
    private AID selectedPartner;

    // ─── Constructor ─────────────────────────────────────────────

    public NegotiationBehaviour(Agent agent, ACLMessage incomingMessage,
                                PlayerState playerState, PathFinder pathFinder,
                                List<AID> partnerAgents) {
        super(agent);
        this.incomingMessage = incomingMessage;
        this.playerState     = playerState;
        this.pathFinder      = pathFinder;
        this.partnerAgents   = partnerAgents;
        this.selectedPartner = null;
    }

    // ─── Action ──────────────────────────────────────────────────

    @Override
    public void action() {
        // If no incoming message, this agent is the proposer
        if (incomingMessage == null) {
            propose();
        } else {
            String content = incomingMessage.getContent();

            if (content.startsWith(CTOntology.PROPOSE)) {
                handleProposal(incomingMessage);
            } else if (content.startsWith(CTOntology.ACCEPT_PROPOSAL)) {
                handleAcceptance(incomingMessage);
            } else if (content.startsWith(CTOntology.REJECT_PROPOSAL)) {
                handleRejection(incomingMessage);
            }
        }
    }

    // ─── Propose ─────────────────────────────────────────────────

    private void propose() {
        // Find shortest path
        List<Cell> path = pathFinder.findShortestPath(
            playerState.getCurrentPosition(),
            playerState.getGoalPosition()
        );

        // Find missing tokens
        List<Token> missingTokens = pathFinder.getMissingTokens(
            playerState, path
        );

        if (missingTokens.isEmpty()) {
            System.out.println(playerState.getPlayerName()
                             + ": No tokens needed, no proposal necessary.");
            return;
        }

        // Try each partner until one accepts
        for (AID partner : partnerAgents) {
            System.out.println(playerState.getPlayerName()
                             + ": Trying to negotiate with "
                             + partner.getLocalName());

            boolean accepted = sendProposalTo(partner, path, missingTokens);

            if (accepted) {
                selectedPartner = partner;
                System.out.println(playerState.getPlayerName()
                                 + ": Negotiation successful with "
                                 + partner.getLocalName());
                return;
            } else {
                System.out.println(playerState.getPlayerName()
                                 + ": " + partner.getLocalName()
                                 + " rejected. Trying next partner...");
            }
        }

        System.out.println(playerState.getPlayerName()
                         + ": All partners rejected. "
                         + "Will proceed with available tokens.");
    }

    // ─── Send Proposal To Specific Partner ───────────────────────

    private boolean sendProposalTo(AID partner, List<Cell> path,
                                   List<Token> missingTokens) {
        // Build offer
        List<Token> tokensToGive    = getSurplusTokens(path);
        List<Token> tokensToReceive = missingTokens;

        Offer offer = new Offer(
            playerState.getPlayerName(),
            partner.getLocalName(),
            tokensToGive,
            tokensToReceive
        );

        // Send proposal
        ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
        msg.addReceiver(partner);
        msg.setOntology(CTOntology.ONTOLOGY_NAME);
        msg.setConversationId(CTOntology.CONV_NEGOTIATION);
        msg.setContent(CTOntology.PROPOSE + ";"
                     + CTOntology.KEY_TOKENS_GIVE + "="
                     + serializeTokens(tokensToGive) + ";"
                     + CTOntology.KEY_TOKENS_WANT + "="
                     + serializeTokens(tokensToReceive));
        myAgent.send(msg);

        System.out.println(playerState.getPlayerName()
                         + ": Sent proposal to "
                         + partner.getLocalName()
                         + " -> " + offer);

        // Wait for response from this specific partner
        MessageTemplate mt = MessageTemplate.and(
            MessageTemplate.MatchConversationId(CTOntology.CONV_NEGOTIATION),
            MessageTemplate.MatchSender(partner)
        );

        ACLMessage response = myAgent.blockingReceive(mt, 5000);

        if (response != null) {
            String content = response.getContent();
            if (content.startsWith(CTOntology.ACCEPT_PROPOSAL)) {
                handleAcceptance(response);
                return true;
            } else {
                handleRejection(response);
                return false;
            }
        } else {
            System.out.println(playerState.getPlayerName()
                             + ": No response from "
                             + partner.getLocalName()
                             + ". Timed out.");
            return false;
        }
    }

    // ─── Handle Incoming Proposal ────────────────────────────────

    private void handleProposal(ACLMessage msg) {
        String content = msg.getContent();
        AID sender     = msg.getSender();

        // Parse tokens from message
        String tokensGiveStr    = CTOntology.getValue(content,
                                    CTOntology.KEY_TOKENS_GIVE);
        String tokensReceiveStr = CTOntology.getValue(content,
                                    CTOntology.KEY_TOKENS_WANT);

        List<Token> theyGive = deserializeTokens(tokensGiveStr);
        List<Token> theyWant = deserializeTokens(tokensReceiveStr);

        System.out.println(playerState.getPlayerName()
                         + ": Received proposal from "
                         + sender.getLocalName()
                         + " | They give: " + theyGive
                         + " | They want: " + theyWant);

        // Evaluate offer
        boolean canFulfill = canFulfillRequest(theyWant);
        boolean isUseful   = isOfferUseful(theyGive);

        if (canFulfill && isUseful) {
            acceptProposal(sender, theyGive, theyWant);
        } else {
            rejectProposal(sender);
        }
    }

    // ─── Accept Proposal ─────────────────────────────────────────

    private void acceptProposal(AID sender, List<Token> theyGive,
                                List<Token> theyWant) {
        System.out.println(playerState.getPlayerName()
                         + ": Accepting proposal from "
                         + sender.getLocalName());

        ACLMessage reply = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
        reply.addReceiver(sender);
        reply.setOntology(CTOntology.ONTOLOGY_NAME);
        reply.setConversationId(CTOntology.CONV_NEGOTIATION);
        reply.setContent(CTOntology.ACCEPT_PROPOSAL);
        myAgent.send(reply);
    }

    // ─── Reject Proposal ─────────────────────────────────────────

    private void rejectProposal(AID sender) {
        System.out.println(playerState.getPlayerName()
                         + ": Rejecting proposal from "
                         + sender.getLocalName());

        ACLMessage reply = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
        reply.addReceiver(sender);
        reply.setOntology(CTOntology.ONTOLOGY_NAME);
        reply.setConversationId(CTOntology.CONV_NEGOTIATION);
        reply.setContent(CTOntology.REJECT_PROPOSAL);
        myAgent.send(reply);
    }

    // ─── Handle Acceptance ───────────────────────────────────────

    private void handleAcceptance(ACLMessage msg) {
        // Track who accepted
        selectedPartner = msg.getSender();

        System.out.println(playerState.getPlayerName()
                         + ": Proposal accepted by "
                         + selectedPartner.getLocalName()
                         + "! Proceeding to transfer.");

        // Pass selectedPartner to TransferBehaviour
        myAgent.addBehaviour(new TransferBehaviour(
            myAgent, msg, playerState, selectedPartner
        ));
    }

    // ─── Handle Rejection ────────────────────────────────────────

    private void handleRejection(ACLMessage msg) {
        System.out.println(playerState.getPlayerName()
                         + ": Proposal rejected by "
                         + msg.getSender().getLocalName());
    }

    // ─── Getter ──────────────────────────────────────────────────

    public AID getSelectedPartner() {
        return selectedPartner;
    }

    // ─── Evaluation Helpers ──────────────────────────────────────

    private boolean canFulfillRequest(List<Token> requested) {
        for (Token token : requested) {
            if (!playerState.hasToken(token.getColor())) {
                return false;
            }
        }
        return true;
    }

    private boolean isOfferUseful(List<Token> offered) {
        List<Cell> myPath = pathFinder.findShortestPath(
            playerState.getCurrentPosition(),
            playerState.getGoalPosition()
        );
        List<Token> myMissing = pathFinder.getMissingTokens(
            playerState, myPath
        );

        for (Token token : offered) {
            for (Token missing : myMissing) {
                if (token.getColor() == missing.getColor()) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Token> getSurplusTokens(List<Cell> path) {
        List<Token> needed  = pathFinder.getTokensNeededForPath(path);
        List<Token> owned   = new ArrayList<>(playerState.getTokens());
        List<Token> surplus = new ArrayList<>();

        for (Token token : needed) {
            owned.remove(token);
        }

        int count = 0;
        for (Token token : owned) {
            if (count >= 2) break;
            surplus.add(token);
            count++;
        }

        return surplus;
    }

    // ─── Serialization Helpers ───────────────────────────────────

    private String serializeTokens(List<Token> tokens) {
        if (tokens.isEmpty()) return "none";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            sb.append(tokens.get(i).getColor().name());
            if (i < tokens.size() - 1) sb.append(",");
        }
        return sb.toString();
    }

    private List<Token> deserializeTokens(String data) {
        List<Token> tokens = new ArrayList<>();
        if (data == null || data.equals("none")) return tokens;
        for (String colorName : data.split(",")) {
            try {
                Token.Color color = Token.Color.valueOf(colorName.trim());
                tokens.add(new Token(color));
            } catch (IllegalArgumentException e) {
                System.err.println("Unknown token color: " + colorName);
            }
        }
        return tokens;
    }
}