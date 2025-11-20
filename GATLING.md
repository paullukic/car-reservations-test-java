# Load Testing with Gatling

This guide explains how to run performance and load tests using Gatling to validate scalability for 10,000+ cars.

## Overview

The load tests simulate real-world usage scenarios:

- **Car Listing**: Paginated requests to browse available cars
- **Availability Checks**: Queries for cars available in specific time slots
- **Reservation Creation**: Concurrent booking attempts (validates double-booking prevention)

These tests prove the system can handle high concurrency and large datasets efficiently.

## Prerequisites

- Java 17+
- Maven 3.6+
- Docker & Docker Compose
- Application running with PostgreSQL

## Setup

### 1. Start the Test Environment

Use the dedicated test environment to avoid affecting your production/development database:

```bash
docker-compose -f docker-compose.test.yml up -d
```

This starts:
- Test PostgreSQL database on port 5433
- Test application on port 8081

**Verify services are running:**
```bash
docker-compose -f docker-compose.test.yml ps
```

### 2. Populate Test Database with 10,000 Cars

For realistic load testing, populate the test database with sample data:

**Option A: Using test class (recommended)**

Run from host machine, pointing to the test database on port 5433:

```bash
./mvnw test -Dtest=DatabasePopulationTest \
  -Dspring.profiles.active=test \
  -Dspring.datasource.url=jdbc:postgresql://localhost:5433/cardb \
  -Dspring.datasource.username=user \
  -Dspring.datasource.password=password
```

**Option B: Direct SQL**
```bash
# Connect to test database
docker-compose -f docker-compose.test.yml exec postgres-test psql -U user -d cardb
```

Then run:
```sql
DO $$
DECLARE i INTEGER := 0;
BEGIN
  FOR i IN 1..10000 LOOP
    INSERT INTO car (id, make, model, license_plate) VALUES (
      gen_random_uuid(),
      'Make' || (i % 10)::text,
      'Model' || (i % 100)::text,
      'PLATE' || LPAD(i::text, 5, '0')
    );
  END LOOP;
END $$;
```

**Verify data population:**
```sql
SELECT COUNT(*) FROM car;
-- Should return 10000+ (includes the 10 pre-loaded sample cars)
```

Type `\q` to exit psql.

## Running Load Tests

### Execute Gatling Simulation

Run the load tests from the host machine against the test environment:

```bash
./mvnw gatling:test
```

Or specify the simulation class explicitly:

```bash
./mvnw gatling:test -Dgatling.simulationClass=com.reservation.car.performance.CarReservationLoadTest
```

The tests will run against `http://localhost:8081` (test application) with the populated test database.

**Note**: The test environment is isolated from your development/production setup, so you can run aggressive load tests without risk. Tests must be run from the host machine as the Docker container only contains the runtime application, not Maven or test sources.

### View Test Reports

After the test completes, Gatling generates an HTML report.

**Option 1: Open directly**
The terminal output will show the report location:
```
Please open the following file: target/gatling/carreservationloadtest-20251119231945136/index.html
```

**Option 2: Browse reports directory**
```bash
# Windows
explorer target\gatling

# Linux/Mac
open target/gatling
```

**Option 3: Find latest report**
```bash
# Windows (PowerShell)
Get-ChildItem target\gatling -Directory | Sort-Object LastWriteTime -Descending | Select-Object -First 1 | ForEach-Object { Start-Process "$($_.FullName)\index.html" }

# Linux/Mac
open $(ls -td target/gatling/*/ | head -1)index.html
```

### Cleanup After Testing

When you're done with load testing, stop and remove the test environment:

```bash
docker-compose -f docker-compose.test.yml down -v
```

This removes test containers and volumes, freeing up system resources.

## Test Configuration

### Simulation Details

- **Location**: `src/test/scala/com/reservation/car/performance/CarReservationLoadTest.scala`
- **Framework**: Gatling 3.13.5 with Scala 2.13.15

### Load Profile

The test simulates three types of concurrent users:

1. **Car Listing Users** (100 users)
   - Ramp up: 30 seconds
   - Action: Browse paginated car lists
   - Expected: HTTP 200

2. **Availability Check Users** (50 users)
   - Ramp up: 30 seconds
   - Action: Query available cars for time slots
   - Expected: HTTP 200

3. **Reservation Users** (20 users)
   - Ramp up: 30 seconds
   - Action: Create concurrent reservations
   - Expected: HTTP 201 (success) or 409 (conflict/double-booking)

### Test Duration

- **Total runtime**: ~90 seconds
- **Concurrent users**: Up to 170 simultaneous users at peak

## Expected Results

### Performance Targets (10,000 cars)

| Metric | Target | Description |
|--------|--------|-------------|
| **Mean Response Time** | < 10ms | Average response across all requests |
| **95th Percentile** | < 20ms | 95% of requests faster than this |
| **99th Percentile** | < 50ms | 99% of requests faster than this |
| **Throughput** | 15-20 req/sec | Requests processed per second |
| **Error Rate** | < 5% | Failed requests (excluding 409 conflicts) |
| **Success Rate** | > 95% | Successful operations |

### What to Look For

✅ **Good Indicators:**
- Response times stay consistent under load
- No 500 errors (internal server errors)
- 409 conflicts are expected (double-booking prevention working)
- Database connection pool remains healthy
- No deadlocks or timeout exceptions

❌ **Red Flags:**
- Response times increasing over time (memory leak?)
- High rate of 500 errors
- Database connection errors
- Application crashes
- Thread pool exhaustion

## Understanding the Report

### Key Sections

1. **Global Statistics**: Overall performance summary
2. **Request Statistics**: Per-endpoint breakdown
3. **Distribution**: Response time percentiles
4. **Active Users Over Time**: Concurrency visualization
5. **Response Time Distribution**: Histogram of response times

### Sample Report Metrics

```
Global Information
  Request count: 12000
  OK count: 11450
  KO count: 550
  Mean response time: 8ms
  95th percentile: 18ms
  99th percentile: 35ms
```

## Customizing Tests

### Modify Load Profile

Edit `src/test/scala/com/reservation/car/performance/CarReservationLoadTest.scala`:

```scala
// Increase users
carListingScenario.inject(
  rampUsers(200).during(60.seconds)  // Changed from 100 to 200
)

// Extend duration
carListingScenario.inject(
  rampUsers(100).during(60.seconds),  // Changed from 30 to 60
  constantUsersPerSec(50).during(120.seconds)  // Added sustained load
)

// Add spike test
carListingScenario.inject(
  nothingFor(10.seconds),
  atOnceUsers(500),  // Spike of 500 users
  nothingFor(10.seconds)
)
```

### Target Different Environments

```bash
# Test against different host
./mvnw gatling:test \
  -Dgatling.simulationClass=com.reservation.car.performance.CarReservationLoadTest \
  -DbaseUrl=http://staging.example.com:8080
```

## Troubleshooting

### ClassNotFoundException for Scala classes

**Problem**: Gatling can't find simulation class

**Solution**: Compile Scala test sources
```bash
./mvnw test-compile
```

### Database Connection Refused

**Problem**: Can't connect to test PostgreSQL

**Solution**: Ensure test database is running
```bash
docker-compose -f docker-compose.test.yml ps postgres-test
docker-compose -f docker-compose.test.yml up -d postgres-test
```

### High Error Rate (> 20%)

**Problem**: Many 500 errors in report

**Solutions**:
1. Check test application logs:
   ```bash
   docker-compose -f docker-compose.test.yml logs app-test
   ```

2. Verify test database has data:
   ```bash
   docker-compose -f docker-compose.test.yml exec postgres-test psql -U user -d cardb -c "SELECT COUNT(*) FROM car;"
   ```
   Should return 10,000+ cars.

3. Ensure valid UUIDs in test scenarios

### Out of Memory Errors

**Problem**: Application crashes during load test

**Solutions**:
1. Increase JVM heap size in `Dockerfile`:
   ```dockerfile
   ENV JAVA_OPTS="-Xmx2g -Xms1g"
   ```

2. Reduce concurrent users in test

### Slow Response Times (> 100ms)

**Problem**: Performance degrades under load

**Investigation steps**:

1. Check database query performance:
   ```sql
   -- Enable query timing
   \timing on
   
   -- Test query performance
   EXPLAIN ANALYZE SELECT * FROM car WHERE make = 'Make1' LIMIT 20;
   ```

2. Monitor database connections:
   ```sql
   SELECT count(*) FROM pg_stat_activity;
   ```

3. Check for missing indexes:
   ```sql
   SELECT schemaname, tablename, indexname 
   FROM pg_indexes 
   WHERE tablename IN ('car', 'reservation');
   ```

## Monitoring During Tests

### Application Metrics

**Health endpoint:**
```bash
curl http://localhost:8081/actuator/health
```

**Application logs (live):**
```bash
docker-compose -f docker-compose.test.yml logs -f app-test
```

### Database Metrics

**Active connections:**
```sql
SELECT count(*), state FROM pg_stat_activity GROUP BY state;
```

**Slow queries:**
```sql
SELECT pid, now() - query_start as duration, query 
FROM pg_stat_activity 
WHERE state = 'active' AND now() - query_start > interval '1 second';
```

**Lock monitoring:**
```sql
SELECT * FROM pg_locks WHERE NOT granted;
```

### System Resources

**Docker stats:**
```bash
docker stats car-reservation-test-app car-reservation-test-db
```

**CPU and Memory:**
- Watch for CPU > 80% sustained
- Watch for memory usage climbing continuously (leak indicator)

## Best Practices

1. **Use Test Environment**: Always use `docker-compose.test.yml` for load testing to keep production/dev data safe
2. **Baseline First**: Run load test with default 10 cars to establish baseline
3. **Incremental Load**: Gradually increase users (10 → 50 → 100 → 200)
4. **Clean State**: Reset test database between tests for consistency:
   ```bash
   docker-compose -f docker-compose.test.yml down -v
   docker-compose -f docker-compose.test.yml up -d
   ```
5. **Multiple Runs**: Run 3-5 times and average results
6. **Document Results**: Save reports with timestamps and configurations
7. **Monitor Everything**: Watch app logs, DB metrics, and system resources
8. **Test Realistic Scenarios**: Use actual user behavior patterns

## Advanced Testing

### Stress Testing

Find breaking point by increasing load until failure:

```bash
# Run with increasing users until failure
for users in 100 200 500 1000; do
  echo "Testing with $users users..."
  # Modify test to use $users
  ./mvnw gatling:test
  sleep 60  # Cool down between tests
done
```

### Soak Testing

Test stability over extended period:

```scala
// Modify scenario for long duration
setUp(
  carListingScenario.inject(
    rampUsers(100).during(5.minutes),
    constantUsersPerSec(50).during(2.hours)  // 2 hour soak test
  )
)
```

## CI/CD Integration

### GitHub Actions Example

```yaml
- name: Run Load Tests
  run: |
    docker-compose up -d
    ./mvnw test -Dtest=DatabasePopulationTest
    ./mvnw gatling:test
    
- name: Upload Gatling Report
  uses: actions/upload-artifact@v3
  with:
    name: gatling-report
    path: target/gatling/
```

## Further Reading

- [Gatling Documentation](https://gatling.io/docs/current/)
- [Gatling Maven Plugin](https://gatling.io/docs/current/extensions/maven_plugin/)
- [Performance Testing Best Practices](https://gatling.io/docs/current/general/)