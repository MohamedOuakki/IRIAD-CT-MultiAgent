package main.java.ct.models;

import java.util.ArrayList;
import java.util.List;

public class Offer {

    public enum Status {
        PENDING, ACCEPTED, REJECTED
    }

    private String proposerName;
    private String receiverName;
    private List<Token> tokensToGive;
    private List<Token> tokensToReceive;
    private Status status;

    public Offer(String proposerName, String receiverName,
                 List<Token> tokensToGive, List<Token> tokensToReceive) {
        this.proposerName = proposerName;
        this.receiverName = receiverName;
        this.tokensToGive = new ArrayList<>(tokensToGive);
        this.tokensToReceive = new ArrayList<>(tokensToReceive);
        this.status = Status.PENDING;
    }

    // --- Getters ---

    public String getProposerName() {
        return proposerName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public List<Token> getTokensToGive() {
        return tokensToGive;
    }

    public List<Token> getTokensToReceive() {
        return tokensToReceive;
    }

    public Status getStatus() {
        return status;
    }

    // --- Status Management ---

    public void accept() {
        this.status = Status.ACCEPTED;
    }

    public void reject() {
        this.status = Status.REJECTED;
    }

    public boolean isPending() {
        return status == Status.PENDING;
    }

    public boolean isAccepted() {
        return status == Status.ACCEPTED;
    }

    public boolean isRejected() {
        return status == Status.REJECTED;
    }

    // --- Validation ---

    public boolean isValid(PlayerState proposer) {
        for (Token token : tokensToGive) {
            if (!proposer.hasToken(token.getColor())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "Offer {" +
               "\n  From: " + proposerName +
               "\n  To: " + receiverName +
               "\n  Giving: " + tokensToGive +
               "\n  Wanting: " + tokensToReceive +
               "\n  Status: " + status +
               "\n}";
    }
}