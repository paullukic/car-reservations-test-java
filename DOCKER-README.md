# Car Reservation System - Docker Setup

Complete guide for running the Car Reservation System with Docker and Docker Compose.

## Quick Start

### 1. Run the entire application stack
```bash
docker-compose up -d
```
This starts both the Spring Boot application and PostgreSQL database.

### 2. Check service status
```bash
docker-compose ps
```

### 3. View logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f app
docker-compose logs -f postgres
```

### 4. Stop the application
```bash
docker-compose down
```

### 5. Stop and remove all data (including volumes)
```bash
docker-compose down -v
```

## Services Overview

| Service | Port | Description | Configuration |
|---------|------|-------------|---------------|
| **app** | 8080 | Spring Boot Car Reservation API | `docker-compose.yml` |
| **postgres** | 5432 | PostgreSQL 15 database | `docker-compose.yml` |
| **app-test** | 8081 | Test environment application | `docker-compose.test.yml` |
| **postgres-test** | 5433 | Test database | `docker-compose.test.yml` |

## Application URLs

- **API**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Test Environment**: http://localhost:8081 (when using `docker-compose.test.yml`)

## Development Workflow

### Initial Setup
```bash
# Clone and navigate to project
git clone https://github.com/paullukic/car-reservations-test-java.git
cd car

# Start all services
docker-compose up -d
```

### Make Code Changes
```bash
# Rebuild the application after code changes
docker-compose build app

# Restart with new build
docker-compose up -d app

# Or do both at once
docker-compose up -d --build app
```

### Running Tests

#### Local Tests (with Testcontainers)
```bash
./mvnw test
```

#### Test Environment with Docker
```bash
# Start test environment
docker-compose -f docker-compose.test.yml up -d

# Run tests against test environment
docker-compose -f docker-compose.test.yml run --rm app-test ./mvnw test

# Stop test environment
docker-compose -f docker-compose.test.yml down -v
```

### Load Testing with Gatling
```bash
# Ensure app is running
docker-compose up -d

# Run Gatling tests
docker-compose run --rm app ./mvnw gatling:test \
  -Dgatling.simulationClass=com.reservation.car.performance.CarReservationLoadTest

# Reports are available in target/gatling/
```

## Database Configuration

### Production Database (`docker-compose.yml`)
- **Database**: `cardb`
- **User**: `user`
- **Password**: `password`
- **Port**: `5432`
- **Volume**: `postgres_data` (persistent)
- **Flyway migrations**: Run automatically on startup

### Test Database (`docker-compose.test.yml`)
- **Database**: `cardb`
- **User**: `user`
- **Password**: `password`
- **Port**: `5433` (different port to avoid conflicts)
- **Volume**: `postgres_test_data` (separate from production)

### Accessing the Database

#### Via psql command line
```bash
# Production database
docker-compose exec postgres psql -U user -d cardb

# Test database
docker-compose -f docker-compose.test.yml exec postgres-test psql -U user -d cardb
```

#### Common SQL queries
```sql
-- List all tables
\dt

-- View schema
\d+ car
\d+ reservation

-- Check car count
SELECT COUNT(*) FROM car;

-- View recent reservations
SELECT * FROM reservation ORDER BY created_at DESC LIMIT 10;
```

## Environment Variables

The application uses Spring profiles:

### Default Profile (`application.properties`)
Used for local development without Docker.

### Docker Profile (`application-docker.yml`)
Activated when running in Docker:
- `SPRING_PROFILES_ACTIVE=docker`
- `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/cardb`
- `SPRING_DATASOURCE_USERNAME=user`
- `SPRING_DATASOURCE_PASSWORD=password`

### Test Profile (`application-test.yml`)
Activated for test environment:
- `SPRING_PROFILES_ACTIVE=test`

## Docker Images

### Building the Application Image
```bash
# Build only (no run)
docker-compose build app

# Build without cache (clean build)
docker-compose build --no-cache app
```

### Image Details
- Based on `Dockerfile` in project root
- Multi-stage build (if applicable)
- Exposes port `8080`

## Volumes

### Persistent Data
- **postgres_data**: Production database data
- **postgres_test_data**: Test database data
- **./target/gatling**: Mounted for Gatling reports

### Managing Volumes
```bash
# List volumes
docker volume ls

# Inspect volume
docker volume inspect car_postgres_data

# Remove all volumes (WARNING: deletes all data)
docker-compose down -v
```

## Networking

- **Production**: `car-reservation-network` (bridge)
- **Test**: `car-reservation-test-network` (bridge)

Services within the same network can communicate using service names (e.g., `postgres`, `app`).

## Health Checks

### PostgreSQL Health Check
- Command: `pg_isready -U user -d cardb`
- Interval: 10s
- Retries: 5

### Application Health Check
- URL: `http://localhost:8080/actuator/health`
- Interval: 30s
- Start period: 40s
- Retries: 3

## Troubleshooting

### Services won't start
```bash
# Check service status
docker-compose ps

# View logs for errors
docker-compose logs
```

### Database connection refused
```bash
# Ensure PostgreSQL is healthy
docker-compose ps postgres

# Check database logs
docker-compose logs postgres

# Verify database is accepting connections
docker-compose exec postgres pg_isready -U user -d cardb
```

### Application won't start
```bash
# Check if app container is running
docker-compose ps app

# View application logs
docker-compose logs app

# Check for port conflicts
netstat -ano | findstr :8080
```

### Port already in use
```bash
# Find process using port 8080
netstat -ano | findstr :8080

# Stop existing services
docker-compose down

# Or change port in docker-compose.yml
```

### Reset everything
```bash
# Stop and remove containers, networks, volumes
docker-compose down -v

# Remove unused Docker resources
docker system prune -f

# Rebuild from scratch
docker-compose build --no-cache
docker-compose up -d
```

### Performance issues
```bash
# Check resource usage
docker stats

# Allocate more resources in Docker Desktop settings
# (Settings > Resources > Advanced)
```

## Best Practices

1. **Development**: Use `docker-compose up -d` for background services
2. **Testing**: Use `docker-compose.test.yml` to isolate test data
3. **Logs**: Regularly check logs with `docker-compose logs -f`
4. **Cleanup**: Run `docker-compose down -v` when done to free resources
5. **Security**: Change default passwords before production deployment
6. **Backups**: Export database before major changes:
   ```bash
   docker-compose exec postgres pg_dump -U user cardb > backup.sql
   ```

## Production Considerations

⚠️ **This setup is for development only**. For production:

1. Use strong passwords and secrets management
2. Enable SSL/TLS for database connections
3. Use proper orchestration (Kubernetes, ECS, etc.)
4. Implement proper backup and disaster recovery
5. Configure resource limits and health checks
6. Use a reverse proxy (nginx, Traefik) for the application
7. Enable monitoring and logging aggregation