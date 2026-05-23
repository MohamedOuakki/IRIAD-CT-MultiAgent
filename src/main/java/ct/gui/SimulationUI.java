package main.java.ct.gui;

import main.java.ct.models.Grid;
import main.java.ct.models.PlayerState;

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
        if (current != null) {
            SwingUtilities.invokeLater(() -> current.showCommunication(from, to, conversation, content));
        }
        log(from + " -> " + to + " [" + conversation + "] " + content);
    }

    public static void pause(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
