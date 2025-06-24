# MemDB - In-Memory Database

A lightweight, in-memory database implementation that supports key-value and sorted set operations, similar to Redis. Built with Spring Boot, it provides a REST API for data operations.

## Features

### Key-Value Operations
- `SET key value [EX seconds]` - Set a key-value pair with optional expiration
- `GET key` - Retrieve a value by key
- `DEL key` - Delete a key-value pair
- `INCR key` - Increment a numeric value by 1

### Sorted Set Operations
- `ZADD key score value` - Add a value with score to a sorted set
- `ZCARD key` - Get the number of elements in a sorted set
- `ZRANK key value` - Get the rank of a value in a sorted set
- `ZRANGE key start end` - Get a range of values from a sorted set

### Memory Management
- Automatic memory limit enforcement (100MB default)
- LRU eviction policy for key-value store
- Random lowest score eviction for sorted sets

## Getting Started

### Prerequisites
- Java 17 or higher
- Gradle 8.x

### Building the Project
To build the project, run the following command in your terminal:

```bash
./gradlew bootJar
```

This will create a JAR file named `MemDB.jar` in the `build/libs` directory.

### Running the Application

You can run the application in two ways:

1. **Using Gradle:**
   Run the following command to start the application using Gradle:

   ```bash
   ./gradlew bootRun
   ```

2. **Using the JAR file directly:**
   Navigate to the `build/libs` directory and run the JAR file using Java:

   ```bash
   java -jar MemDB.jar
   ```

The application will start on port 8080. You can access the API at `http://localhost:8080`.

### Running Tests
To run the tests, use the following command:

```bash
./gradlew test
```

This will execute all the tests in the project, including unit tests, integration tests, and concurrent tests.

## API Usage

### Key-Value Operations
```bash
# Set a value
curl -X POST "http://localhost:8080/set?key=mykey&value=myvalue"

# Get a value
curl "http://localhost:8080/get?key=mykey"

# Set with expiration (10 seconds)
curl -X POST "http://localhost:8080/set?key=tempkey&value=tempvalue&expiry=10"

# Increment a counter
curl -X POST "http://localhost:8080/incr?key=counter"
```

### Sorted Set Operations
```bash
# Add to sorted set
curl -X POST "http://localhost:8080/zadd?key=scores&score=100&value=player1"

# Get set size
curl "http://localhost:8080/zcard?key=scores"

# Get value rank
curl "http://localhost:8080/zrank?key=scores&value=player1"

# Get range of values
curl "http://localhost:8080/zrange?key=scores&start=0&end=10"
```

## Memory Management

The database enforces a 100MB memory limit. When this limit is reached:
- For key-value store: Least Recently Used (LRU) items are evicted
- For sorted sets: Random sets are selected and their lowest scores are removed

## Testing

The project includes comprehensive test coverage across all components:

### Test Categories
- Unit Tests: Testing individual components in isolation
- Integration Tests: Testing component interactions
- Concurrent Tests: Testing thread safety and concurrent operations
- Validation Tests: Testing input validation and error handling

### Test Coverage
The test suite includes:
- 76 total tests with 100% success rate
- Coverage across all major components:
  - Controller layer (33 tests)
  - Service layer (33 tests)
  - Core MemDB functionality (10 tests)

### Running Tests
To run the tests, use one of the following commands:

```bash
# Run all tests
./gradlew cleanTest test

```
