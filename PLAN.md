# Final Recommended Architecture Detailed Specifications

# 1. Executive Summary

This document defines the final recommended architecture for the real-time trade execution processing system. The architecture is designed to address critical issues such as high latency, low throughput, poor reliability, and insufficient scalability in the current system, while meeting business requirements for real-time margin calculation, position tracking, data replay, and reference data enrichment. It is a streamlined, reactive architecture that balances performance, reliability, maintainability, and extensibility, serving as the definitive technical blueprint for system implementation.

# 2. Architecture Overview

The final architecture adopts a streamlined reactive pipeline with clear separation of concerns and efficient data flow. It eliminates redundant intermediate layers, implements parallel processing, and integrates persistent caching and backpressure mechanisms to ensure high performance and reliability.

## 2.1 Core Architecture Diagram

Kafka → DataLoader (Deserialization & Persistent Caching) → ProcessorRegistry (Plug-in Routing) → Parallel Processor Pool (Margin/Position Calculation) → Push-Based Aggregator (Real-Time Aggregation) → Bounded Cache (Low-Latency Data Access)

## 2.2 Key Design Principles

- Minimize Latency: Remove redundant layers and adopt push-based processing to achieve end-to-end latency < 10ms (p99).

- Maximize Throughput: Implement parallel processing via worker pools and Kafka consumer groups to support > 10,000 msg/s.

- Ensure Reliability: Integrate persistent caching, backpressure, and error handling (retry + DLQ) to achieve 0% data loss.

- Support Extensibility: Adopt plug-in architecture for processors to enable seamless addition of new business logic.

- Enable Audibility: Implement comprehensive observability and data replay capabilities for operational governance.

# 3. Component Responsibilities and Technical Implementation

The architecture consists of six core components, each with well-defined responsibilities and technical specifications. All components are designed to work in coordination to ensure efficient and reliable data processing.

## 3.1 Kafka Layer (Data Input Source)

Serves as the entry point for real-time trade execution data, providing high-throughput and reliable message delivery.

- Topic Configuration: Use a multi-partition topic (trade-executions) to support parallel consumption. The number of partitions should be scaled based on expected throughput (recommended 1 partition per 1,000 msg/s).

- Consumer Group Strategy: Adopt Kafka consumer groups to enable horizontal scaling of DataLoader instances. Each consumer instance processes 1–multiple partitions to avoid data duplication.

- Offset Management: Support both automatic and manual offset submission. Manual submission is used for critical processing scenarios to ensure "at-least-once" semantics.

- Message Retention: Configure appropriate message retention policies (e.g., 7 days) to support data replay from historical time points.

## 3.2 DataLoader Layer (Deserialization & Persistent Caching)

Core responsibilities: Receive data from Kafka, perform deserialization and validation, and persist data to RocksDB for crash recovery. It acts as the bridge between Kafka and the downstream processing pipeline.

- Deserialization: Convert binary Kafka messages into strongly-typed Execution entities (fields: account, symbol, price, quantity, side, timestamp, etc.). Use Avro or Protobuf for schema evolution support.

- Data Validation: Perform integrity checks (required fields, data type validity) and business rule validation (e.g., valid symbol, non-negative quantity). Invalid messages are routed to the Dead Letter Queue (DLQ) with detailed error logs.

- RocksDB Integration (Persistent Caching):
        

    - Data Storage: Persist all valid Execution entities to RocksDB using a composite key: `KafkaTopic + Partition + Offset`. The value is the serialized Execution entity (Avro/Protobuf binary).

    - Crash Recovery: After system restart, DataLoader hydrates (recovers) data from RocksDB, resuming processing from the last successfully processed offset. This avoids re-consuming large volumes of historical data from Kafka.

    - RocksDB Optimization: Configure leveled compaction to balance write performance and disk space; set a memory budget (e.g., 2GB) for the block cache; enable Write-Ahead Logging (WAL) to prevent data loss in case of unexpected crashes.

- Performance Tuning: Use Vert.x Kafka Client with batch pulling (max.poll.records=500) to reduce network overhead. Directly pass validated Execution entities to ProcessorRegistry to eliminate intermediate message bus overhead.

## 3.3 ProcessorRegistry Layer (Plug-in Routing Hub)

Acts as the core routing and extension hub for the system, managing all business processors and enabling dynamic routing of Execution entities. It eliminates hard-coded dependencies between components.

- Plug-in Registration: During system startup, automatically scan and load all components implementing the `Processor` interface (e.g., MarginProcessor, PositionProcessor) via annotation scanning (@Processor). Processors are registered in a registry map for quick lookup.

- Dynamic Routing: Route Execution entities to the corresponding processors based on configurable rules (e.g., trade type, symbol category, account type). Routing rules are stored in YAML configuration files, supporting on-the-fly updates without code changes.

- Load Balancing: Distribute Execution entities across parallel processor instances to ensure even load distribution and maximize resource utilization.

- Routing Cache: Maintain an in-memory routing table cache to avoid repeated rule matching for each message, reducing routing latency.

## 3.4 Parallel Processor Pool (Business Logic Calculation)

Implements core business logic (margin and position calculation) in parallel, with reserved interfaces for reference data enrichment. It is designed to be stateless and scalable.

- Parallel Processing Foundation: Use Vert.x Worker Pool to implement parallel computing. The pool size is configured based on CPU core count (recommended 2 × number of CPU cores) to avoid context switching overhead and fully utilize multi-core resources.

- Processor Implementations:
        

    - MarginProcessor: Calculates initial margin (e.g., 50% of notional value) and maintenance margin (e.g., 25% of notional value) for each trade execution. Outputs `MarginResult` (fields: account, symbol, initialMargin, maintenanceMargin, notionalValue).

    - PositionProcessor: Calculates net positions (long/short/flat), average entry price, and real-time P&L for each account-symbol pair. Outputs `PositionResult` (fields: account, symbol, positionType, quantity, averagePrice, pnl).

- Reference Data Enrichment Interface: All processors reserve standard enrichment interfaces to integrate external reference data, ensuring extensibility for business logic:
        

    - Standard Interface: The `Processor` interface defines a default enrichment method: `default Execution enrich(Execution execution, RefDataService refDataService) throws RefDataException`.

    - Use Case Example: MarginProcessor can call `refDataService.getMarginRate(execution.getSymbol())` to obtain symbol-specific margin rates, overriding the default 50%/25% rates for more accurate calculations.

- Result Delivery: After completing calculations, processors push results to the Aggregator via callback functions (no intermediate queues) to minimize latency. Thread safety is ensured via atomic operations during result generation.

## 3.5 Aggregator Layer (Real-Time Aggregation)

Performs real-time aggregation of processor results by account-symbol pairs, maintaining consistent and accurate aggregated states for risk management and reporting.

- Aggregation Dimensions: Aggregate data by the composite key `account + symbol` to support granular risk monitoring. Aggregated metrics include total margin requirements, net position quantity, average price, and accumulated P&L.

- Push-Based Aggregation: Immediately update aggregated states upon receiving results from processors (no polling) to ensure real-time data freshness.

- Concurrent Safety: Use thread-safe data structures (e.g., ConcurrentHashMap with atomic references, CopyOnWriteArrayList) to avoid race conditions during high-throughput processing. All update operations are atomic.

- Data Consistency Verification: Implement built-in consistency checks (e.g., verifying that the sum of individual execution quantities matches the net position quantity) to detect and log data anomalies.

## 3.6 Bounded Cache Layer (Low-Latency Data Access)

Stores aggregated results to provide low-latency data access for API services, with built-in eviction policies to prevent memory leaks.

- Cache Implementation: Based on Caffeine (high-performance Java caching library) to support low-latency read/write operations.

- Core Configuration:
        

    - Maximum Entries: 10,000 (sufficient to cover all active account-symbol pairs).

    - Time-To-Live (TTL): 24 hours (automatically evict inactive data to free up memory).

    - Eviction Strategy: LRU (Least Recently Used) to prioritize retention of high-frequency access data and maximize cache hit rate.

- Cache Synchronization: Automatically refresh cache entries when aggregated states are updated by the Aggregator to ensure data consistency.

- Observability Metrics: Expose key cache metrics (hit rate, miss rate, eviction count, current size) to monitor cache performance and prevent memory issues.

# 4. Core Mechanisms for Reliability & Performance

## 4.1 Backpressure Mechanism

Prevents downstream components from being overwhelmed by upstream data surges, ensuring system stability during peak loads.

- Monitoring Trigger: ProcessorRegistry monitors the queue depth of the parallel processor pool in real time.

- Throttle Rules:
        

    - High Watermark (80%): When the queue depth reaches 80% of the maximum capacity, pause Kafka consumption via Vert.x Kafka Client to stop upstream data inflow.

    - Low Watermark (50%): When the queue depth drops below 50%, resume Kafka consumption to restore normal data flow.

- Alerting: Trigger an alarm when backpressure is activated (queue depth ≥ 80%) to notify operations teams of potential system overload.

## 4.2 Error Handling & Recovery

Implements a comprehensive error handling framework to ensure no data loss and support failure recovery.

- Dead Letter Queue (DLQ): Route all failed messages (invalid data, processing exceptions, reference data access failures) to a dedicated DLQ (topic: trade-executions-dlq). DLQ messages include the original message, error code, and error description for troubleshooting.

- Retry Strategy: For transient failures (e.g., temporary reference data service unavailability), implement exponential backoff retry:
        

    - Initial Retry Interval: 100ms

    - Maximum Retries: 5

    - Maximum Retry Interval: 5s

- Failure Logging: Log all errors with structured logs (including message details, stack traces, and timestamps) to facilitate root cause analysis.

## 4.3 Data Replay Capability

Supports reprocessing of historical data from any time point (e.g., start of day, specific timestamp) to enable reconciliation, auditing, and system recovery.

- Offset Control: Provide API and configuration parameters (e.g., `replay.from.offset`, `replay.from.timestamp`) to reset the Kafka consumer to the desired starting offset.

- Data Isolation: During replay, create a temporary RocksDB instance and bounded cache to store replay data, isolating it from production data to avoid impacting live services.

- Idempotent Processing: Ensure all processors and the Aggregator support idempotency. Reprocessing the same Execution entity (identified by `KafkaOffset + MessageID`) will not alter the final aggregated state incorrectly.

- Progress Monitoring: Expose replay progress metrics (current offset, remaining messages, replay speed) and a progress query API to enable operational control.

# 5. Extensibility & Maintainability

## 5.1 Plug-in Architecture for Processors

Enable seamless addition of new business logic without modifying core system code, reducing development and maintenance costs.

- Extension Steps for New Processors:
        

    1. Implement the `Processor` interface and override the `process(Execution execution)` method with custom logic.

    2. Add the `@Processor` annotation to the new processor class.

    3. Configure routing rules in the YAML file to route relevant Execution entities to the new processor.

    4. Deploy the new processor (supports hot deployment via Vert.x to avoid system restart).

- Interface Compatibility: Use semantic versioning for the `Processor` interface to ensure backward compatibility with existing processors.

## 5.2 Configuration Management

Centralize and standardize configuration to support environment-specific deployments and dynamic adjustments.

- Configuration Sources: Load configurations from multiple sources (prioritized from high to low):
        

    1. Environment Variables (for production environment-specific settings)

    2. application-{environment}.properties (e.g., application-prod.properties)

    3. Default application.properties

- Configuration Items: Include Kafka settings, RocksDB parameters, worker pool size, cache configuration, backpressure thresholds, and routing rules.

- Dynamic Configuration: Use Vert.x Config to support dynamic configuration updates without restarting the system (for non-critical settings).

## 5.3 Observability & Monitoring

Implement comprehensive observability to enable real-time monitoring, troubleshooting, and performance optimization.

- Metrics Collection: Track key performance and reliability metrics using Prometheus:
        

    - Throughput: Kafka consumption rate, processor processing rate, aggregator update rate.

    - Latency: Deserialization latency, processing latency, aggregation latency, cache access latency.

    - Reliability: Error rate, DLQ backlog, retry count, cache hit/miss rate.

    - Resource Utilization: CPU usage, memory usage, RocksDB disk usage, queue depth.

- Logging: Use structured logging (JSON format) to record system events, errors, and data flows. Include correlation IDs to enable full-link tracing.

- Monitoring Dashboard: Build a Grafana dashboard to visualize metrics in real time. Set up alerts for critical thresholds (e.g., error rate > 0.1%, queue depth ≥ 80%).

- Health Checks: Expose HTTP health check endpoints (e.g., `/health/live`, `/health/ready`) to monitor system status and support Kubernetes liveness/readiness probes.

# 6. Reference Data Enrichment Specification

Define standard interfaces and error handling for integrating reference data, ensuring consistency and reliability of enrichment logic across processors.

## 6.1 RefDataService Interface (Abstraction Layer)

Encapsulate access to external reference data sources, isolating processors from specific data retrieval implementations.

```java

public interface RefDataService {
    // Get margin rate for a specific symbol
    MarginRate getMarginRate(String symbol) throws RefDataException;
    
    // Get market data (latest price) for a specific symbol
    MarketData getMarketData(String symbol) throws RefDataException;
    
    // Get risk category for a specific account
    AccountRiskCategory getAccountRiskCategory(String account) throws RefDataException;
}
```

## 6.2 Implementation Requirements

- Multiple Implementations: Support different data sources via multiple implementations (e.g., `HttpRefDataService` for REST APIs, `CachedRefDataService` for local cached data).

- Dependency Injection: Use Guice for dependency injection to inject the appropriate `RefDataService` implementation into processors based on environment configuration.

## 6.3 Exception Handling

- Custom Exception: Define `RefDataException` for reference data access failures (e.g., service unavailable, data not found).

- Uniform Handling: ProcessorRegistry catches `RefDataException` uniformly, routes the corresponding Execution entity to the DLQ, and triggers retries based on the retry strategy.

# 7. Performance & Reliability Targets

The following targets define the minimum performance and reliability standards that the system must meet after implementation:

|Category|Metric|Target Value|
|---|---|---|
|Performance|End-to-End Latency (p99)|< 10ms|
||Throughput|> 10,000 msg/s|
|Reliability|Data Loss Rate|0%|
||Error Rate|< 0.1%|
||System Availability|> 99.9%|
|Operational|Cache Hit Rate|> 95%|
||Replay Speed (1 Day Data)|< 1 Hour|
||Peak CPU Usage|< 70%|
# 8. Conclusion

This document defines the final, definitive architecture for the real-time trade execution processing system. By adopting a streamlined reactive pipeline, parallel processing, persistent caching, and plug-in extensibility, the architecture addresses all critical issues in the current system while meeting business and technical requirements. The detailed component specifications, core mechanisms, and performance targets provide clear guidance for implementation, testing, and operational deployment. This architecture will serve as the foundation for building a high-performance, reliable, and maintainable system that supports future business growth.
> （注：文档部分内容可能由 AI 生成）