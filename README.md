# margin-api

A reactive Java API project built with Vert.x, Google Guice, and Gradle.

## Project Structure

```
margin-api/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/margin/api/
│   │   │       ├── model/
│   │   │       │   ├── BaseEntity.java
│   │   │       │   ├── User.java
│   │   │       │   └── Account.java
│   │   │       ├── Application.java
│   │   │       ├── ApplicationModule.java
│   │   │       └── MainVerticle.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── logback.xml
│   └── test/
│       ├── java/
│       │   └── com/margin/api/
│       │       ├── model/
│       │       │   ├── UserTest.java
│       │       │   └── AccountTest.java
│       │       └── ApplicationTest.java
│       └── resources/
│           └── logback-test.xml
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
└── README.md
```

## Technology Stack

- **Vert.x 4.5.1** - Reactive toolkit for building event-driven applications
- **Google Guice 7.0.0** - Lightweight dependency injection framework
- **Jackson** - JSON serialization/deserialization
- **SLF4J + Logback** - Logging
- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework

## Prerequisites

- Java 17 or higher
- No need to install Gradle separately - the project includes Gradle Wrapper

## Building the Project

### On Windows:
```bash
gradlew.bat build
```

### On Linux/Mac:
```bash
./gradlew build
```

## Running the Application

### On Windows:
```bash
gradlew.bat run
```

### On Linux/Mac:
```bash
./gradlew run
```

The application will start an HTTP server on `http://localhost:8080`

## API Endpoints

- `GET /` - Welcome message with API version
- `GET /health` - Health check endpoint

## Running Tests

### On Windows:
```bash
gradlew.bat test
```

### On Linux/Mac:
```bash
./gradlew test
```

## Common Gradle Tasks

- `gradlew clean` - Clean build artifacts
- `gradlew build` - Build the project
- `gradlew test` - Run tests
- `gradlew run` - Run the application
- `gradlew tasks` - List all available tasks
- `gradlew dependencies` - Show project dependencies

## Architecture

This application implements a reactive data processing pipeline:

```
Kafka → DataLoader → EventBus → Consumer → Processor → FIFO Queue → Aggregator
```

### Pipeline Components:

1. **KafkaDataLoader** - Loads trade executions from Kafka and publishes to EventBus
2. **ExecutionConsumer** - Consumes executions from EventBus and distributes to processors
3. **Processors** - Transform executions (MarginProcessor, PositionProcessor)
4. **FIFO Queue** - Thread-safe cache for each processor
5. **Aggregators** - Poll queues and aggregate data to output cache

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed documentation.

## Model Packages

### `model` package - Domain entities:

- **BaseEntity** - Abstract base class with common fields (id, createdAt, updatedAt)
- **User** - User entity with username, email, and status
- **Account** - Trading account entity with balance, margin tracking, and status
- **Execution** - Trade execution data
- **Margin** - Margin requirement calculation
- **Position** - Position tracking
- **TradeExecutionWrapper** - Kafka message wrapper

### Other packages:

- `loader` - Kafka data loading
- `consumer` - EventBus consumers
- `processor` - Execution processors
- `aggregator` - Data aggregators
- `cache` - FIFO queue implementation

## Development

### Project Architecture

The application follows a reactive architecture pattern:

1. **Application** - Entry point that initializes Guice and Vert.x
2. **ApplicationModule** - Guice module for dependency injection configuration
3. **MainVerticle** - Main Vert.x verticle that sets up HTTP server and routes
4. **Model Package** - Domain entities with immutable design

### Adding New Routes

Edit `MainVerticle.java` and add your routes in the `setupRoutes()` method:

```java
router.get("/api/users").handler(ctx -> {
    // Your handler code
});
```

### Adding New Dependencies

Configure bindings in `ApplicationModule.java`:

```java
@Override
protected void configure() {
    bind(YourService.class).to(YourServiceImpl.class);
}
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
