package main.java.ct;

import main.java.ct.models.Cell;
import main.java.ct.models.Grid;
import main.java.ct.models.Token;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainContainer {

    public static void main(String[] args) {

        System.out.println("Starting Colored Trails Multi-Agent System...");

        // ─── Step 1: Start JADE Runtime ──────────────────────────
        Runtime runtime = Runtime.instance();

        // Enable JADE GUI
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.GUI, "true");

        // Create main container
        AgentContainer mainContainer = runtime.createMainContainer(profile);

        // ─── Step 2: Create Grid ──────────────────────────────────
        Grid grid = new Grid();
        System.out.println("Grid initialized:");
        System.out.println(grid);

        // ─── Step 3: Setup Player 1 ───────────────────────────────
        Cell start1 = grid.getCell(0, 0);
        Cell goal1  = grid.getCell(0, 6);
        List<Token> tokens1 = generateRandomTokens(5);

        // ─── Step 4: Setup Player 2 ───────────────────────────────
        Cell start2 = grid.getCell(4, 6);
        Cell goal2  = grid.getCell(4, 0);
        List<Token> tokens2 = generateRandomTokens(5);

        // ─── Step 5: Launch Agents ────────────────────────────────
        try {
            // Launch EnvironmentAgent
            AgentController envAgent = mainContainer.createNewAgent(
                "EnvironmentAgent",
                "main.java.ct.agents.EnvironmentAgent",
                new Object[]{}
            );
            envAgent.start();
            System.out.println("EnvironmentAgent launched.");

            // Small delay to let EnvironmentAgent initialize first
            Thread.sleep(1000);

            // Launch Player 1
            AgentController player1 = mainContainer.createNewAgent(
                "Player1",
                "main.java.ct.agents.PlayerAgent",
                new Object[]{
                    "Player1",
                    start1,
                    goal1,
                    tokens1
                }
            );
            player1.start();
            System.out.println("Player1 launched."
                             + " Start: " + start1
                             + " | Goal: " + goal1
                             + " | Tokens: " + tokens1);

            // Launch Player 2
            AgentController player2 = mainContainer.createNewAgent(
                "Player2",
                "main.java.ct.agents.PlayerAgent",
                new Object[]{
                    "Player2",
                    start2,
                    goal2,
                    tokens2
                }
            );
            player2.start();
            System.out.println("Player2 launched."
                             + " Start: " + start2
                             + " | Goal: " + goal2
                             + " | Tokens: " + tokens2);

        } catch (StaleProxyException e) {
            System.err.println("Error creating agents: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("Interrupted: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("All agents launched. Game is running...");
    }

    // ─── Generate Random Tokens ───────────────────────────────────

    private static List<Token> generateRandomTokens(int count) {
        List<Token> tokens     = new ArrayList<>();
        Token.Color[] colors   = Token.Color.values();
        Random random          = new Random();

        for (int i = 0; i < count; i++) {
            tokens.add(new Token(colors[random.nextInt(colors.length)]));
        }

        System.out.println("Generated tokens: " + tokens);
        return tokens;
    }
}