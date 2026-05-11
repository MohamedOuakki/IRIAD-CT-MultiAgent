package main.java.ct.utils;

import main.java.ct.models.Cell;
import main.java.ct.models.PlayerState;

import java.util.List;

public class ScoreCalculator {

    private static final int BONUS_REACHED_GOAL    = 100;
    private static final int BONUS_PER_TOKEN       = 5;
    private static final int PENALTY_PER_CELL      = 10;

    private PathFinder pathFinder;

    public ScoreCalculator(PathFinder pathFinder) {
        this.pathFinder = pathFinder;
    }

    // Calculates the final score of a player at the end of the game
    public int calculateFinalScore(PlayerState player) {
        int score = 0;

        // Bonus for reaching the goal
        if (player.hasReachedGoal()) {
            score += BONUS_REACHED_GOAL;
        } else {
            // Penalty for each missing cell in the remaining path
            int remainingCells = getRemainingCells(player);
            score -= remainingCells * PENALTY_PER_CELL;
        }

        // Bonus for each remaining token
        score += player.getTokens().size() * BONUS_PER_TOKEN;

        return score;
    }

    // Returns the number of cells left between player and goal
    public int getRemainingCells(PlayerState player) {
        List<Cell> remainingPath = pathFinder.findShortestPath(
            player.getCurrentPosition(),
            player.getGoalPosition()
        );

        // Subtract 1 to exclude the current position
        return remainingPath.isEmpty() ? 0 : remainingPath.size() - 1;
    }

    // Applies the final score to the player
    public void applyFinalScore(PlayerState player) {
        int finalScore = calculateFinalScore(player);
        player.setScore(finalScore);
        System.out.println("Final score for " + player.getPlayerName()
                         + ": " + finalScore);
    }

    // Prints a detailed score breakdown for a player
    public void printScoreBreakdown(PlayerState player) {
        System.out.println("=== Score Breakdown for " + player.getPlayerName() + " ===");

        if (player.hasReachedGoal()) {
            System.out.println("  + " + BONUS_REACHED_GOAL + " (reached goal)");
        } else {
            int remainingCells = getRemainingCells(player);
            System.out.println("  - " + (remainingCells * PENALTY_PER_CELL)
                             + " (" + remainingCells + " cells missing x "
                             + PENALTY_PER_CELL + ")");
        }

        int tokenBonus = player.getTokens().size() * BONUS_PER_TOKEN;
        System.out.println("  + " + tokenBonus + " ("
                         + player.getTokens().size() + " tokens remaining x "
                         + BONUS_PER_TOKEN + ")");

        System.out.println("  = Total: " + calculateFinalScore(player));
        System.out.println("==========================================");
    }
}