package main.java.ct.gui;

import main.java.ct.models.Cell;
import main.java.ct.models.Grid;
import main.java.ct.models.PlayerState;
import main.java.ct.models.Token;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameUI extends JFrame {

    private static final Color[] PLAYER_COLORS = {
        new Color(30, 64, 175),
        new Color(190, 24, 93),
        new Color(21, 128, 61),
        new Color(180, 83, 9),
        new Color(109, 40, 217),
        new Color(8, 145, 178),
        new Color(185, 28, 28),
        new Color(77, 124, 15),
        new Color(67, 56, 202),
        new Color(15, 118, 110)
    };

    private final Grid grid;
    private final GridPanel gridPanel;
    private final JTextArea statusArea;
    private final JTextArea logArea;
    private final JTextArea movementArea;
    private final JComboBox<String> eventTypeFilter;
    private final JComboBox<String> playerFilter;
    private final List<EventRecord> eventHistory;
    private final Map<String, PlayerState> players;
    private final Map<String, Color> playerColors;
    private final Map<String, double[]> drawnPositions;
    private String activePlayer;
    private String communicationFrom;
    private String communicationTo;
    private String communicationLabel;

    public GameUI(Grid grid, Collection<PlayerState> initialPlayers) {
        super("Colored Trails - Simulation");
        this.grid = grid;
        this.players = new LinkedHashMap<>();
        this.playerColors = new LinkedHashMap<>();
        this.drawnPositions = new LinkedHashMap<>();

        gridPanel = new GridPanel();
        statusArea = new JTextArea();
        logArea = new JTextArea();
        movementArea = new JTextArea();
        eventTypeFilter = new JComboBox<>(new String[]{
            "Tous", "Mouvements", "Negociations", "Transferts", "Tours", "Scores", "Autres"
        });
        playerFilter = new JComboBox<>();
        eventHistory = new ArrayList<>();

        configureTextArea(statusArea);
        configureTextArea(logArea);
        configureMovementArea();

        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        leftPanel.add(gridPanel, BorderLayout.CENTER);
        leftPanel.add(movementArea, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 12));
        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setBorder(BorderFactory.createTitledBorder("Etat des joueurs"));
        statusScroll.setPreferredSize(new Dimension(330, 235));
        rightPanel.add(statusScroll, BorderLayout.NORTH);
        rightPanel.add(buildEventPanel(), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.68);
        splitPane.setDividerSize(6);
        add(splitPane);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 700));
        setLocationRelativeTo(null);

        setPlayers(initialPlayers);
        configureFilters();
    }

    public void setPlayers(Collection<PlayerState> updatedPlayers) {
        Object selectedPlayer = playerFilter.getSelectedItem();
        players.clear();
        playerColors.clear();
        drawnPositions.clear();
        playerFilter.removeAllItems();
        playerFilter.addItem("Tous les joueurs");
        int index = 0;
        boolean selectedPlayerStillExists = "Tous les joueurs".equals(selectedPlayer);
        for (PlayerState state : updatedPlayers) {
            players.put(state.getPlayerName(), state);
            playerColors.put(state.getPlayerName(), PLAYER_COLORS[index % PLAYER_COLORS.length]);
            drawnPositions.put(state.getPlayerName(), new double[]{
                state.getCurrentPosition().getRow(),
                state.getCurrentPosition().getCol()
            });
            playerFilter.addItem(state.getPlayerName());
            if (state.getPlayerName().equals(selectedPlayer)) {
                selectedPlayerStillExists = true;
            }
            index++;
        }
        if (selectedPlayer != null && selectedPlayerStillExists) {
            playerFilter.setSelectedItem(selectedPlayer);
        }
        refresh();
    }

    public void updatePlayer(PlayerState state) {
        players.put(state.getPlayerName(), state);
        if (!playerColors.containsKey(state.getPlayerName())) {
            playerColors.put(state.getPlayerName(), PLAYER_COLORS[playerColors.size() % PLAYER_COLORS.length]);
        }
        updateMovementArea(state);
        animatePlayerTo(state);
        refresh();
    }

    public void setActivePlayer(String playerName) {
        activePlayer = playerName;
        gridPanel.repaint();
    }

    public void showCommunication(String from, String to, String conversation, String content) {
        communicationFrom = from;
        communicationTo = to;
        communicationLabel = conversation + " : " + simplifyContent(content);
        gridPanel.repaint();

        Timer timer = new Timer(1400, event -> {
            if (from.equals(communicationFrom) && to.equals(communicationTo)) {
                communicationFrom = null;
                communicationTo = null;
                communicationLabel = null;
                gridPanel.repaint();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    public void addLog(String text) {
        addEvent(detectEventType(text), detectPlayer(text), text);
    }

    private void refresh() {
        statusArea.setText(buildStatusText());
        gridPanel.repaint();
    }

    private void updateMovementArea(PlayerState state) {
        double[] start = drawnPositions.get(state.getPlayerName());
        Cell target = state.getCurrentPosition();
        if (start == null) {
            return;
        }

        int fromRow = (int) Math.round(start[0]);
        int fromCol = (int) Math.round(start[1]);
        int toRow = target.getRow();
        int toCol = target.getCol();
        if (fromRow == toRow && fromCol == toCol) {
            return;
        }

        movementArea.setText(
            "MOUVEMENT | " + state.getPlayerName()
          + " (" + state.getPersonality() + ") : "
          + "(" + fromRow + "," + fromCol + ") -> "
          + "(" + toRow + "," + toCol + ")"
          + " | case " + target.getColor()
          + " | jetons restants: " + state.getTokens().size()
        );
        addEvent("Mouvements", state.getPlayerName(), movementArea.getText());
    }

    private void animatePlayerTo(PlayerState state) {
        Cell target = state.getCurrentPosition();
        double[] start = drawnPositions.get(state.getPlayerName());
        if (start == null) {
            drawnPositions.put(state.getPlayerName(), new double[]{target.getRow(), target.getCol()});
            return;
        }

        double fromRow = start[0];
        double fromCol = start[1];
        double toRow = target.getRow();
        double toCol = target.getCol();
        if (fromRow == toRow && fromCol == toCol) {
            return;
        }

        int frames = 18;
        final int[] frame = {0};
        Timer timer = new Timer(35, null);
        timer.addActionListener(event -> {
            frame[0]++;
            double t = Math.min(1.0, frame[0] / (double) frames);
            double eased = t * t * (3 - 2 * t);
            drawnPositions.put(state.getPlayerName(), new double[]{
                fromRow + (toRow - fromRow) * eased,
                fromCol + (toCol - fromCol) * eased
            });
            gridPanel.repaint();
            if (t >= 1.0) {
                timer.stop();
            }
        });
        timer.start();
    }

    private JPanel buildEventPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Historique"));
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filters.add(new JLabel("Evenements"));
        filters.add(eventTypeFilter);
        filters.add(new JLabel("Joueur"));
        filters.add(playerFilter);
        panel.add(filters, BorderLayout.NORTH);
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return panel;
    }

    private void configureFilters() {
        eventTypeFilter.setMaximumRowCount(7);
        playerFilter.setMaximumRowCount(10);
        eventTypeFilter.setToolTipText("Filtrer les mouvements, negociations, transferts, tours ou scores");
        playerFilter.setToolTipText("Afficher seulement les evenements lies a un joueur");
        eventTypeFilter.addActionListener(event -> refreshEventLog());
        playerFilter.addActionListener(event -> refreshEventLog());
        refreshEventLog();
    }

    private void addEvent(String type, String playerName, String text) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        eventHistory.add(new EventRecord(time, type, playerName, text));
        refreshEventLog();
    }

    private void refreshEventLog() {
        String selectedType = (String) eventTypeFilter.getSelectedItem();
        String selectedPlayer = (String) playerFilter.getSelectedItem();
        StringBuilder sb = new StringBuilder();
        for (EventRecord event : eventHistory) {
            if (!matchesType(selectedType, event) || !matchesPlayer(selectedPlayer, event)) {
                continue;
            }
            sb.append("[")
              .append(event.time)
              .append("] [")
              .append(event.type)
              .append("] ");
            if (event.playerName != null) {
                sb.append(event.playerName).append(" | ");
            }
            sb.append(event.text).append("\n");
        }
        logArea.setText(sb.toString());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private boolean matchesType(String selectedType, EventRecord event) {
        return selectedType == null || "Tous".equals(selectedType) || selectedType.equals(event.type);
    }

    private boolean matchesPlayer(String selectedPlayer, EventRecord event) {
        return selectedPlayer == null
            || "Tous les joueurs".equals(selectedPlayer)
            || selectedPlayer.equals(event.playerName)
            || (event.text != null && event.text.contains(selectedPlayer));
    }

    private String detectEventType(String text) {
        String lower = text == null ? "" : text.toLowerCase();
        if (lower.contains("mouvement") || lower.contains("avance") || lower.contains("deplace")) {
            return "Mouvements";
        }
        if (lower.contains("negociation") || lower.contains("offre") || lower.contains("accepte")
            || lower.contains("rejette") || lower.contains("refuse") || lower.contains("proposition")) {
            return "Negociations";
        }
        if (lower.contains("score")) {
            return "Scores";
        }
        if (lower.contains("transfert") || lower.contains("jeton") || lower.contains("trahison")
            || lower.contains("respecte l'accord")) {
            return "Transferts";
        }
        if (lower.contains("tour") || lower.contains("bloque")) {
            return "Tours";
        }
        return "Autres";
    }

    private String detectPlayer(String text) {
        if (text == null) {
            return null;
        }
        for (String playerName : players.keySet()) {
            if (text.contains(playerName)) {
                return playerName;
            }
        }
        return null;
    }

    private String buildStatusText() {
        StringBuilder sb = new StringBuilder();
        sb.append("JOUEURS\n\n");
        for (PlayerState state : players.values()) {
            sb.append(state.getPlayerName()).append("\n");
            sb.append("  Profil   : ").append(state.getPersonality()).append("\n");
            sb.append("  Position : ").append(formatCell(state.getCurrentPosition())).append("\n");
            sb.append("  But      : ").append(formatCell(state.getGoalPosition())).append("\n");
            sb.append("  Tokens   : ").append(formatTokens(state)).append("\n");
            sb.append("  Score    : ").append(state.getScore()).append("\n");
            sb.append("  Bloque   : ").append(state.getBlockedTurns()).append(" tour(s)\n\n");
        }
        return sb.toString();
    }

    private String formatCell(Cell cell) {
        return "(" + cell.getRow() + "," + cell.getCol() + ") " + cell.getColor();
    }

    private String formatTokens(PlayerState state) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < state.getTokens().size(); i++) {
            sb.append(state.getTokens().get(i).getColor());
            if (i < state.getTokens().size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private String simplifyContent(String content) {
        if (content == null) return "";
        if (content.length() <= 90) return content;
        return content.substring(0, 87) + "...";
    }

    private void configureTextArea(JTextArea area) {
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private void configureMovementArea() {
        movementArea.setEditable(false);
        movementArea.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        movementArea.setLineWrap(true);
        movementArea.setWrapStyleWord(true);
        movementArea.setRows(2);
        movementArea.setText("MOUVEMENT | En attente du premier deplacement");
        movementArea.setForeground(new Color(15, 23, 42));
        movementArea.setBackground(new Color(226, 232, 240));
        movementArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 116, 139), 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
    }

    private Color toAwtColor(Token.Color color) {
        switch (color) {
            case RED:
                return new Color(248, 113, 113);
            case GREEN:
                return new Color(134, 239, 172);
            case BLUE:
                return new Color(147, 197, 253);
            case YELLOW:
                return new Color(253, 224, 71);
            case PURPLE:
                return new Color(196, 181, 253);
            case GRAY:
            default:
                return new Color(209, 213, 219);
        }
    }

    private static class EventRecord {
        private final String time;
        private final String type;
        private final String playerName;
        private final String text;

        private EventRecord(String time, String type, String playerName, String text) {
            this.time = time;
            this.type = type;
            this.playerName = playerName;
            this.text = text;
        }
    }

    private class GridPanel extends JPanel {

        GridPanel() {
            setBackground(new Color(248, 250, 252));
            setPreferredSize(new Dimension(720, 620));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int padding = 24;
            int availableWidth = getWidth() - padding * 2;
            int availableHeight = getHeight() - padding * 2;
            int cellSize = Math.max(24, Math.min(availableWidth / grid.getCols(), availableHeight / grid.getRows()));
            int gridWidth = cellSize * grid.getCols();
            int gridHeight = cellSize * grid.getRows();
            int startX = (getWidth() - gridWidth) / 2;
            int startY = (getHeight() - gridHeight) / 2;

            drawCells(g, startX, startY, cellSize);
            drawGoals(g, startX, startY, cellSize);
            drawCommunication(g, startX, startY, cellSize);
            drawPlayers(g, startX, startY, cellSize);

            g.dispose();
        }

        private void drawCells(Graphics2D g, int startX, int startY, int cellSize) {
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(10, cellSize / 5)));
            for (int row = 0; row < grid.getRows(); row++) {
                for (int col = 0; col < grid.getCols(); col++) {
                    Cell cell = grid.getCell(row, col);
                    int x = startX + col * cellSize;
                    int y = startY + row * cellSize;
                    g.setColor(toAwtColor(cell.getColor()));
                    g.fillRect(x, y, cellSize, cellSize);
                    g.setColor(new Color(51, 65, 85));
                    g.drawRect(x, y, cellSize, cellSize);
                }
            }
        }

        private void drawGoals(Graphics2D g, int startX, int startY, int cellSize) {
            g.setStroke(new BasicStroke(Math.max(2, cellSize / 12)));
            for (PlayerState state : players.values()) {
                Cell goal = state.getGoalPosition();
                int x = startX + goal.getCol() * cellSize;
                int y = startY + goal.getRow() * cellSize;
                g.setColor(playerColors.get(state.getPlayerName()));
                int margin = Math.max(5, cellSize / 7);
                g.drawRect(x + margin, y + margin, cellSize - margin * 2, cellSize - margin * 2);
            }
            g.setStroke(new BasicStroke(1));
        }

        private void drawPlayers(Graphics2D g, int startX, int startY, int cellSize) {
            Map<String, List<PlayerState>> byCell = new LinkedHashMap<>();
            for (PlayerState state : players.values()) {
                double[] drawn = drawnPositions.get(state.getPlayerName());
                int row = drawn == null ? state.getCurrentPosition().getRow() : (int) Math.round(drawn[0]);
                int col = drawn == null ? state.getCurrentPosition().getCol() : (int) Math.round(drawn[1]);
                String key = row + ":" + col;
                byCell.computeIfAbsent(key, ignored -> new ArrayList<>()).add(state);
            }

            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(11, cellSize / 4)));
            for (List<PlayerState> cellPlayers : byCell.values()) {
                for (int i = 0; i < cellPlayers.size(); i++) {
                    PlayerState state = cellPlayers.get(i);
                    double[] drawn = drawnPositions.get(state.getPlayerName());
                    double row = drawn == null ? state.getCurrentPosition().getRow() : drawn[0];
                    double col = drawn == null ? state.getCurrentPosition().getCol() : drawn[1];
                    int x = startX + (int) Math.round(col * cellSize);
                    int y = startY + (int) Math.round(row * cellSize);
                    int size = Math.max(18, cellSize / 2);
                    int offset = i * Math.max(5, size / 4);
                    int circleX = x + (cellSize - size) / 2 + offset;
                    int circleY = y + (cellSize - size) / 2 + offset;

                    if (state.getPlayerName().equals(activePlayer)) {
                        g.setColor(new Color(15, 23, 42));
                        g.setStroke(new BasicStroke(4));
                        g.drawOval(circleX - 5, circleY - 5, size + 10, size + 10);
                        g.setStroke(new BasicStroke(1));
                    }
                    g.setColor(playerColors.get(state.getPlayerName()));
                    g.fillOval(circleX, circleY, size, size);
                    g.setColor(Color.WHITE);
                    g.drawString(state.getPlayerName().replace("Player", "P"), circleX + 5, circleY + size / 2 + 5);
                }
            }
        }

        private void drawCommunication(Graphics2D g, int startX, int startY, int cellSize) {
            if (communicationFrom == null || communicationTo == null) return;
            double[] from = drawnPositions.get(communicationFrom);
            double[] to = drawnPositions.get(communicationTo);
            if (from == null || to == null) return;

            int x1 = startX + (int) Math.round((from[1] + 0.5) * cellSize);
            int y1 = startY + (int) Math.round((from[0] + 0.5) * cellSize);
            int x2 = startX + (int) Math.round((to[1] + 0.5) * cellSize);
            int y2 = startY + (int) Math.round((to[0] + 0.5) * cellSize);

            g.setColor(new Color(15, 23, 42));
            g.setStroke(new BasicStroke(Math.max(3, cellSize / 14), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(x1, y1, x2, y2);
            drawArrowHead(g, x1, y1, x2, y2, Math.max(10, cellSize / 4));

            if (communicationLabel != null) {
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, cellSize / 5)));
                int labelX = (x1 + x2) / 2;
                int labelY = (y1 + y2) / 2 - 8;
                g.setColor(new Color(255, 255, 255, 225));
                int width = Math.min(g.getFontMetrics().stringWidth(communicationLabel) + 14, getWidth() - 48);
                g.fillRoundRect(labelX - width / 2, labelY - 18, width, 24, 8, 8);
                g.setColor(new Color(15, 23, 42));
                g.drawString(communicationLabel, labelX - width / 2 + 7, labelY);
            }
            g.setStroke(new BasicStroke(1));
        }

        private void drawArrowHead(Graphics2D g, int x1, int y1, int x2, int y2, int size) {
            double angle = Math.atan2(y2 - y1, x2 - x1);
            int xA = x2 - (int) (Math.cos(angle - Math.PI / 6) * size);
            int yA = y2 - (int) (Math.sin(angle - Math.PI / 6) * size);
            int xB = x2 - (int) (Math.cos(angle + Math.PI / 6) * size);
            int yB = y2 - (int) (Math.sin(angle + Math.PI / 6) * size);
            g.drawLine(x2, y2, xA, yA);
            g.drawLine(x2, y2, xB, yB);
        }
    }

    public static GameUI open(Grid grid, Collection<PlayerState> players) {
        final GameUI[] frame = new GameUI[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame[0] = new GameUI(grid, players);
                frame[0].setVisible(true);
            });
        } catch (Exception e) {
            throw new IllegalStateException("Unable to open game UI", e);
        }
        return frame[0];
    }
}
