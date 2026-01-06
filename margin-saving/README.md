# Margin Saving Engine

This module analyzes client positions and finds optimal pairing opportunities based on exchange combination margin parameters to reduce margin requirements.

## Overview

The Margin Saving Engine reads:
1. **Combination Parameters** (`组合保证金优惠参数.txt`) - Contains all possible combinations from the exchange that provide margin savings
2. **Client Positions** (`positions.csv`) - Contains actual client positions that need margin calculation

It then matches positions to combinations and outputs:
- Which positions can be paired together
- How much margin is required for each pair
- What positions remain unpaired

## File Formats

### Combination Parameters File

Tab-separated file with the following columns:
- Date (YYYYMMDD)
- Combination name (e.g., `a2601,-a2601` means buy a2601 pairs with sell a2601)
- Settlement prices (comma-separated)
- Priority (lower number = higher priority, so priority 1 is highest)
- Margin per combination
- Hedge attribute (e.g., 投机-投机, 套保-套保, 套保-投机, 投机-套保)

**Unique Key**: Combination name + Hedge attribute (same combination name can have different priorities for different hedge attributes)

Example:
```
20260106    a2601,-a2601    4203,4203    1    840.6    投机-投机
```

### Positions CSV File

CSV file with the following columns:
- account: Account identifier
- contract: Contract code (e.g., a2601)
- buy/sell: Position direction
- quantity: Number of contracts

Example:
```csv
account,contract,buy/sell,quantity
g1,a2601,buy,10
g1,a2601,sell,5
g1,a2603,sell,3
```

## Usage

### From Command Line

```bash
# Compile
./gradlew :modules:margin-saving:compileJava

# Run
java -cp "modules/margin-saving/build/classes/java/main" com.margin.saving.MarginSavingEngine
```

### From Code

```java
MarginSavingEngine engine = new MarginSavingEngine();

// Load data
engine.loadCombinations("path/to/组合保证金优惠参数.txt");
engine.loadPositions("path/to/positions.csv");

// Find pairs
List<PairResult> results = engine.findPairs();

// Display results
engine.printResults(results);
```

## Example Output

```
=== MARGIN SAVING PAIR RESULTS ===

Pair #1
Pair: a2601,-a2601 (priority=143075)
  Pairs matched: 5
  Margin per pair: 840.60
  Total margin: 4203.00
  Positions used:
    - 5 x a2601 buy
    - 5 x a2601 sell

Pair #2
Pair: a2601,-a2603 (priority=143323)
  Pairs matched: 3
  Margin per pair: 3362.40
  Total margin: 10087.20
  Positions used:
    - 3 x a2601 buy
    - 3 x a2603 sell

=====================================
Total combinations found: 2
Total contracts paired: 16
Total margin requirement: 14290.20

=== UNPAIRED POSITIONS ===

  g1: 2 x a2601 buy
```

## Algorithm

The engine uses a greedy algorithm to find pairs:

1. **Group positions** by account and contract
2. **Sort combinations** by priority (lower number = higher priority)
3. **For each combination**, check if all required legs are available
4. **Match positions** to combinations, using the maximum number of pairs possible
5. **Update available quantities** after each match
6. **Continue** until no more matches can be made

## Key Features

- ✅ Handles multi-leg combinations (spreads, butterflies, etc.)
- ✅ Respects combination priority for optimal pairing
- ✅ Tracks remaining unpaired positions
- ✅ Calculates total margin requirements
- ✅ Supports Chinese file names and content
- ✅ Handles comma-separated numbers (e.g., "1,460")
- ✅ Processes multiple accounts with account isolation
- ✅ Loads 157,478+ combination parameters efficiently
- ✅ Supports calendar spreads and multi-month strategies

## Notes

- The engine processes positions **per account** (positions from different accounts cannot be paired together)
- Priority determines which combinations are tried first (lower priority number = higher priority, so priority 1 is tried first)
- Once a position is used in a pair, it cannot be reused in another pair
- Unpaired positions will require standard margin calculated as: `settlement_price × 2` per lot
- **Combination uniqueness**: Each combination name (e.g., `a2601,-a2601`) appears multiple times with different hedge attributes (投机-投机, 套保-套保, 套保-投机, 投机-套保). Each combination name + hedge attribute pair is unique and has its own priority.

## Test Cases and Documentation

For comprehensive test coverage and examples, see:
- **[TEST_README.md](TEST_README.md)** - **Automated test suite** (12 JUnit tests, 100% passing)
- **[POSITION_TEST_CASES.md](POSITION_TEST_CASES.md)** - Detailed explanation of each test scenario
- **[TEST_RESULTS_SUMMARY.md](TEST_RESULTS_SUMMARY.md)** - Complete test results with analysis

The test suite includes:
- **12 automated JUnit tests** validating all functionality
- 6 different client accounts
- 34 positions across 10 different products (a, b, c, i, j, jd, jm, l, eb, fb)
- Perfect hedges, partial hedges, calendar spreads, and unpaired positions
- Account isolation verification
- Margin efficiency comparisons (0% to 100% pairing efficiency)
- Total test margin requirement: 377,191.16

### Running Tests
```bash
./gradlew :margin-saving:test
```

