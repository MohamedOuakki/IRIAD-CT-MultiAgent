package main.java.ct.behaviours;

import main.java.ct.models.PlayerState;
import main.java.ct.models.Token;
import main.java.ct.ontology.CTOntology;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.List;

public class TransferBehaviour extends OneShotBehaviour {

    private ACLMessage incomingMessage;
    private PlayerState playerState;
    private AID selectedPartner;

    // ─── Constructor ─────────────────────────────────────────────

    public TransferBehaviour(Agent agent, ACLMessage incomingMessage,
                             PlayerState playerState, AID selectedPartner) {
        super(agent);
        this.incomingMessage = incomingMessage;
        this.playerState     = playerState;
        this.selectedPartner = selectedPartner;
    }

    // ─── Action ──────────────────────────────────────────────────

    @Override
    public void action() {
        String content = incomingMessage.getContent();
        AID sender     = incomingMessage.getSender();

        if (content.startsWith(CTOntology.ACCEPT_PROPOSAL)) {
            // We are the proposer, initiate the transfer
            initiateTransfer(selectedPartner != null ? selectedPartner : sender);
        } else if (content.startsWith(CTOntology.TRANSFER_TOKENS)) {
            // We are the receiver, handle incoming tokens
            handleIncomingTransfer(incomingMessage);
        } else if (content.startsWith(CTOntology.CONFIRM_TRANSFER)) {
            // Transfer confirmed by receiver
            handleTransferConfirmed(incomingMessage);
        } else if (content.startsWith(CTOntology.DENY_TRANSFER)) {
            // Transfer denied by receiver
            handleTransferDenied(incomingMessage);
        }
    }

    // ─── Initiate Transfer ───────────────────────────────────────

    private void initiateTransfer(AID receiver) {
        System.out.println(playerState.getPlayerName()
                         + ": Initiating transfer to "
                         + receiver.getLocalName());

        // Decide which tokens to actually send
        List<Token> tokensToSend = decideTokensToSend();

        if (tokensToSend.isEmpty()) {
            // Agent decides to cheat — sends nothing
            System.out.println(playerState.getPlayerName()
                             + ": Decided NOT to honor the agreement!");
            sendDenyTransfer(receiver);
            return;
        }

        // Remove tokens from own inventory
        for (Token token : tokensToSend) {
            playerState.removeToken(token.getColor());
        }

        System.out.println(playerState.getPlayerName()
                         + ": Sending tokens -> " + tokensToSend
                         + " to " + receiver.getLocalName());

        // Send tokens to partner
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(receiver);
        msg.setOntology(CTOntology.ONTOLOGY_NAME);
        msg.setConversationId(CTOntology.CONV_TRANSFER);
        msg.setContent(CTOntology.TRANSFER_TOKENS + ";"
                     + CTOntology.KEY_TOKENS_GIVE + "="
                     + serializeTokens(tokensToSend));
        myAgent.send(msg);

        // Wait for confirmation from selected partner
        waitForConfirmation(receiver);
    }

    // ─── Handle Incoming Transfer ────────────────────────────────

    private void handleIncomingTransfer(ACLMessage msg) {
        String content       = msg.getContent();
        AID sender           = msg.getSender();
        String tokensStr     = CTOntology.getValue(content,
                                 CTOntology.KEY_TOKENS_GIVE);
        List<Token> received = deserializeTokens(tokensStr);

        System.out.println(playerState.getPlayerName()
                         + ": Received tokens from "
                         + sender.getLocalName()
                         + " -> " + received);

        // Decide whether to honor the agreement
        boolean willHonor = decideToHonor(received);

        if (willHonor) {
            // Add received tokens to inventory
            for (Token token : received) {
                playerState.addToken(token);
            }
            System.out.println(playerState.getPlayerName()
                             + ": Honored the agreement. Tokens added.");

            // Send back agreed tokens
            sendConfirmTransfer(sender);
        } else {
            // Agent cheats
            System.out.println(playerState.getPlayerName()
                             + ": CHEATED! Keeping received tokens "
                             + "without sending back agreed tokens.");
            for (Token token : received) {
                playerState.addToken(token);
            }
            sendDenyTransfer(sender);
        }
    }

    // ─── Send Confirm Transfer ───────────────────────────────────

    private void sendConfirmTransfer(AID receiver) {
        List<Token> tokensToSendBack = decideTokensToSend();

        // Remove from inventory
        for (Token token : tokensToSendBack) {
            playerState.removeToken(token.getColor());
        }

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(receiver);
        msg.setOntology(CTOntology.ONTOLOGY_NAME);
        msg.setConversationId(CTOntology.CONV_TRANSFER);
        msg.setContent(CTOntology.CONFIRM_TRANSFER + ";"
                     + CTOntology.KEY_TOKENS_GIVE + "="
                     + serializeTokens(tokensToSendBack));
        myAgent.send(msg);

        System.out.println(playerState.getPlayerName()
                         + ": Sent confirmation with tokens -> "
                         + tokensToSendBack
                         + " to " + receiver.getLocalName());
    }

    // ─── Send Deny Transfer ──────────────────────────────────────

    private void sendDenyTransfer(AID receiver) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(receiver);
        msg.setOntology(CTOntology.ONTOLOGY_NAME);
        msg.setConversationId(CTOntology.CONV_TRANSFER);
        msg.setContent(CTOntology.DENY_TRANSFER);
        myAgent.send(msg);

        System.out.println(playerState.getPlayerName()
                         + ": Sent transfer denial to "
                         + receiver.getLocalName());
    }

    // ─── Handle Transfer Confirmed ───────────────────────────────

    private void handleTransferConfirmed(ACLMessage msg) {
        String content       = msg.getContent();
        String tokensStr     = CTOntology.getValue(content,
                                 CTOntology.KEY_TOKENS_GIVE);
        List<Token> received = deserializeTokens(tokensStr);

        for (Token token : received) {
            playerState.addToken(token);
        }

        System.out.println(playerState.getPlayerName()
                         + ": Transfer confirmed by "
                         + msg.getSender().getLocalName()
                         + "! Received -> " + received
                         + " | Updated tokens: "
                         + playerState.getTokens());
    }

    // ─── Handle Transfer Denied ──────────────────────────────────

    private void handleTransferDenied(ACLMessage msg) {
        System.out.println(playerState.getPlayerName()
                         + ": Transfer DENIED by "
                         + msg.getSender().getLocalName()
                         + ". Partner did not honor the agreement!");
        System.out.println(playerState.getPlayerName()
                         + ": Proceeding with current tokens: "
                         + playerState.getTokens());
    }

    // ─── Wait For Confirmation ───────────────────────────────────

    private void waitForConfirmation(AID partner) {
        MessageTemplate mt = MessageTemplate.and(
            MessageTemplate.MatchConversationId(CTOntology.CONV_TRANSFER),
            MessageTemplate.MatchSender(partner)
        );

        ACLMessage response = myAgent.blockingReceive(mt, 5000);

        if (response != null) {
            String content = response.getContent();
            if (content.startsWith(CTOntology.CONFIRM_TRANSFER)) {
                handleTransferConfirmed(response);
            } else {
                handleTransferDenied(response);
            }
        } else {
            System.out.println(playerState.getPlayerName()
                             + ": Transfer confirmation from "
                             + partner.getLocalName()
                             + " timed out.");
        }
    }

    // ─── Decision Helpers ────────────────────────────────────────

    private List<Token> decideTokensToSend() {
        // Honest strategy: always send tokens as agreed
        // Can be modified to implement cheating strategy
        List<Token> toSend = new ArrayList<>();
        if (!playerState.getTokens().isEmpty()) {
            toSend.add(playerState.getTokens().get(0));
        }
        return toSend;
    }

    private boolean decideToHonor(List<Token> received) {
        // Honest strategy: always honor the agreement
        // Can be modified to implement cheating strategy
        return true;
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