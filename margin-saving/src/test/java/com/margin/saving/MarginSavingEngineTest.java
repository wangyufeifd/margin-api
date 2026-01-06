package com.margin.saving;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for MarginSavingEngine
 * Validates position pairing and margin calculation logic
 */
public class MarginSavingEngineTest {
    
    private MarginSavingEngine engine;
    private String basePath;
    
    @BeforeEach
    public void setUp() {
        engine = new MarginSavingEngine();
        
        // Determine base path for test resources
        String currentDir = System.getProperty("user.dir");
        if (currentDir.endsWith("margin-saving")) {
            basePath = "src/main/resources/";
        } else {
            basePath = "margin-saving/src/main/resources/";
        }
    }
    
    @Test
    @DisplayName("Should load combination parameters successfully")
    public void testLoadCombinations() throws Exception {
        engine.loadCombinations(basePath + "组合保证金优惠参数.txt");
        
        // Verify combinations were loaded
        List<MarginSavingEngine.Combination> combinations = engine.getCombinations();
        assertNotNull(combinations, "Combinations should not be null");
        assertEquals(157478, combinations.size(), "Should load exactly 157,478 combinations");
    }
    
    @Test
    @DisplayName("Should load positions successfully")
    public void testLoadPositions() throws Exception {
        engine.loadPositions(basePath + "positions.csv");
        
        // Verify positions were loaded
        List<MarginSavingEngine.Position> positions = engine.getPositions();
        assertNotNull(positions, "Positions should not be null");
        assertEquals(34, positions.size(), "Should load exactly 34 positions");
        
        // Verify first position
        MarginSavingEngine.Position firstPos = positions.get(0);
        assertEquals("client_A", firstPos.account);
        assertEquals("a2601", firstPos.contract);
        assertTrue(firstPos.isBuy);
        assertEquals(10, firstPos.quantity);
    }
    
    @Test
    @DisplayName("Should find correct number of pairs")
    public void testFindPairs() throws Exception {
        // Load data
        engine.loadCombinations(basePath + "组合保证金优惠参数.txt");
        engine.loadPositions(basePath + "positions.csv");
        
        // Find pairs
        List<MarginSavingEngine.PairResult> results = engine.findPairs();
        
        // Verify results
        assertNotNull(results, "Results should not be null");
        assertEquals(24, results.size(), "Should find 24 total results (14 paired + 10 unpaired)");
        
        // Count paired vs unpaired
        long pairedCount = results.stream().filter(r -> !r.isUnpaired).count();
        long unpairedCount = results.stream().filter(r -> r.isUnpaired).count();
        
        assertEquals(14, pairedCount, "Should have 14 paired combinations");
        assertEquals(10, unpairedCount, "Should have 10 unpaired positions");
    }
    
    @Test
    @DisplayName("Should calculate correct total margin")
    public void testTotalMargin() throws Exception {
        // Load data
        engine.loadCombinations(basePath + "组合保证金优惠参数.txt");
        engine.loadPositions(basePath + "positions.csv");
        
        // Find pairs
        List<MarginSavingEngine.PairResult> results = engine.findPairs();
        
        // Calculate total margin
        double totalMargin = results.stream()
            .mapToDouble(r -> r.totalMarginSaving)
            .sum();
        
        // Verify total margin (with small delta for floating point comparison)
        assertEquals(377191.16, totalMargin, 0.01, 
            "Total margin should be 377,191.16");
    }
    
    @Test
    @DisplayName("Should pair a2601 same-month hedge correctly")
    public void testA2601SameMonthPair() throws Exception {
        // Load data
        engine.loadCombinations(basePath + "组合保证金优惠参数.txt");
        engine.loadPositions(basePath + "positions.csv");
        
        // Find pairs
        List<MarginSavingEngine.PairResult> results = engine.findPairs();
        
        // Find a2601,-a2601 pairs
        List<MarginSavingEngine.PairResult> a2601Pairs = results.stream()
            .filter(r -> !r.isUnpaired && r.combination.name.equals("a2601,-a2601"))
            .toList();
        
        // Should have pairs from multiple accounts
        assertFalse(a2601Pairs.isEmpty(), "Should have a2601,-a2601 pairs");
        
        // Verify margin per pair
        for (MarginSavingEngine.PairResult pair : a2601Pairs) {
            assertEquals(840.60, pair.combination.margin, 0.01,
                "a2601,-a2601 margin should be 840.60 per pair");
            assertEquals(1, pair.combination.priority,
                "a2601,-a2601 should have priority 1 (highest)");
        }
    }
    
    @Test
    @DisplayName("Should calculate unpaired position margin correctly")
    public void testUnpairedMargin() throws Exception {
        // Load data
        engine.loadCombinations(basePath + "组合保证金优惠参数.txt");
        engine.loadPositions(basePath + "positions.csv");
        
        // Find pairs
        List<MarginSavingEngine.PairResult> results = engine.findPairs();
        
        // Find unpaired a2601 buy positions
        List<MarginSavingEngine.PairResult> unpairedA2601 = results.stream()
            .filter(r -> r.isUnpaired && 
                        r.positionUsages.get(0).position.contract.equals("a2601") &&
                        r.positionUsages.get(0).position.isBuy)
            .toList();
        
        assertFalse(unpairedA2601.isEmpty(), "Should have unpaired a2601 positions");
        
        // Verify unpaired margin = settlement_price * 2
        for (MarginSavingEngine.PairResult unpaired : unpairedA2601) {
            double expectedMarginPerLot = 4203.0 * 2; // settlement price * 2
            double actualMarginPerLot = unpaired.totalMarginSaving / unpaired.pairCount;
            
            assertEquals(expectedMarginPerLot, actualMarginPerLot, 0.01,
                "Unpaired a2601 margin should be 4203 * 2 = 8406 per lot");
        }
    }
    
    @Test
    @DisplayName("Should maintain account separation")
    public void testAccountSeparation() throws Exception {
        // Load data
        engine.loadCombinations(basePath + "组合保证金优惠参数.txt");
        engine.loadPositions(basePath + "positions.csv");
        
        // Find pairs
        List<MarginSavingEngine.PairResult> results = engine.findPairs();
        
        // Verify each pair result uses positions from only one account
        for (MarginSavingEngine.PairResult result : results) {
            if (!result.isUnpaired && result.positionUsages.size() > 1) {
                String firstAccount = result.positionUsages.get(0).position.account;
                
                for (MarginSavingEngine.PairResult.PositionUsage usage : result.positionUsages) {
                    assertEquals(firstAccount, usage.position.account,
                        "All positions in a pair should be from the same account");
                }
            }
        }
    }
    
    @Test
    @DisplayName("Should respect priority order")
    public void testPriorityOrder() throws Exception {
        // Load data
        engine.loadCombinations(basePath + "组合保证金优惠参数.txt");
        engine.loadPositions(basePath + "positions.csv");
        
        // Find pairs
        List<MarginSavingEngine.PairResult> results = engine.findPairs();
        
        // Get paired results only
        List<MarginSavingEngine.PairResult> pairedResults = results.stream()
            .filter(r -> !r.isUnpaired)
            .toList();
        
        // Same-month hedges should have lower priority numbers than spreads
        for (MarginSavingEngine.PairResult result : pairedResults) {
            String comboName = result.combination.name;
            
            // Same contract hedge (e.g., a2601,-a2601)
            if (comboName.matches("\\w+\\d+,-\\w+\\d+") && 
                comboName.split(",")[0].replace("-", "").equals(
                comboName.split(",")[1].replace("-", ""))) {
                
                assertTrue(result.combination.priority < 250,
                    "Same-month hedges should have priority < 250");
            }
        }
    }
    
    @Test
    @DisplayName("Should handle Client E with 100% pairing efficiency")
    public void testClientEFullyPaired() throws Exception {
        // Load data
        engine.loadCombinations(basePath + "组合保证金优惠参数.txt");
        engine.loadPositions(basePath + "positions.csv");
        
        // Find pairs
        List<MarginSavingEngine.PairResult> results = engine.findPairs();
        
        // Client E should have no unpaired positions
        boolean hasClientEUnpaired = results.stream()
            .filter(r -> r.isUnpaired)
            .anyMatch(r -> r.positionUsages.get(0).position.account.equals("client_E"));
        
        assertFalse(hasClientEUnpaired, 
            "Client E should have 100% pairing efficiency (no unpaired positions)");
    }
    
    @Test
    @DisplayName("Should handle Client D with 0% pairing efficiency")
    public void testClientDNoPairs() throws Exception {
        // Load data
        engine.loadCombinations(basePath + "组合保证金优惠参数.txt");
        engine.loadPositions(basePath + "positions.csv");
        
        // Find pairs
        List<MarginSavingEngine.PairResult> results = engine.findPairs();
        
        // Client D should have no paired positions
        boolean hasClientDPaired = results.stream()
            .filter(r -> !r.isUnpaired)
            .anyMatch(r -> r.positionUsages.stream()
                .anyMatch(u -> u.position.account.equals("client_D")));
        
        assertFalse(hasClientDPaired, 
            "Client D should have 0% pairing efficiency (all unpaired)");
    }
    
    @Test
    @DisplayName("Should produce expected output format")
    public void testOutputFormat() throws Exception {
        // Capture output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        
        try {
            // Load and process
            engine.loadCombinations(basePath + "组合保证金优惠参数.txt");
            engine.loadPositions(basePath + "positions.csv");
            List<MarginSavingEngine.PairResult> results = engine.findPairs();
            engine.printResults(results);
            
            // Get output
            String output = outContent.toString();
            
            // Verify output contains expected sections
            assertTrue(output.contains("MARGIN CALCULATION RESULTS"),
                "Output should contain results header");
            assertTrue(output.contains("Total paired combinations:"),
                "Output should contain paired combinations count");
            assertTrue(output.contains("Total contracts paired:"),
                "Output should contain contracts paired count");
            assertTrue(output.contains("Total unpaired positions:"),
                "Output should contain unpaired positions count");
            assertTrue(output.contains("Total margin requirement:"),
                "Output should contain total margin");
            assertTrue(output.contains("377191.16") || output.contains("377,191.16"),
                "Output should contain correct total margin");
            
        } finally {
            System.setOut(originalOut);
        }
    }
    
    @Test
    @DisplayName("Should calculate calendar spread margins correctly")
    public void testCalendarSpread() throws Exception {
        // Load data
        engine.loadCombinations(basePath + "组合保证金优惠参数.txt");
        engine.loadPositions(basePath + "positions.csv");
        
        // Find pairs
        List<MarginSavingEngine.PairResult> results = engine.findPairs();
        
        // Find a2601,-a2603 calendar spread
        MarginSavingEngine.PairResult spreadResult = results.stream()
            .filter(r -> !r.isUnpaired && r.combination.name.equals("a2601,-a2603"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(spreadResult, "Should find a2601,-a2603 calendar spread");
        assertEquals(3362.40, spreadResult.combination.margin, 0.01,
            "Calendar spread margin should be 3362.40 per pair");
        assertTrue(spreadResult.combination.priority > 1,
            "Calendar spread should have lower priority than same-month hedge");
    }
}

