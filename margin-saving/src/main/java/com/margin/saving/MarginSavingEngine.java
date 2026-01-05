package com.margin.saving;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Margin Saving Engine - Matches client positions with combination margin parameters
 * to find optimal pairing opportunities that reduce margin requirements.
 */
public class MarginSavingEngine {
    
    /**
     * Represents a combination that can provide margin savings
     */
    static class Combination {
        String date;
        String name; // e.g., "a2601,-a2601"
        List<Leg> legs;
        int priority;
        double margin;
        String attribute;
        
        static class Leg {
            String contract;
            boolean isBuy; // true for positive, false for negative (sell)
            double settlementPrice;
        }
        
        @Override
        public String toString() {
            return String.format("Combination{%s, priority=%d, margin=%.2f}", name, priority, margin);
        }
    }
    
    /**
     * Represents a client position
     */
    static class Position {
        String account;
        String contract;
        boolean isBuy; // true for buy, false for sell
        int quantity;
        
        @Override
        public String toString() {
            return String.format("Position{%s, %s, %s, qty=%d}", 
                account, contract, isBuy ? "buy" : "sell", quantity);
        }
    }
    
    /**
     * Represents a matched pair result
     */
    static class PairResult {
        Combination combination;
        List<PositionUsage> positionUsages;
        int pairCount;
        double totalMarginSaving;
        boolean isUnpaired; // true if this is an unpaired standalone position
        
        static class PositionUsage {
            Position position;
            int usedQuantity;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (isUnpaired) {
                sb.append("Unpaired Position (standalone margin)\n");
                for (PositionUsage usage : positionUsages) {
                    sb.append(String.format("  Contract: %s %s\n", 
                        usage.position.contract,
                        usage.position.isBuy ? "buy" : "sell"));
                    sb.append(String.format("  Quantity: %d\n", usage.usedQuantity));
                    double singleMargin = totalMarginSaving / pairCount;
                    sb.append(String.format("  Margin per lot: %.2f\n", singleMargin));
                    sb.append(String.format("  Total margin: %.2f\n", totalMarginSaving));
                }
            } else {
                sb.append(String.format("Pair: %s (priority=%d)\n", combination.name, combination.priority));
                sb.append(String.format("  Pairs matched: %d\n", pairCount));
                sb.append(String.format("  Margin per pair: %.2f\n", combination.margin));
                sb.append(String.format("  Total margin: %.2f\n", pairCount * combination.margin));
                sb.append("  Positions used:\n");
                for (PositionUsage usage : positionUsages) {
                    sb.append(String.format("    - %d x %s %s\n", 
                        usage.usedQuantity,
                        usage.position.contract,
                        usage.position.isBuy ? "buy" : "sell"));
                }
            }
            return sb.toString();
        }
    }
    
    private List<Combination> combinations = new ArrayList<>();
    private List<Position> positions = new ArrayList<>();
    
    /**
     * Load combination parameters from file
     */
    public void loadCombinations(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
        
        for (int i = 3; i < lines.size(); i++) { // Skip header rows
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            
            String[] parts = line.split("\t+");
            if (parts.length < 6) continue;
            
            Combination combo = new Combination();
            combo.date = parts[0].trim();
            combo.name = parts[1].trim();
            String settlementPrices = parts[2].trim();
            combo.priority = Integer.parseInt(parts[3].trim());
            // Remove comma separators from margin value (e.g., "1,460" -> "1460")
            combo.margin = Double.parseDouble(parts[4].trim().replace(",", ""));
            combo.attribute = parts[5].trim();
            
            // Parse legs
            combo.legs = parseLegs(combo.name, settlementPrices);
            
            combinations.add(combo);
        }
        
        System.out.println("Loaded " + combinations.size() + " combinations");
    }
    
    /**
     * Parse combination legs from name and settlement prices
     * e.g., "a2601,-a2601" with "4203,4203"
     */
    private List<Combination.Leg> parseLegs(String name, String settlementPrices) {
        String[] contracts = name.split(",");
        String[] prices = settlementPrices.split(",");
        
        List<Combination.Leg> legs = new ArrayList<>();
        for (int i = 0; i < contracts.length && i < prices.length; i++) {
            Combination.Leg leg = new Combination.Leg();
            String contract = contracts[i].trim();
            
            if (contract.startsWith("-")) {
                leg.isBuy = false;
                leg.contract = contract.substring(1);
            } else {
                leg.isBuy = true;
                leg.contract = contract;
            }
            
            leg.settlementPrice = Double.parseDouble(prices[i].trim());
            legs.add(leg);
        }
        
        return legs;
    }
    
    /**
     * Load positions from CSV file
     */
    public void loadPositions(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
        
        for (int i = 1; i < lines.size(); i++) { // Skip header
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            
            String[] parts = line.split(",");
            if (parts.length < 4) continue;
            
            Position pos = new Position();
            pos.account = parts[0].trim();
            pos.contract = parts[1].trim();
            pos.isBuy = parts[2].trim().equalsIgnoreCase("buy");
            pos.quantity = Integer.parseInt(parts[3].trim());
            
            positions.add(pos);
        }
        
        System.out.println("Loaded " + positions.size() + " positions");
    }
    
    /**
     * Find all possible pairs for the loaded positions
     */
    public List<PairResult> findPairs() {
        List<PairResult> results = new ArrayList<>();
        
        // Group positions by account and contract
        Map<String, Map<String, List<Position>>> accountPositions = positions.stream()
            .collect(Collectors.groupingBy(
                p -> p.account,
                Collectors.groupingBy(p -> p.contract + ":" + p.isBuy)
            ));
        
        // For each account, try to find matching combinations
        for (String account : accountPositions.keySet()) {
            Map<String, List<Position>> positionMap = accountPositions.get(account);
            
            // Create a working copy of position quantities
            Map<String, Integer> availableQuantities = new HashMap<>();
            for (Map.Entry<String, List<Position>> entry : positionMap.entrySet()) {
                int totalQty = entry.getValue().stream()
                    .mapToInt(p -> p.quantity)
                    .sum();
                availableQuantities.put(entry.getKey(), totalQty);
            }
            
            // Try each combination sorted by priority
            List<Combination> sortedCombos = combinations.stream()
                .sorted(Comparator.comparingInt(c -> c.priority))
                .collect(Collectors.toList());
            
            for (Combination combo : sortedCombos) {
                PairResult result = tryMatchCombination(combo, availableQuantities, positionMap);
                if (result != null && result.pairCount > 0) {
                    results.add(result);
                    
                    // Update available quantities
                    for (PairResult.PositionUsage usage : result.positionUsages) {
                        String key = usage.position.contract + ":" + usage.position.isBuy;
                        int remaining = availableQuantities.get(key) - usage.usedQuantity;
                        availableQuantities.put(key, remaining);
                    }
                }
            }
            
            // Add unpaired positions as standalone margin requirements
            for (Map.Entry<String, Integer> entry : availableQuantities.entrySet()) {
                if (entry.getValue() > 0) {
                    String key = entry.getKey();
                    List<Position> posList = positionMap.get(key);
                    if (posList != null && !posList.isEmpty()) {
                        Position pos = posList.get(0);
                        PairResult unpairedResult = createUnpairedResult(pos, entry.getValue());
                        if (unpairedResult != null) {
                            results.add(unpairedResult);
                        }
                    }
                }
            }
        }
        
        return results;
    }
    
    /**
     * Create a result for an unpaired standalone position
     */
    private PairResult createUnpairedResult(Position position, int quantity) {
        // Find the standalone margin for this contract
        final String lookupKey;
        if (position.isBuy) {
            lookupKey = position.contract + ",-" + position.contract;
        } else {
            lookupKey = "-" + position.contract + "," + position.contract;
        }
        
        // Find the first matching combination (should be priority 1)
        Combination standaloneCombo = combinations.stream()
            .filter(c -> c.name.equals(lookupKey))
            .min(Comparator.comparingInt(c -> c.priority))
            .orElse(null);
        
        if (standaloneCombo == null) {
            // If no combination found, estimate margin (this shouldn't happen normally)
            return null;
        }
        
        // Get the settlement price for the position's leg
        // For buy positions, use the first settlement price
        // For sell positions, use the second settlement price
        double settlementPrice = position.isBuy ? 
            standaloneCombo.legs.get(0).settlementPrice : 
            standaloneCombo.legs.get(1).settlementPrice;
        
        // Unpaired position margin = settlement_price * 2 per lot
        double marginPerLot = settlementPrice * 2;
        
        PairResult result = new PairResult();
        result.combination = standaloneCombo;
        result.isUnpaired = true;
        result.pairCount = quantity;
        result.totalMarginSaving = quantity * marginPerLot;
        result.positionUsages = new ArrayList<>();
        
        PairResult.PositionUsage usage = new PairResult.PositionUsage();
        usage.position = position;
        usage.usedQuantity = quantity;
        result.positionUsages.add(usage);
        
        return result;
    }
    
    /**
     * Try to match a combination against available positions
     */
    private PairResult tryMatchCombination(Combination combo, 
                                          Map<String, Integer> availableQuantities,
                                          Map<String, List<Position>> positionMap) {
        // Check if all legs are available
        int minPairs = Integer.MAX_VALUE;
        List<String> legKeys = new ArrayList<>();
        
        for (Combination.Leg leg : combo.legs) {
            String key = leg.contract + ":" + leg.isBuy;
            legKeys.add(key);
            
            Integer available = availableQuantities.get(key);
            if (available == null || available <= 0) {
                return null; // Cannot match this combination
            }
            
            minPairs = Math.min(minPairs, available);
        }
        
        if (minPairs <= 0) return null;
        
        // Create result
        PairResult result = new PairResult();
        result.combination = combo;
        result.pairCount = minPairs;
        result.totalMarginSaving = minPairs * combo.margin;
        result.positionUsages = new ArrayList<>();
        
        for (String key : legKeys) {
            List<Position> positions = positionMap.get(key);
            if (positions != null && !positions.isEmpty()) {
                PairResult.PositionUsage usage = new PairResult.PositionUsage();
                usage.position = positions.get(0); // Take the first position for display
                usage.usedQuantity = minPairs;
                result.positionUsages.add(usage);
            }
        }
        
        return result;
    }
    
    /**
     * Print pair results in a formatted way
     */
    public void printResults(List<PairResult> results) {
        if (results.isEmpty()) {
            System.out.println("\nNo pairs found!");
            return;
        }
        
        System.out.println("\n=== MARGIN CALCULATION RESULTS ===\n");
        
        double totalMargin = 0;
        int pairedCount = 0;
        int unpairedCount = 0;
        int pairCombinations = 0;
        
        for (int i = 0; i < results.size(); i++) {
            PairResult result = results.get(i);
            if (result.isUnpaired) {
                System.out.println("Position #" + (i + 1) + " (Unpaired)");
                unpairedCount++;
            } else {
                System.out.println("Pair #" + (i + 1));
                pairCombinations++;
                pairedCount += result.pairCount;
            }
            System.out.println(result);
            totalMargin += result.totalMarginSaving;
        }
        
        System.out.println("=====================================");
        System.out.printf("Total paired combinations: %d\n", pairCombinations);
        System.out.printf("Total contracts paired: %d\n", pairedCount * 2); // Each pair has 2 legs
        System.out.printf("Total unpaired positions: %d\n", unpairedCount);
        System.out.printf("Total margin requirement: %.2f\n", totalMargin);
    }
    
    public static void main(String[] args) {
        try {
            MarginSavingEngine engine = new MarginSavingEngine();
            
            // Determine base path based on working directory
            String basePath;
            String currentDir = System.getProperty("user.dir");
            if (currentDir.endsWith("margin-saving")) {
                // Running from module directory
                basePath = "src/main/resources/";
            } else if (new java.io.File("margin-saving/src/main/resources").exists()) {
                // Running from project root
                basePath = "margin-saving/src/main/resources/";
            } else {
                throw new RuntimeException("Cannot find resources directory. Current dir: " + currentDir);
            }
            
            System.out.println("Loading combination parameters...");
            engine.loadCombinations(basePath + "组合保证金优惠参数.txt");
            
            System.out.println("Loading positions...");
            engine.loadPositions(basePath + "positions.csv");
            
            System.out.println("\nFinding pairs...");
            List<PairResult> results = engine.findPairs();
            
            engine.printResults(results);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

