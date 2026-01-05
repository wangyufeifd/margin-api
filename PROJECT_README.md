# Margin API Project

A comprehensive system for calculating and managing margin requirements for futures trading positions.

## 📁 Project Structure

```
margin-api/                    # Main API module - HTTP API server
├── src/
│   ├── main/java/
│   └── resources/
├── build.gradle
└── README.md

margin-saving/                 # Margin optimization engine
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   │       ├── 组合保证金优惠参数.txt
│   │       └── positions.csv
├── build.gradle
├── README.md
├── HOW_TO_RUN.md
├── POSITION_TEST_CASES.md
└── TEST_RESULTS_SUMMARY.md
```

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Gradle Wrapper included (no separate installation needed)

### Build Everything
```bash
./gradlew clean build
```

### Run Specific Module

#### Margin API (HTTP Server)
```bash
./gradlew :margin-api:run
```
Server starts on `http://localhost:8080`

#### Margin Saving Engine
```bash
./gradlew :margin-saving:run
```
Processes positions and calculates optimal margin requirements.

## 📦 Modules

### 1. margin-api
**Purpose**: Reactive HTTP API server for margin calculations

**Technology Stack**:
- Vert.x 4.5.1 (reactive toolkit)
- Google Guice 7.0.0 (dependency injection)
- Jackson (JSON processing)
- SLF4J + Logback (logging)

**Key Features**:
- Reactive data processing pipeline
- Kafka integration
- EventBus-based architecture
- Trade execution processing
- Position and margin tracking

**Documentation**: See `margin-api/README.md`

---

### 2. margin-saving
**Purpose**: Margin optimization engine for position pairing

**Key Features**:
- Loads 157,478+ combination margin parameters from exchange
- Analyzes client positions across multiple accounts
- Finds optimal position pairings (hedges and spreads)
- Calculates margin requirements for both paired and unpaired positions
- Supports calendar spreads and multi-month strategies
- Account isolation (positions from different accounts don't pair)

**Test Coverage**:
- 6 test accounts with different scenarios
- 34 positions across 10 different products
- Pairing efficiency from 0% to 100%
- Total test margin: 377,191.16

**Documentation**:
- Main: `margin-saving/README.md`
- How to run: `margin-saving/HOW_TO_RUN.md`
- Test cases: `margin-saving/POSITION_TEST_CASES.md`
- Test results: `margin-saving/TEST_RESULTS_SUMMARY.md`

**Sample Output**:
```
Loading combination parameters...
Loaded 157478 combinations
Loading positions...
Loaded 34 positions

Finding pairs...

=== MARGIN CALCULATION RESULTS ===
Total paired combinations: 14
Total contracts paired: 220
Total unpaired positions: 10
Total margin requirement: 377191.16
```

## 🛠️ Common Commands

### Building
```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :margin-api:build
./gradlew :margin-saving:build

# Clean build
./gradlew clean build
```

### Running
```bash
# Run margin-api server
./gradlew :margin-api:run

# Run margin-saving engine
./gradlew :margin-saving:run
```

### Testing
```bash
# Test all modules
./gradlew test

# Test specific module
./gradlew :margin-api:test
./gradlew :margin-saving:test
```

### Creating Distributions
```bash
# Create ZIP distribution for margin-api
./gradlew :margin-api:distZip

# Create ZIP distribution for margin-saving
./gradlew :margin-saving:distZip
```

Distributions are created in `{module}/build/distributions/`

## 📊 Margin Saving Results

The margin-saving engine demonstrates significant margin optimization:

| Metric | Value |
|--------|-------|
| Combinations loaded | 157,478 |
| Positions processed | 34 |
| Accounts | 6 |
| Paired combinations | 14 |
| Contracts paired | 220 |
| Unpaired positions | 10 |
| **Total margin** | **377,191.16** |

### Margin Savings Example (a2601 contract)
- **Unpaired margin**: 8,406 per lot (2× settlement price)
- **Paired margin**: 840.60 per pair
- **Savings**: 7,565.40 per lot (90% reduction!)

### Pairing Efficiency by Account
| Account | Paired Contracts | Unpaired | Efficiency |
|---------|------------------|----------|------------|
| Client A | 16 | 9 | 64% |
| Client B | 40 | 10 | 80% |
| Client C | 21 | 11 | 66% |
| Client D | 0 | 7 | 0% |
| Client E | 26 | 0 | **100%** ✨ |
| Client F | 17 | 6 | 74% |

## 🏗️ Architecture

### margin-api Architecture
```
Kafka → DataLoader → EventBus → Consumer → Processor → FIFO Queue → Aggregator
```

See `ARCHITECTURE.md` for detailed documentation.

### margin-saving Algorithm
1. **Group positions** by account and contract
2. **Sort combinations** by priority (lower = higher priority)
3. **Match positions** to combinations using greedy algorithm
4. **Calculate margins** for paired and unpaired positions
5. **Report results** with full breakdown

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔗 Additional Resources

- Project architecture: `ARCHITECTURE.md`
- Implementation summary: `IMPLEMENTATION_SUMMARY.md`
- Project plan: `PLAN.md`
- Margin-saving test cases: `margin-saving/POSITION_TEST_CASES.md`
- Margin-saving test results: `margin-saving/TEST_RESULTS_SUMMARY.md`

