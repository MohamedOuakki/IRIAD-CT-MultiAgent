package main.java.ct.models;

import java.util.Random;

public class Grid {

    public static final int ROWS = 5;
    public static final int COLS = 7;

    private Cell[][] cells;

    public Grid() {
        cells = new Cell[ROWS][COLS];
        initializeGrid();
    }

    private void initializeGrid() {
        Token.Color[] colors = Token.Color.values();
        Random random = new Random();

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Token.Color randomColor = colors[random.nextInt(colors.length)];
                cells[row][col] = new Cell(row, col, randomColor);
            }
        }
    }

    public Cell getCell(int row, int col) {
        if (isValidPosition(row, col)) {
            return cells[row][col];
        }
        return null;
    }

    public boolean isValidPosition(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    public Cell[][] getCells() {
        return cells;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                sb.append(String.format("%-15s", cells[row][col].toString()));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}