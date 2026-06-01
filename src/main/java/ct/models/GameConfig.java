package main.java.ct.models;

public class GameConfig {

    // Constants
    private static final int BASE_COLS         = 5;
    private static final int BASE_ROWS         = 7;
    private static final int ROWS_PER_EXTRA_PLAYER = 1;
    private static final int COLS_PER_EXTRA_PLAYER = 2;
    private static final int TOKENS_PER_PLAYER = 5;
    private static final int MIN_PLAYERS       = 2;
    private static final int MAX_PLAYERS       = 10;

    // Fields
    private int numberOfPlayers;
    private int rows;
    private int cols;
    private int tokensPerPlayer;

    // Constructor
    public GameConfig(int numberOfPlayers) {
        if (numberOfPlayers < MIN_PLAYERS) {
            System.out.println("Minimum players is " + MIN_PLAYERS
                             + ". Setting to " + MIN_PLAYERS + ".");
            numberOfPlayers = MIN_PLAYERS;
        }
        if (numberOfPlayers > MAX_PLAYERS) {
            System.out.println("Maximum players is " + MAX_PLAYERS
                             + ". Setting to " + MAX_PLAYERS + ".");
            numberOfPlayers = MAX_PLAYERS;
        }

        this.numberOfPlayers = numberOfPlayers;
        int extraPlayers = numberOfPlayers - MIN_PLAYERS;
        this.rows        = BASE_ROWS + (extraPlayers * ROWS_PER_EXTRA_PLAYER);
        this.cols        = BASE_COLS + (extraPlayers * COLS_PER_EXTRA_PLAYER);
        this.tokensPerPlayer = TOKENS_PER_PLAYER;
    }

    // Getters
    public int getNumberOfPlayers() {
        return numberOfPlayers;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getTokensPerPlayer() {
        return tokensPerPlayer;
    }

    // Position Helpers
    // Returns the start cell position for a given player index
    public int[] getStartPosition(int playerIndex) {
        int row = playerIndex % rows;
        int col = (playerIndex % 2 == 0) ? 0 : cols - 1;
        return new int[]{row, col};
    }

    // Returns the goal cell position for a given player index
    public int[] getGoalPosition(int playerIndex) {
        int row = playerIndex % rows;
        int col = (playerIndex % 2 == 0) ? cols - 1 : 0;
        return new int[]{row, col};
    }

    @Override
    public String toString() {
        return "GameConfig {" +
               "\n  Players : " + numberOfPlayers +
               "\n  Grid    : " + rows + " x " + cols +
               "\n  Tokens  : strategic distribution + controlled random" +
               "\n}";
    }
}
