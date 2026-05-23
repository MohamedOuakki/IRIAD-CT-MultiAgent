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
        int tokenBonus = calculateTokenBonus(player);

        if (player.hasReachedGoal()) {
            return BONUS_REACHED_GOAL + tokenBonus;
        }

        return tokenBonus - calculateMissingCellsPenalty(player);
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

    public int calculateTokenBonus(PlayerState player) {
        return player.getTokens().size() * BONUS_PER_TOKEN;
    }

    public int calculateMissingCellsPenalty(PlayerState player) {
        return getRemainingCells(player) * PENALTY_PER_CELL;
    }

    public String buildScoreBreakdown(PlayerState player) {
        StringBuilder sb = new StringBuilder();
        int tokenBonus = calculateTokenBonus(player);

        sb.append(player.getPlayerName()).append(" : ");
        if (player.hasReachedGoal()) {
            sb.append(BONUS_REACHED_GOAL).append(" bonus but");
        } else {
            int remainingCells = getRemainingCells(player);
            sb.append("-").append(remainingCells * PENALTY_PER_CELL)
              .append(" penalite (")
              .append(remainingCells)
              .append(" cases restantes x ")
              .append(PENALTY_PER_CELL)
              .append(")");
        }

        sb.append(" + ").append(tokenBonus)
          .append(" jetons (")
          .append(player.getTokens().size())
          .append(" x ")
          .append(BONUS_PER_TOKEN)
          .append(") = ")
          .append(calculateFinalScore(player));

        return sb.toString();
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
            System.out.println("  + " + BONUS_REACHED_GOAL
                             + " (agent arrived at goal)");
        } else {
            int remainingCells = getRemainingCells(player);
            System.out.println("  - " + (remainingCells * PENALTY_PER_CELL)
                             + " (" + remainingCells + " remaining cells x "
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
