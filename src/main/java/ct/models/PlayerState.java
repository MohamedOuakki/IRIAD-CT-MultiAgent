package main.java.ct.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PlayerState {

    private String playerName;
    private Cell currentPosition;
    private Cell goalPosition;
    private List<Token> tokens;
    private AgentPersonality personality;
    private int score;
    private int blockedTurns;

    public PlayerState(String playerName, Cell currentPosition, Cell goalPosition, List<Token> tokens) {
        this(playerName, currentPosition, goalPosition, tokens, randomPersonality());
    }

    public PlayerState(String playerName, Cell currentPosition, Cell goalPosition,
                       List<Token> tokens, AgentPersonality personality) {
        this.playerName = playerName;
        this.currentPosition = currentPosition;
        this.goalPosition = goalPosition;
        this.tokens = new ArrayList<>(tokens);
        this.personality = personality;
        this.score = 0;
        this.blockedTurns = 0;
    }

    // --- Getters & Setters ---

    public String getPlayerName() {
        return playerName;
    }

    public Cell getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(Cell currentPosition) {
        this.currentPosition = currentPosition;
    }

    public Cell getGoalPosition() {
        return goalPosition;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public AgentPersonality getPersonality() {
        return personality;
    }

    public void setPersonality(AgentPersonality personality) {
        this.personality = personality;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getBlockedTurns() {
        return blockedTurns;
    }

    public void incrementBlockedTurns() {
        this.blockedTurns++;
    }

    public void resetBlockedTurns() {
        this.blockedTurns = 0;
    }

    // --- Token Management ---

    public boolean hasToken(Token.Color color) {
        for (Token token : tokens) {
            if (token.getColor() == color) {
                return true;
            }
        }
        return false;
    }

    public void addToken(Token token) {
        tokens.add(token);
    }

    public boolean removeToken(Token.Color color) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).getColor() == color) {
                tokens.remove(i);
                return true;
            }
        }
        return false;
    }

    // --- Goal Check ---

    public boolean hasReachedGoal() {
        return currentPosition.equals(goalPosition);
    }

    public boolean isBlocked() {
        return blockedTurns >= 3;
    }

    private static AgentPersonality randomPersonality() {
        AgentPersonality[] personalities = AgentPersonality.values();
        return personalities[new Random().nextInt(personalities.length)];
    }

    @Override
    public String toString() {
        return "Player: " + playerName +
               " | Personality: " + personality +
               " | Position: " + currentPosition +
               " | Goal: " + goalPosition +
               " | Tokens: " + tokens +
               " | Score: " + score +
               " | Blocked Turns: " + blockedTurns;
    }
}
