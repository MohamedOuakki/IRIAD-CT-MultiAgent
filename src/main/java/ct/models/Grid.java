package main.java.ct.models;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Grid {

    private int rows;
    private int cols;
    private Cell[][] cells;
    private Cell[] startCells;
    private Cell[] goalCells;
    private Random random;
    private int targetDistance;

    // Constructor
    public Grid(GameConfig config) {
        this.rows  = config.getRows();
        this.cols  = config.getCols();
        this.cells = new Cell[rows][cols];
        this.startCells = new Cell[config.getNumberOfPlayers()];
        this.goalCells = new Cell[config.getNumberOfPlayers()];
        this.random = new Random();
        this.targetDistance = Math.max(3, (rows + cols) / 2);
        initializeGrid();
        initializePlayerPositions(config.getNumberOfPlayers());
    }

    // Initialize Grid
    private void initializeGrid() {
        Token.Color[] colors = Token.Color.values();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Token.Color randomColor = colors[random.nextInt(colors.length)];
                cells[row][col]         = new Cell(row, col, randomColor);
            }
        }
    }

    private void initializePlayerPositions(int numberOfPlayers) {
        Set<String> usedPositions = new HashSet<>();

        for (int i = 0; i < numberOfPlayers; i++) {
            Cell start = randomFreeCell(usedPositions);
            Cell goal = randomBalancedGoal(usedPositions, start);

            startCells[i] = start;
            goalCells[i] = goal;
            usedPositions.add(positionKey(start));
            usedPositions.add(positionKey(goal));
        }
    }

    private Cell randomFreeCell(Set<String> usedPositions) {
        Cell cell;
        do {
            cell = getCell(random.nextInt(rows), random.nextInt(cols));
        } while (usedPositions.contains(positionKey(cell)));
        return cell;
    }

    private Cell randomBalancedGoal(Set<String> usedPositions, Cell start) {
        for (int tolerance = 1; tolerance <= targetDistance; tolerance++) {
            Cell candidate = findGoalCandidate(usedPositions, start, tolerance);
            if (candidate != null) {
                return candidate;
            }
        }
        return randomFreeGoalOnDifferentRow(usedPositions, start);
    }

    private Cell findGoalCandidate(Set<String> usedPositions, Cell start,
                                   int tolerance) {
        Cell selected = null;
        int seen = 0;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Cell candidate = getCell(row, col);
                if (usedPositions.contains(positionKey(candidate))) continue;
                if (candidate.getRow() == start.getRow()) continue;

                int distance = manhattanDistance(start, candidate);
                if (Math.abs(distance - targetDistance) <= tolerance) {
                    seen++;
                    if (random.nextInt(seen) == 0) {
                        selected = candidate;
                    }
                }
            }
        }

        return selected;
    }

    private Cell randomFreeGoalOnDifferentRow(Set<String> usedPositions,
                                              Cell start) {
        Cell cell;
        do {
            cell = randomFreeCell(usedPositions);
        } while (cell.getRow() == start.getRow());
        return cell;
    }

    private int manhattanDistance(Cell first, Cell second) {
        return Math.abs(first.getRow() - second.getRow())
             + Math.abs(first.getCol() - second.getCol());
    }

    private String positionKey(Cell cell) {
        return cell.getRow() + ":" + cell.getCol();
    }

    // Getters
    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public Cell getCell(int row, int col) {
        if (isValidPosition(row, col)) {
            return cells[row][col];
        }
        return null;
    }

    public Cell[][] getCells() {
        return cells;
    }

    // Validation
    public boolean isValidPosition(int row, int col) {
        return row >= 0 && row < rows
            && col >= 0 && col < cols;
    }

    // Get Start Cell for Player
    public Cell getStartCell(int playerIndex) {
        return startCells[playerIndex];
    }

    // Get Goal Cell for Player
    public Cell getGoalCell(int playerIndex) {
        return goalCells[playerIndex];
    }

    // Print Grid
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Grid [" + rows + " x " + cols + "]\n");
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                sb.append(String.format("%-15s", cells[row][col].toString()));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
