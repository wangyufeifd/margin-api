# Margin API - Data Pipeline Architecture

## Overview

This application implements a reactive data processing pipeline for trade executions:

```
Kafka → DataLoader → EventBus → Consumer → Processor → FIFO Queue → Aggregator (Output Cache)
```

## Pipeline Components

### 1. **KafkaDataLoader** (`loader/`)
- Loads `TradeExecutionWrapper` messages from Kafka topics
- Deserializes the wrapper and extracts the `Execution` model
- Publishes executions to Vert.x EventBus

### 2. **EventBus** (Vert.x)
- Message passing infrastructure
- Address: `execution.incoming`
- Decouples data loading from processing

### 3. **ExecutionConsumer** (`consumer/`)
- Subscribes to EventBus `execution.incoming` address
- Receives execution messages
- Distributes executions to all registered processors

### 4. **Processors** (`processor/`)
Multiple processors can transform executions:

- **MarginProcessor**: Transforms executions into margin requirements
  - Calculates initial margin (50% of notional)
  - Calculates maintenance margin (25% of notional)
  - Saves to FIFO queue cache
  
- **PositionProcessor**: Transforms executions into positions
  - Determines position side (LONG/SHORT)
  - Calculates position details
  - Saves to FIFO queue cache

### 5. **FIFO Queue** (`cache/`)
- Thread-safe concurrent queue implementation
- Acts as cache for each processor
- Configurable max size (default: 1000)
- Auto-evicts oldest items when full

### 6. **Aggregators** (`aggregator/`)
- Poll FIFO queues at regular intervals (default: 1000ms)
- Aggregate data to final output cache

- **MarginAggregator**: Aggregates margin by account + symbol
  - Total initial margin
  - Total maintenance margin
  - Total margin requirement
  
- **PositionAggregator**: Aggregates positions by account + symbol
  - Net quantity
  - Average price
  - P&L tracking

## Data Models (`model/`)

- **TradeExecutionWrapper**: Kafka message wrapper
- **Execution**: Core trade execution data
- **Margin**: Margin requirement calculation
- **Position**: Position tracking data
- **AggregatedMargin**: Aggregated margin output
- **AggregatedPosition**: Aggregated position output

## Configuration

See `application.properties`:

```properties
# Kafka
kafka.bootstrap.servers=localhost:9092
kafka.topics=trade-executions

# Queue Sizes
margin.queue.size=1000
position.queue.size=1000

# Aggregation Interval
aggregator.poll.interval=1000
```

## Architecture Benefits

1. **Decoupled**: Each component is independent
2. **Scalable**: Can add more processors without changing pipeline
3. **Reactive**: Non-blocking async processing with Vert.x
4. **Fault-tolerant**: Failed processing doesn't break the pipeline
5. **Observable**: Each stage can be monitored independently

