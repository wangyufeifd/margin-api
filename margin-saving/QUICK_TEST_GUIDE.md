# Quick Test Guide

## ⚡ Quick Start

```bash
# Run all tests
./gradlew :margin-saving:test

# View HTML report
open margin-saving/build/reports/tests/test/index.html
```

## 📊 Test Results at a Glance

```
✅ 12/12 Tests Passed
⏱️  3.8 seconds
📁 Test file: MarginSavingEngineTest.java
📄 Expected output: expectedOutput.json
```

## 🧪 What Gets Tested

| # | Test Name | What It Validates |
|---|-----------|-------------------|
| 1 | Load combinations | 157,478 combinations loaded ✅ |
| 2 | Load positions | 34 positions loaded ✅ |
| 3 | Find pairs | 14 paired + 10 unpaired ✅ |
| 4 | Total margin | 377,191.16 calculated ✅ |
| 5 | a2601 pairing | Same-month hedge @ 840.60 ✅ |
| 6 | Unpaired margin | settlement × 2 = 8,406 ✅ |
| 7 | Account separation | No cross-account pairing ✅ |
| 8 | Priority order | Lower number = higher priority ✅ |
| 9 | Client E (100%) | All positions paired ✅ |
| 10 | Client D (0%) | No positions paired ✅ |
| 11 | Output format | Correct format & content ✅ |
| 12 | Calendar spread | a2601,-a2603 @ 3,362.40 ✅ |

## 🎯 Key Assertions

```java
// Total combinations loaded
assertEquals(157478, combinations.size());

// Total positions loaded
assertEquals(34, positions.size());

// Total margin calculated
assertEquals(377191.16, totalMargin, 0.01);

// Unpaired margin formula
assertEquals(settlement * 2, marginPerLot, 0.01);
```

## 📈 Expected Results

| Metric | Value |
|--------|-------|
| Combinations | 157,478 |
| Positions | 34 |
| Accounts | 6 |
| Paired Combos | 14 |
| Unpaired Pos | 10 |
| **Total Margin** | **377,191.16** |

## 🚀 Common Commands

```bash
# Run tests
./gradlew :margin-saving:test

# Clean and test
./gradlew :margin-saving:clean :margin-saving:test

# Run specific test
./gradlew :margin-saving:test --tests "MarginSavingEngineTest.Should calculate correct total margin"

# Verbose output
./gradlew :margin-saving:test --info

# Generate report
./gradlew :margin-saving:test
open margin-saving/build/reports/tests/test/index.html
```

## 📁 Test Files

```
src/test/
├── java/com/margin/saving/
│   └── MarginSavingEngineTest.java    ← 12 test methods
└── resources/
    └── expectedOutput.json             ← Expected results
```

## 🔍 Debugging Failed Tests

If a test fails:

1. **Check input data**: Did positions.csv or combinations txt change?
2. **Check expected values**: Are test assertions still valid?
3. **Run with --info**: See detailed output
4. **Read error message**: It tells you what's wrong!

```bash
# Verbose test run
./gradlew :margin-saving:test --info
```

## 📚 Documentation Links

- [TEST_README.md](TEST_README.md) - Full test documentation
- [TEST_SUITE_SUMMARY.md](TEST_SUITE_SUMMARY.md) - Overview
- [expectedOutput.json](src/test/resources/expectedOutput.json) - Expected results

---

**Status**: ✅ All Tests Passing  
**Last Run**: Check with `./gradlew :margin-saving:test`

