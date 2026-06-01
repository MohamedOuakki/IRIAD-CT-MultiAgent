package main.java.ct;

import main.java.ct.models.GameConfig;
import main.java.ct.models.Grid;
import main.java.ct.models.PlayerState;
import main.java.ct.utils.TokenDistributor;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Scanner;

public class MainContainer {

    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {

        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   Colored Trails Multi-Agent System  ║");
        System.out.println("╚══════════════════════════════════════╝");

        // Step 1: Read Number of Players
        int numberOfPlayers = readNumberOfPlayers();

        // Step 2: Create Game Config
        GameConfig config = new GameConfig(numberOfPlayers);
        System.out.println("\nGame configuration:");
        System.out.println(config);

        // Step 3: Create Grid (for display only)
        Grid grid = new Grid(config);
        System.out.println("Grid initialized:");
        System.out.println(grid);
        TokenDistributor distributor = new TokenDistributor(config, grid);
        List<PlayerState> playerStates = distributor.createInitialPlayerStates();

        // Step 4: Start JADE Runtime
        Runtime runtime  = Runtime.instance();
        Profile profile  = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.LOCAL_PORT,
                             String.valueOf(findAvailablePort(1099)));
        profile.setParameter(Profile.GUI, "true");

        AgentContainer mainContainer =
            runtime.createMainContainer(profile);

        // Step 5: Launch Agents
        try {
            // Launch EnvironmentAgent first with config
            AgentController envAgent = mainContainer.createNewAgent(
                "EnvironmentAgent",
                "main.java.ct.agents.EnvironmentAgent",
                new Object[]{ config, grid, playerStates }
            );
            envAgent.start();
            System.out.println("\nEnvironmentAgent launched.");

            // Delay to let EnvironmentAgent fully initialize
            Thread.sleep(500);

            // Launch N PlayerAgents dynamically
            for (int i = 0; i < numberOfPlayers; i++) {
                PlayerState state = playerStates.get(i);

                // Create player agent
                AgentController player = mainContainer.createNewAgent(
                    state.getPlayerName(),
                    "main.java.ct.agents.PlayerAgent",
                    new Object[]{
                        state,
                        grid,
                        config
                    }
                );
                player.start();

                System.out.println(state.getPlayerName() + " launched."
                                 + " Start: "  + state.getCurrentPosition()
                                 + " | Goal: " + state.getGoalPosition()
                                 + " | Distance: "
                                 + distanceBetween(state.getCurrentPosition(),
                                                   state.getGoalPosition())
                                 + " | Personality: "
                                 + state.getPersonality()
                                 + " | Tokens: " + state.getTokens());

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

    // Read Number of Players
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

    // Generate Random Tokens
    private static int findAvailablePort(int startPort) {
        int port = startPort;
        while (port < startPort + 100) {
            try (ServerSocket socket = new ServerSocket(port)) {
                socket.setReuseAddress(true);
                return port;
            } catch (IOException e) {
                port++;
            }
        }
        return startPort;
    }

    private static int distanceBetween(main.java.ct.models.Cell first,
                                       main.java.ct.models.Cell second) {
        return Math.abs(first.getRow() - second.getRow())
             + Math.abs(first.getCol() - second.getCol());
    }
}
