package main.java.ct.utils;

import main.java.ct.models.Cell;
import main.java.ct.models.GameConfig;
import main.java.ct.models.Grid;
import main.java.ct.models.PlayerState;
import main.java.ct.models.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TokenDistributor {

    private static final double OWN_PATH_RATIO = 0.65;
    private static final int RANDOM_TOKENS_MIN = 1;
    private static final int RANDOM_TOKENS_MAX = 2;
    private static final int MAX_TOKENS_ABOVE_AVERAGE = 4;

    private final GameConfig config;
    private final Grid grid;
    private final PathFinder pathFinder;
    private final Random random;

    public TokenDistributor(GameConfig config, Grid grid) {
        this.config = config;
        this.grid = grid;
        this.pathFinder = new PathFinder(grid);
        this.random = new Random();
    }

    public List<PlayerState> createInitialPlayerStates() {
        int playerCount = config.getNumberOfPlayers();
        List<List<Cell>> paths = new ArrayList<>();
        List<List<Token>> neededTokens = new ArrayList<>();
        List<List<Token>> assignedTokens = new ArrayList<>();
        List<List<Token>> forcedMissingTokens = new ArrayList<>();

        for (int i = 0; i < playerCount; i++) {
            List<Cell> path = pathFinder.findShortestPath(
                grid.getStartCell(i),
                grid.getGoalCell(i)
            );
            List<Token> needed = pathFinder.getTokensNeededForPath(path);
            paths.add(path);
            neededTokens.add(needed);
            assignedTokens.add(new ArrayList<>());
            forcedMissingTokens.add(new ArrayList<>());
        }

        assignOwnUsefulTokens(neededTokens, assignedTokens, forcedMissingTokens);
        distributeMissingTokensToOtherAgents(neededTokens, assignedTokens);
        addControlledRandomTokens(neededTokens, assignedTokens);
        ensureNegotiationIsRequired(neededTokens, assignedTokens, forcedMissingTokens);
        List<String> repairLogs = validateAndRepairDistribution(
            neededTokens,
            assignedTokens,
            forcedMissingTokens
        );

        List<PlayerState> states = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            states.add(new PlayerState(
                "Player" + (i + 1),
                grid.getStartCell(i),
                grid.getGoalCell(i),
                assignedTokens.get(i)
            ));
        }

        printDistributionLogs(paths, neededTokens, assignedTokens,
                              forcedMissingTokens, repairLogs);
        return states;
    }

    private void assignOwnUsefulTokens(List<List<Token>> neededTokens,
                                       List<List<Token>> assignedTokens,
                                       List<List<Token>> forcedMissingTokens) {
        for (int i = 0; i < neededTokens.size(); i++) {
            List<Token> needed = neededTokens.get(i);
            if (needed.isEmpty()) continue;

            List<Token> shuffledNeeded = copyTokens(needed);
            Collections.shuffle(shuffledNeeded, random);

            Token forcedMissing = shuffledNeeded.remove(random.nextInt(shuffledNeeded.size()));
            forcedMissingTokens.get(i).add(forcedMissing);

            int ownUsefulCount = Math.max(1, (int) Math.round(needed.size() * OWN_PATH_RATIO));
            ownUsefulCount = Math.min(ownUsefulCount, shuffledNeeded.size());

            for (int j = 0; j < ownUsefulCount; j++) {
                assignedTokens.get(i).add(new Token(shuffledNeeded.get(j).getColor()));
            }
        }
    }

    private void distributeMissingTokensToOtherAgents(List<List<Token>> neededTokens,
                                                     List<List<Token>> assignedTokens) {
        int playerCount = neededTokens.size();

        for (int owner = 0; owner < playerCount; owner++) {
            List<Token> missing = subtractTokens(
                neededTokens.get(owner),
                assignedTokens.get(owner)
            );

            for (Token token : missing) {
                int receiver = chooseOtherAgent(owner, playerCount);
                assignedTokens.get(receiver).add(new Token(token.getColor()));
            }
        }
    }

    private void addControlledRandomTokens(List<List<Token>> neededTokens,
                                           List<List<Token>> assignedTokens) {
        Token.Color[] colors = Token.Color.values();
        for (int i = 0; i < assignedTokens.size(); i++) {
            List<Token> tokens = assignedTokens.get(i);
            int randomCount = RANDOM_TOKENS_MIN
                + random.nextInt(RANDOM_TOKENS_MAX - RANDOM_TOKENS_MIN + 1);
            for (int j = 0; j < randomCount; j++) {
                Token.Color color = chooseRandomSafeColor(
                    colors,
                    subtractTokens(neededTokens.get(i), tokens)
                );
                if (color != null) {
                    tokens.add(new Token(color));
                }
            }
        }
    }

    private Token.Color chooseRandomSafeColor(Token.Color[] colors,
                                              List<Token> missingTokens) {
        List<Token.Color> missingColors = new ArrayList<>();
        for (Token token : missingTokens) {
            if (!missingColors.contains(token.getColor())) {
                missingColors.add(token.getColor());
            }
        }

        List<Token.Color> safeColors = new ArrayList<>();
        for (Token.Color color : colors) {
            if (!missingColors.contains(color)) {
                safeColors.add(color);
            }
        }

        if (safeColors.isEmpty()) {
            return null;
        }
        return safeColors.get(random.nextInt(safeColors.size()));
    }

    private void ensureNegotiationIsRequired(List<List<Token>> neededTokens,
                                             List<List<Token>> assignedTokens,
                                             List<List<Token>> forcedMissingTokens) {
        for (int i = 0; i < neededTokens.size(); i++) {
            if (!subtractTokens(neededTokens.get(i), assignedTokens.get(i)).isEmpty()) {
                continue;
            }

            List<Token> needed = neededTokens.get(i);
            if (needed.isEmpty()) continue;

            Token tokenToMove = needed.get(random.nextInt(needed.size()));
            boolean removed = removeOneTokenAndReport(
                assignedTokens.get(i),
                tokenToMove.getColor()
            );
            if (removed) {
                int receiver = chooseOtherAgent(i, assignedTokens.size());
                Token movedToken = new Token(tokenToMove.getColor());
                assignedTokens.get(receiver).add(movedToken);
                forcedMissingTokens.get(i).add(movedToken);
            }
        }
    }

    private List<String> validateAndRepairDistribution(
            List<List<Token>> neededTokens,
            List<List<Token>> assignedTokens,
            List<List<Token>> forcedMissingTokens) {
        List<String> repairLogs = new ArrayList<>();

        for (int i = 0; i < assignedTokens.size(); i++) {
            ensureNegotiationIsRequiredForAgent(
                i,
                neededTokens,
                assignedTokens,
                forcedMissingTokens,
                repairLogs
            );
            ensureMissingTokensExistInOtherInventories(
                i,
                neededTokens,
                assignedTokens,
                repairLogs
            );
            ensureAgentHasSomethingToOffer(
                i,
                neededTokens,
                assignedTokens,
                repairLogs
            );

            if (!canAgentReachGoalWithNegotiation(i, neededTokens, assignedTokens)) {
                repairLogs.add("Player" + (i + 1)
                             + " still had no realistic route; adding fallback exchange tokens.");
                ensureMissingTokensExistInOtherInventories(
                    i,
                    neededTokens,
                    assignedTokens,
                    repairLogs
                );
                ensureAgentHasSomethingToOffer(
                    i,
                    neededTokens,
                    assignedTokens,
                    repairLogs
                );
            }
        }

        rebalanceInventories(neededTokens, assignedTokens, repairLogs);

        for (int i = 0; i < assignedTokens.size(); i++) {
            ensureNegotiationIsRequiredForAgent(
                i,
                neededTokens,
                assignedTokens,
                forcedMissingTokens,
                repairLogs
            );
            ensureMissingTokensExistInOtherInventories(
                i,
                neededTokens,
                assignedTokens,
                repairLogs
            );
            ensureAgentHasSomethingToOffer(
                i,
                neededTokens,
                assignedTokens,
                repairLogs
            );
        }

        return repairLogs;
    }

    private boolean canAgentReachGoalWithNegotiation(
            int agentIndex,
            List<List<Token>> neededTokens,
            List<List<Token>> assignedTokens) {
        List<Token> missing = subtractTokens(
            neededTokens.get(agentIndex),
            assignedTokens.get(agentIndex)
        );
        return !missing.isEmpty()
            && allMissingTokensAvailableElsewhere(agentIndex, missing, assignedTokens)
            && hasUsefulOffer(agentIndex, neededTokens, assignedTokens);
    }

    private void ensureMissingTokensExistInOtherInventories(
            int agentIndex,
            List<List<Token>> neededTokens,
            List<List<Token>> assignedTokens,
            List<String> repairLogs) {
        List<Token> missing = subtractTokens(
            neededTokens.get(agentIndex),
            assignedTokens.get(agentIndex)
        );

        for (Token token : missing) {
            int available = countColorInOtherInventories(
                agentIndex,
                token.getColor(),
                assignedTokens
            );
            int required = countColor(missing, token.getColor());

            while (available < required) {
                int receiver = chooseOtherAgent(agentIndex, assignedTokens.size());
                assignedTokens.get(receiver).add(new Token(token.getColor()));
                available++;
                repairLogs.add("Added missing " + token.getColor()
                             + " for Player" + (agentIndex + 1)
                             + " into Player" + (receiver + 1)
                             + "'s inventory.");
            }
        }
    }

    private void ensureAgentHasSomethingToOffer(
            int agentIndex,
            List<List<Token>> neededTokens,
            List<List<Token>> assignedTokens,
            List<String> repairLogs) {
        if (hasUsefulOffer(agentIndex, neededTokens, assignedTokens)) {
            return;
        }

        Token.Color exchangeColor = findUsefulOfferColorForAnotherAgent(
            agentIndex,
            neededTokens,
            assignedTokens
        );
        if (exchangeColor == null) {
            exchangeColor = randomNonBlockingColor(agentIndex, neededTokens, assignedTokens);
        }

        assignedTokens.get(agentIndex).add(new Token(exchangeColor));
        repairLogs.add("Gave Player" + (agentIndex + 1)
                     + " exchangeable " + exchangeColor
                     + " so it can propose at least one useful offer.");
    }

    private void ensureNegotiationIsRequiredForAgent(
            int agentIndex,
            List<List<Token>> neededTokens,
            List<List<Token>> assignedTokens,
            List<List<Token>> forcedMissingTokens,
            List<String> repairLogs) {
        List<Token> missing = subtractTokens(
            neededTokens.get(agentIndex),
            assignedTokens.get(agentIndex)
        );
        if (!missing.isEmpty() || neededTokens.get(agentIndex).isEmpty()) {
            return;
        }

        Token tokenToMove = chooseMovableNeededToken(
            agentIndex,
            neededTokens.get(agentIndex),
            assignedTokens
        );
        if (tokenToMove == null) return;

        boolean removed = removeOneTokenAndReport(
            assignedTokens.get(agentIndex),
            tokenToMove.getColor()
        );
        if (removed) {
            int receiver = chooseOtherAgent(agentIndex, assignedTokens.size());
            Token movedToken = new Token(tokenToMove.getColor());
            assignedTokens.get(receiver).add(movedToken);
            forcedMissingTokens.get(agentIndex).add(movedToken);
            repairLogs.add("Moved " + tokenToMove.getColor()
                         + " from Player" + (agentIndex + 1)
                         + " to Player" + (receiver + 1)
                         + " so Player" + (agentIndex + 1)
                         + " cannot finish without negotiation.");
        }
    }

    private void rebalanceInventories(List<List<Token>> neededTokens,
                                      List<List<Token>> assignedTokens,
                                      List<String> repairLogs) {
        int total = 0;
        for (List<Token> tokens : assignedTokens) {
            total += tokens.size();
        }

        int maxAllowed = (int) Math.ceil(total / (double) assignedTokens.size())
            + MAX_TOKENS_ABOVE_AVERAGE;

        for (int donor = 0; donor < assignedTokens.size(); donor++) {
            while (assignedTokens.get(donor).size() > maxAllowed) {
                int receiver = findSmallestInventoryIndex(assignedTokens, donor);
                Token moved = removeNonCriticalToken(donor, neededTokens, assignedTokens);
                if (moved == null) {
                    break;
                }
                assignedTokens.get(receiver).add(moved);
                repairLogs.add("Rebalanced " + moved.getColor()
                             + " from Player" + (donor + 1)
                             + " to Player" + (receiver + 1)
                             + " because Player" + (donor + 1)
                             + " had too many tokens.");
            }
        }
    }

    private int chooseOtherAgent(int owner, int playerCount) {
        int receiver;
        do {
            receiver = random.nextInt(playerCount);
        } while (receiver == owner);
        return receiver;
    }

    private boolean allMissingTokensAvailableElsewhere(
            int agentIndex,
            List<Token> missing,
            List<List<Token>> assignedTokens) {
        Map<Token.Color, Integer> missingCounts = countColors(missing);
        for (Map.Entry<Token.Color, Integer> entry : missingCounts.entrySet()) {
            int available = countColorInOtherInventories(
                agentIndex,
                entry.getKey(),
                assignedTokens
            );
            if (available < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private int countColorInOtherInventories(int agentIndex,
                                             Token.Color color,
                                             List<List<Token>> assignedTokens) {
        int count = 0;
        for (int i = 0; i < assignedTokens.size(); i++) {
            if (i == agentIndex) continue;
            count += countColor(assignedTokens.get(i), color);
        }
        return count;
    }

    private int countColor(List<Token> tokens, Token.Color color) {
        int count = 0;
        for (Token token : tokens) {
            if (token.getColor() == color) {
                count++;
            }
        }
        return count;
    }

    private boolean hasUsefulOffer(int agentIndex,
                                   List<List<Token>> neededTokens,
                                   List<List<Token>> assignedTokens) {
        for (int other = 0; other < assignedTokens.size(); other++) {
            if (other == agentIndex) continue;

            List<Token> otherMissing = subtractTokens(
                neededTokens.get(other),
                assignedTokens.get(other)
            );

            for (Token token : assignedTokens.get(agentIndex)) {
                if (containsColor(otherMissing, token.getColor())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Token.Color findUsefulOfferColorForAnotherAgent(
            int agentIndex,
            List<List<Token>> neededTokens,
            List<List<Token>> assignedTokens) {
        for (int other = 0; other < assignedTokens.size(); other++) {
            if (other == agentIndex) continue;

            List<Token> otherMissing = subtractTokens(
                neededTokens.get(other),
                assignedTokens.get(other)
            );

            if (!otherMissing.isEmpty()) {
                return otherMissing.get(random.nextInt(otherMissing.size())).getColor();
            }
        }
        return null;
    }

    private Token.Color randomNonBlockingColor(int agentIndex,
                                               List<List<Token>> neededTokens,
                                               List<List<Token>> assignedTokens) {
        Token.Color[] colors = Token.Color.values();
        List<Token> missing = subtractTokens(
            neededTokens.get(agentIndex),
            assignedTokens.get(agentIndex)
        );

        for (int attempt = 0; attempt < colors.length * 2; attempt++) {
            Token.Color color = colors[random.nextInt(colors.length)];
            if (!containsColor(missing, color)) {
                return color;
            }
        }
        return colors[random.nextInt(colors.length)];
    }

    private Token chooseMovableNeededToken(int agentIndex,
                                           List<Token> needed,
                                           List<List<Token>> assignedTokens) {
        List<Token> candidates = new ArrayList<>();
        for (Token token : needed) {
            if (containsColor(assignedTokens.get(agentIndex), token.getColor())) {
                candidates.add(token);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private int findSmallestInventoryIndex(List<List<Token>> assignedTokens,
                                           int excludedIndex) {
        int smallestIndex = -1;
        int smallestSize = Integer.MAX_VALUE;
        for (int i = 0; i < assignedTokens.size(); i++) {
            if (i == excludedIndex) continue;
            int size = assignedTokens.get(i).size();
            if (size < smallestSize) {
                smallestSize = size;
                smallestIndex = i;
            }
        }
        return smallestIndex == -1 ? chooseOtherAgent(excludedIndex, assignedTokens.size()) : smallestIndex;
    }

    private Token removeNonCriticalToken(int agentIndex,
                                         List<List<Token>> neededTokens,
                                         List<List<Token>> assignedTokens) {
        List<Token> missing = subtractTokens(
            neededTokens.get(agentIndex),
            assignedTokens.get(agentIndex)
        );
        List<Token> tokens = assignedTokens.get(agentIndex);

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (!containsColor(missing, token.getColor())) {
                tokens.remove(i);
                return token;
            }
        }

        if (!tokens.isEmpty()
         && subtractTokens(neededTokens.get(agentIndex), tokens).size() > 1) {
            return tokens.remove(tokens.size() - 1);
        }
        return null;
    }

    private List<Token> subtractTokens(List<Token> required, List<Token> owned) {
        List<Token> missing = copyTokens(required);
        for (Token token : owned) {
            removeOneToken(missing, token.getColor());
        }
        return missing;
    }

    private void removeOneToken(List<Token> tokens, Token.Color color) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).getColor() == color) {
                tokens.remove(i);
                return;
            }
        }
    }

    private boolean removeOneTokenAndReport(List<Token> tokens,
                                            Token.Color color) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).getColor() == color) {
                tokens.remove(i);
                return true;
            }
        }
        return false;
    }

    private List<Token> copyTokens(List<Token> source) {
        List<Token> copy = new ArrayList<>();
        for (Token token : source) {
            copy.add(new Token(token.getColor()));
        }
        return copy;
    }

    private void printDistributionLogs(List<List<Cell>> paths,
                                       List<List<Token>> neededTokens,
                                       List<List<Token>> assignedTokens,
                                       List<List<Token>> forcedMissingTokens,
                                       List<String> repairLogs) {
        System.out.println("\n========== STRATEGIC TOKEN DISTRIBUTION ==========");

        if (repairLogs.isEmpty()) {
            System.out.println("Validation: no repair needed after initial distribution.");
        } else {
            System.out.println("Validation repairs:");
            for (String log : repairLogs) {
                System.out.println("  - " + log);
            }
        }

        for (int i = 0; i < paths.size(); i++) {
            String playerName = "Player" + (i + 1);
            List<Token> missing = subtractTokens(neededTokens.get(i), assignedTokens.get(i));
            boolean canReachWithNegotiation = canAgentReachGoalWithNegotiation(
                i,
                neededTokens,
                assignedTokens
            );

            System.out.println("\n" + playerName);
            System.out.println("Path: " + formatPath(paths.get(i)));
            System.out.println("Needed colors: " + formatCounts(countColors(neededTokens.get(i))));
            System.out.println("Initial tokens: " + assignedTokens.get(i));
            System.out.println("Missing useful tokens: " + formatCounts(countColors(missing)));
            System.out.println("Forced missing token(s): " + forcedMissingTokens.get(i));
            System.out.println("Can reach goal with negotiation: "
                             + (canReachWithNegotiation ? "yes" : "no"));
            System.out.println("Has at least one useful offer: "
                             + (hasUsefulOffer(i, neededTokens, assignedTokens)
                                ? "yes" : "no"));
            System.out.println("Negotiation needed: "
                             + (!missing.isEmpty()
                                ? "yes, own tokens do not cover the full path"
                                : "no"));

            for (Token token : missing) {
                System.out.println("  " + token.getColor()
                                 + " can be requested from "
                                 + findAgentsOwningColor(i, token.getColor(), assignedTokens));
            }
        }

        System.out.println("=================================================\n");
    }

    private String findAgentsOwningColor(int requester,
                                         Token.Color color,
                                         List<List<Token>> assignedTokens) {
        List<String> owners = new ArrayList<>();
        for (int i = 0; i < assignedTokens.size(); i++) {
            if (i == requester) continue;
            if (containsColor(assignedTokens.get(i), color)) {
                owners.add("Player" + (i + 1));
            }
        }
        return owners.isEmpty() ? "no visible owner" : owners.toString();
    }

    private boolean containsColor(List<Token> tokens, Token.Color color) {
        for (Token token : tokens) {
            if (token.getColor() == color) return true;
        }
        return false;
    }

    private Map<Token.Color, Integer> countColors(List<Token> tokens) {
        Map<Token.Color, Integer> counts = new EnumMap<>(Token.Color.class);
        for (Token token : tokens) {
            counts.put(token.getColor(), counts.getOrDefault(token.getColor(), 0) + 1);
        }
        return counts;
    }

    private String formatCounts(Map<Token.Color, Integer> counts) {
        return counts.isEmpty() ? "none" : counts.toString();
    }

    private String formatPath(List<Cell> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            Cell cell = path.get(i);
            sb.append("(")
              .append(cell.getRow())
              .append(",")
              .append(cell.getCol())
              .append(",")
              .append(cell.getColor())
              .append(")");
            if (i < path.size() - 1) {
                sb.append(" -> ");
            }
        }
        return sb.toString();
    }
}
