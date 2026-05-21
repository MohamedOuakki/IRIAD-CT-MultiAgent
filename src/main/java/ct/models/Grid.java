package main.java.ct.models;

import java.util.Random;

public class Grid {

    private int rows;
    private int cols;
    private Cell[][] cells;

    // ─── Constructor ─────────────────────────────────────────────
    public Grid(GameConfig config) {
        this.rows  = config.getRows();
        this.cols  = config.getCols();
        this.cells = new Cell[rows][cols];
        initializeGrid();
    }

    // ─── Initialize Grid ─────────────────────────────────────────
    private void initializeGrid() {
        Token.Color[] colors = Token.Color.values();
        Random random        = new Random();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Token.Color randomColor = colors[random.nextInt(colors.length)];
                cells[row][col]         = new Cell(row, col, randomColor);
            }
        }
    }

    // ─── Getters ─────────────────────────────────────────────────
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

    // ─── Validation ──────────────────────────────────────────────
    public boolean isValidPosition(int row, int col) {
        return row >= 0 && row < rows
            && col >= 0 && col < cols;
    }

    // ─── Get Start Cell for Player ───────────────────────────────
    public Cell getStartCell(int playerIndex) {
        int[] pos = new int[2];
        pos[0]    = playerIndex % rows;
        pos[1]    = (playerIndex % 2 == 0) ? 0 : cols - 1;
        return getCell(pos[0], pos[1]);
    }

    // ─── Get Goal Cell for Player ────────────────────────────────
    public Cell getGoalCell(int playerIndex) {
        int[] pos = new int[2];
        pos[0]    = playerIndex % rows;
        pos[1]    = (playerIndex % 2 == 0) ? cols - 1 : 0;
        return getCell(pos[0], pos[1]);
    }

    // ─── Print Grid ──────────────────────────────────────────────
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