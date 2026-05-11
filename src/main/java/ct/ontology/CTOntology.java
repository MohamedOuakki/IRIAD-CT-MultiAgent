package main.java.ct.ontology;

public class CTOntology {

    // ─── Ontology Name ───────────────────────────────────────────
    public static final String ONTOLOGY_NAME = "ColoredTrails-Ontology";

    // ─── Conversation IDs ────────────────────────────────────────
    public static final String CONV_NEGOTIATION = "negotiation";
    public static final String CONV_TRANSFER    = "transfer";
    public static final String CONV_GAME        = "game";

    // ─── Performatives (Message Types) ───────────────────────────

    // Negotiation phase
    public static final String PROPOSE         = "PROPOSE";
    public static final String ACCEPT_PROPOSAL = "ACCEPT_PROPOSAL";
    public static final String REJECT_PROPOSAL = "REJECT_PROPOSAL";

    // Transfer phase
    public static final String TRANSFER_TOKENS  = "TRANSFER_TOKENS";
    public static final String CONFIRM_TRANSFER = "CONFIRM_TRANSFER";
    public static final String DENY_TRANSFER    = "DENY_TRANSFER";

    // Game control (EnvironmentAgent <-> PlayerAgents)
    public static final String GAME_START       = "GAME_START";
    public static final String GAME_OVER        = "GAME_OVER";
    public static final String YOUR_TURN        = "YOUR_TURN";
    public static final String TURN_DONE        = "TURN_DONE";
    public static final String MOVE             = "MOVE";
    public static final String BLOCKED          = "BLOCKED";
    public static final String UPDATE_STATE     = "UPDATE_STATE";

    // ─── Message Content Keys ────────────────────────────────────
    public static final String KEY_OFFER        = "offer";
    public static final String KEY_TOKENS_GIVE  = "tokensToGive";
    public static final String KEY_TOKENS_WANT  = "tokensToReceive";
    public static final String KEY_PLAYER_NAME  = "playerName";
    public static final String KEY_POSITION     = "position";
    public static final String KEY_SCORE        = "score";

    // ─── Message Format ──────────────────────────────────────────
    public static final String LANGUAGE         = "plain-text";

    // ─── Helper: Build message content string ────────────────────
    public static String buildContent(String key, String value) {
        return key + "=" + value;
    }

    public static String getValue(String content, String key) {
        for (String part : content.split(";")) {
            String[] pair = part.trim().split("=");
            if (pair.length == 2 && pair[0].trim().equals(key)) {
                return pair[1].trim();
            }
        }
        return null;
    }
}