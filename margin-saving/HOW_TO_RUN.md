# How to Run the Margin Saving Engine

## ✅ Recommended: Using Gradle (Works from any IDE)

### From Command Line
```bash
cd /Users/yufei/dev/code/margin-api
./gradlew :margin-saving:run
```

### From IntelliJ IDEA or Cursor IDE
1. Open the Gradle panel (usually on the right side)
2. Navigate to: `margin-api-project` → `margin-saving` → `Tasks` → `application` → `run`
3. Double-click on `run`

OR

1. Right-click on `margin-saving/build.gradle`
2. Select "Run 'margin-saving [run]'"

## Configuration Details

### Main Class
The main class is configured in `build.gradle`:
```gradle
application {
    mainClass = 'com.margin.saving.MarginSavingEngine'
}
```

### Source Sets
Source directories are explicitly configured:
```gradle
sourceSets {
    main {
        java {
            srcDirs = ['src/main/java']
        }
        resources {
            srcDirs = ['src/main/resources']
        }
    }
}
```

### Java Version
```gradle
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
```

## Running Directly (Alternative)

If you prefer to run the Java class directly:

```bash
cd /Users/yufei/dev/code/margin-api
./gradlew :margin-saving:compileJava
java -cp "margin-saving/build/classes/java/main" com.margin.saving.MarginSavingEngine
```

## Troubleshooting

### Issue: "Cannot add main class to path"
**Solution**: Use Gradle's `run` task instead of trying to run the class directly from the IDE.

### Issue: "NoSuchFileException" when loading resources
**Solution**: The code now automatically detects the working directory and adjusts paths accordingly. Make sure you're running from either:
- Project root directory (`/Users/yufei/dev/code/margin-api`)
- Module directory (`/Users/yufei/dev/code/margin-api/margin-saving`)

### Issue: IDE doesn't recognize source directories
**Solution**: 
1. Refresh Gradle project: Right-click on project → Gradle → Refresh Gradle Project
2. Rebuild: `./gradlew :margin-saving:clean :margin-saving:build`

## Building Distribution

To create a runnable distribution:

```bash
./gradlew :margin-saving:distZip
```

This creates a ZIP file in `margin-saving/build/distributions/` with:
- All JAR files and dependencies
- Startup scripts for Windows and Unix/Mac
- Ready to deploy to any environment

### Running the Distribution

After extracting the ZIP:
```bash
cd margin-saving-1.0.0-SNAPSHOT
./bin/margin-saving
```

## IDE Setup Notes

### IntelliJ IDEA
- The IDE should automatically detect the Gradle configuration
- If not, go to: File → Project Structure → Modules
- Make sure `margin-saving` module has correct source roots:
  - `src/main/java` (Sources)
  - `src/main/resources` (Resources)

### VS Code / Cursor
- Install "Extension Pack for Java" if not already installed
- The IDE should auto-detect Gradle projects
- Use the Gradle panel to run tasks
- Or use the built-in terminal to run Gradle commands

## Quick Commands Summary

| Task | Command |
|------|---------|
| Run | `./gradlew :margin-saving:run` |
| Compile | `./gradlew :margin-saving:compileJava` |
| Build | `./gradlew :margin-saving:build` |
| Clean | `./gradlew :margin-saving:clean` |
| Distribution | `./gradlew :margin-saving:distZip` |
| Tests | `./gradlew :margin-saving:test` |

## Expected Output

When running successfully, you should see:
```
Loading combination parameters...
Loaded 157478 combinations
Loading positions...
Loaded 34 positions

Finding pairs...

=== MARGIN CALCULATION RESULTS ===
...
Total paired combinations: 14
Total contracts paired: 220
Total unpaired positions: 10
Total margin requirement: 377191.16
```

