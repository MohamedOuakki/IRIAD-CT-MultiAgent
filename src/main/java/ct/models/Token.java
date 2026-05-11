package main.java.ct.models;

public class Token {
    
    public enum Color {
        RED, GREEN, BLUE, YELLOW, PURPLE, GRAY
    }
    
    private Color color;

    public Token(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public String toString() {
        return "Token(" + color + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Token)) return false;
        Token other = (Token) obj;
        return this.color == other.color;
    }

    @Override
    public int hashCode() {
        return color.hashCode();
    }
}