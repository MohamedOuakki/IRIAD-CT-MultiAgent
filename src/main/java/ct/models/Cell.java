package main.java.ct.models;

public class Cell {

    private int row;
    private int col;
    private Token.Color color;

    public Cell(int row, int col, Token.Color color) {
        this.row = row;
        this.col = col;
        this.color = color;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public Token.Color getColor() {
        return color;
    }

    public void setColor(Token.Color color) {
        this.color = color;
    }

    @Override
    public String toString() {
        return "Cell(" + row + ", " + col + ", " + color + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Cell)) return false;
        Cell other = (Cell) obj;
        return this.row == other.row && this.col == other.col;
    }

    @Override
    public int hashCode() {
        return 7 * row + 31 * col;
    }
}