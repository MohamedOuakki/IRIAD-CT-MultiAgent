package main.java.ct.gui;

import main.java.ct.models.Grid;
import main.java.ct.models.PlayerState;
import main.java.ct.ontology.CTOntology;

import javax.swing.SwingUtilities;
import java.util.Collection;

public final class SimulationUI {

    private static GameUI ui;

    private SimulationUI() {
    }

    public static synchronized void show(Grid grid, Collection<PlayerState> players) {
        if (ui == null) {
            ui = GameUI.open(grid, players);
        } else {
            setPlayers(players);
        }
    }

    public static void setPlayers(Collection<PlayerState> players) {
        GameUI current = ui;
        if (current == null) return;
        SwingUtilities.invokeLater(() -> current.setPlayers(players));
    }

    public static void updatePlayer(PlayerState player) {
        GameUI current = ui;
        if (current == null) return;
        SwingUtilities.invokeLater(() -> current.updatePlayer(player));
    }

    public static void setActivePlayer(String playerName) {
        GameUI current = ui;
        if (current == null) return;
        SwingUtilities.invokeLater(() -> current.setActivePlayer(playerName));
    }

    public static void log(String message) {
        GameUI current = ui;
        if (current == null) return;
        SwingUtilities.invokeLater(() -> current.addLog(message));
    }

    public static void logMessage(String from, String to, String conversation, String content) {
        GameUI current = ui;
        String readableContent = formatMessageContent(from, to, conversation, content);
        if (current != null) {
            SwingUtilities.invokeLater(() -> current.showCommunication(from, to, conversation, readableContent));
        }
        log(from + " -> " + to + " [" + conversation + "] " + readableContent);
    }

    public static void pause(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String formatMessageContent(String from, String to,
                                               String conversation,
                                               String content) {
        if (content == null) {
            return "";
        }

        if (CTOntology.CONV_TRANSFER.equals(conversation)) {
            return formatTransferMessage(from, to, content);
        }
        if (CTOntology.CONV_NEGOTIATION.equals(conversation)) {
            return formatNegotiationMessage(from, to, content);
        }
        return content;
    }

    private static String formatTransferMessage(String from, String to,
                                                String content) {
        if (content.startsWith(CTOntology.TRANSFER_TOKENS)) {
            String sent = formatTokens(CTOntology.getValue(content, CTOntology.KEY_TOKENS_GIVE));
            String expected = formatTokens(CTOntology.getValue(content, CTOntology.KEY_TOKENS_WANT));
            return "Demande de transfert : " + from
                 + " envoie " + sent
                 + " a " + to
                 + " et attend en retour " + expected;
        }
        if (content.startsWith(CTOntology.CONFIRM_TRANSFER)) {
            String sent = formatTokens(CTOntology.getValue(content, CTOntology.KEY_TOKENS_GIVE));
            return "Transfert confirme : " + from
                 + " renvoie " + sent
                 + " a " + to;
        }
        if (content.startsWith(CTOntology.DENY_TRANSFER)) {
            return "Transfert refuse : " + from
                 + " ne respecte pas l'accord avec " + to;
        }
        return content;
    }

    private static String formatNegotiationMessage(String from, String to,
                                                   String content) {
        if (content.startsWith(CTOntology.PROPOSE)) {
            String offered = formatTokens(CTOntology.getValue(content, CTOntology.KEY_TOKENS_GIVE));
            String requested = formatTokens(CTOntology.getValue(content, CTOntology.KEY_TOKENS_WANT));
            return "Offre : " + from
                 + " propose " + offered
                 + " a " + to
                 + " contre " + requested;
        }
        if (content.startsWith(CTOntology.ACCEPT_PROPOSAL)) {
            String offered = formatTokens(CTOntology.getValue(content, CTOntology.KEY_TOKENS_GIVE));
            String requested = formatTokens(CTOntology.getValue(content, CTOntology.KEY_TOKENS_WANT));
            return "Offre acceptee : " + from
                 + " accepte, donnera " + offered
                 + " et recevra " + requested;
        }
        if (content.startsWith(CTOntology.REJECT_PROPOSAL)) {
            return "Offre rejetee : " + from
                 + " refuse la proposition de " + to;
        }
        return content;
    }

    private static String formatTokens(String rawTokens) {
        if (rawTokens == null || rawTokens.trim().isEmpty() || "none".equals(rawTokens)) {
            return "aucun jeton";
        }
        return "[" + rawTokens.replace(",", ", ") + "]";
    }
}
