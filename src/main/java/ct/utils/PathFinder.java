package main.java.ct.utils;

import main.java.ct.models.Cell;
import main.java.ct.models.Grid;
import main.java.ct.models.PlayerState;
import main.java.ct.models.Token;

import java.util.*;

public class PathFinder {

    private Grid grid;

    public PathFinder(Grid grid) {
        this.grid = grid;
    }

    // Returns the shortest path from current position to goal as a list of cells
    public List<Cell> findShortestPath(Cell start, Cell goal) {
        Queue<Cell> queue = new LinkedList<>();
        Map<Cell, Cell> cameFrom = new HashMap<>();

        queue.add(start);
        cameFrom.put(start, null);

        while (!queue.isEmpty()) {
            Cell current = queue.poll();

            if (current.equals(goal)) {
                return reconstructPath(cameFrom, start, goal);
            }

            for (Cell neighbor : getNeighbors(current)) {
                if (!cameFrom.containsKey(neighbor)) {
                    queue.add(neighbor);
                    cameFrom.put(neighbor, current);
                }
            }
        }

        return new ArrayList<>(); // No path found
    }

    // Returns neighbors (up, down, left, right) of a cell
    private List<Cell> getNeighbors(Cell cell) {
        List<Cell> neighbors = new ArrayList<>();
        int row = cell.getRow();
        int col = cell.getCol();

        int[][] directions = {
            {-1, 0}, // up
            {1, 0},  // down
            {0, -1}, // left
            {0, 1}   // right
        };

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            if (grid.isValidPosition(newRow, newCol)) {
                neighbors.add(grid.getCell(newRow, newCol));
            }
        }

        return neighbors;
    }

    // Reconstructs the path from start to goal using the cameFrom map
    private List<Cell> reconstructPath(Map<Cell, Cell> cameFrom, Cell start, Cell goal) {
        List<Cell> path = new ArrayList<>();
        Cell current = goal;

        while (current != null) {
            path.add(current);
            current = cameFrom.get(current);
        }

        Collections.reverse(path);
        return path;
    }

    // Returns tokens needed to follow a given path
    public List<Token> getTokensNeededForPath(List<Cell> path) {
        List<Token> needed = new ArrayList<>();
        // Skip the first cell (current position, no token needed)
        for (int i = 1; i < path.size(); i++) {
            needed.add(new Token(path.get(i).getColor()));
        }
        return needed;
    }

    // Returns tokens the player is missing to complete the path
    public List<Token> getMissingTokens(PlayerState player, List<Cell> path) {
        List<Token> needed = getTokensNeededForPath(path);
        List<Token> playerTokens = new ArrayList<>(player.getTokens());
        List<Token> missing = new ArrayList<>();

        for (Token token : needed) {
            boolean found = false;
            for (int i = 0; i < playerTokens.size(); i++) {
                if (playerTokens.get(i).getColor() == token.getColor()) {
                    playerTokens.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                missing.add(token);
            }
        }

        return missing;
    }
}