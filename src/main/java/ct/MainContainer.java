package main.java.ct;

import main.java.ct.models.GameConfig;
import main.java.ct.models.Grid;
import main.java.ct.models.Token;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.util.Scanner;

public class MainContainer {

    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {

        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   Colored Trails Multi-Agent System  ║");
        System.out.println("╚══════════════════════════════════════╝");

        // ─── Step 1: Read Number of Players ──────────────────────
        int numberOfPlayers = readNumberOfPlayers();

        // ─── Step 2: Create Game Config ───────────────────────────
        GameConfig config = new GameConfig(numberOfPlayers);
        System.out.println("\nGame configuration:");
        System.out.println(config);

        // ─── Step 3: Create Grid (for display only) ───────────────
        Grid grid = new Grid(config);
        System.out.println("Grid initialized:");
        System.out.println(grid);

        // ─── Step 4: Start JADE Runtime ───────────────────────────
        Runtime runtime  = Runtime.instance();
        Profile profile  = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.GUI, "true");

        AgentContainer mainContainer =
            runtime.createMainContainer(profile);

        // ─── Step 5: Launch Agents ────────────────────────────────
        try {
            // Launch EnvironmentAgent first with config
            AgentController envAgent = mainContainer.createNewAgent(
                "EnvironmentAgent",
                "ct.agents.EnvironmentAgent",
                new Object[]{ config }
            );
            envAgent.start();
            System.out.println("\nEnvironmentAgent launched.");

            // Delay to let EnvironmentAgent fully initialize
            Thread.sleep(2000);

            // Launch N PlayerAgents dynamically
            for (int i = 0; i < numberOfPlayers; i++) {
                String playerName = "Player" + (i + 1);

                // Get start and goal from grid
                main.java.ct.models.Cell startCell = grid.getStartCell(i);
                main.java.ct.models.Cell goalCell  = grid.getGoalCell(i);

                // Generate random tokens
                java.util.List<Token> tokens =
                    generateRandomTokens(config.getTokensPerPlayer());

                // Create player agent
                AgentController player = mainContainer.createNewAgent(
                    playerName,
                    "ct.agents.PlayerAgent",
                    new Object[]{
                        playerName,
                        startCell,
                        goalCell,
                        tokens,
                        config
                    }
                );
                player.start();

                System.out.println(playerName + " launched."
                                 + " Start: "  + startCell
                                 + " | Goal: " + goalCell
                                 + " | Tokens: " + tokens);

                // Small delay between player launches
                Thread.sleep(500);
            }

        } catch (StaleProxyException e) {
            System.err.println("Error creating agents: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("Interrupted: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\nAll agents launched. Game is running...");
    }

    // ─── Read Number of Players ───────────────────────────────────

    private static int readNumberOfPlayers() {
        int number = 0;

        while (number < 2 || number > 10) {
            System.out.print("\nEnter number of players (2-10): ");
            try {
                number = Integer.parseInt(SCANNER.nextLine().trim());
                if (number < 2 || number > 10) {
                    System.out.println("Please enter a number between 2 and 10.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }

        System.out.println("Number of players: " + number);
        return number;
    }

    // ─── Generate Random Tokens ───────────────────────────────────

    private static java.util.List<Token> generateRandomTokens(int count) {
        java.util.List<Token> tokens = new java.util.ArrayList<>();
        Token.Color[] colors         = Token.Color.values();
        java.util.Random random      = new java.util.Random();

        for (int i = 0; i < count; i++) {
            tokens.add(new Token(colors[random.nextInt(colors.length)]));
        }
        return tokens;
    }
}