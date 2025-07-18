# Satellite Telemetry System

A system for receiving, processing, and analyzing telemetry data from satellites and IoT devices. This application demonstrates the use of virtual threads in Java 21 for handling high-concurrency workloads.

## Features

- **High-concurrency processing** using Java 21 virtual threads
- **Real-time telemetry data reception** via REST API
- **Asynchronous processing** of telemetry packets
- **Automatic alert detection** for critical conditions
- **Persistent storage** in PostgreSQL database
- **Caching** with Redis for improved performance
- **Dockerized deployment** for easy setup and scaling

## Architecture

The system is built using:

- **Java 21** for virtual threads support
- **Spring Boot 3.x** for the application framework
- **PostgreSQL** for persistent storage
- **Redis** for caching and message queuing
- **Docker** for containerization
- **MapStruct** for DTO mapping

## Project Structure

- `model` - Domain entities
- `dto` - Data Transfer Objects
- `mapper` - MapStruct mappers for entity-DTO conversion
- `repository` - Data access layer
- `service` - Business logic layer
- `controller` - REST API endpoints
- `config` - Application configuration
- `exception` - Exception handling
- `simulator` - Telemetry data simulator

## Running the Application

### Prerequisites

- Docker and Docker Compose
- Java 21 (for development)

### Using Docker Compose for Infrastructure Only

1. Clone the repository
2. Navigate to the project directory
3. Start PostgreSQL and Redis using Docker Compose:

```bash
docker-compose up -d
```

This will start:
- PostgreSQL database on port 5432
- Redis on port 6379

4. Run the application locally:

```bash
./mvnw spring-boot:run
```

The application will connect to the dockerized PostgreSQL and Redis services automatically because it's configured to use localhost:5432 for PostgreSQL and localhost:6379 for Redis.

### Full Dockerized Setup (Optional)

If you want to run the entire application in Docker, including the Spring Boot application:

1. Uncomment the `app` service in the `docker-compose.yml` file
2. Run Docker Compose:

```bash
docker-compose up -d
```

## API Endpoints

### Telemetry Data

- `POST /api/telemetry` - Submit a single telemetry packet
- `POST /api/telemetry/batch` - Submit multiple telemetry packets
- `GET /api/telemetry/{id}` - Get a telemetry packet by ID
- `GET /api/telemetry/device/{deviceId}` - Get telemetry packets for a specific device
- `GET /api/telemetry/device/{deviceId}/timerange` - Get telemetry packets for a device within a time range

## Telemetry Simulator

The application includes a telemetry simulator that can be used to generate test data. To enable it, set the `simulator` profile:

```bash
./mvnw spring-boot:run -Dspring.profiles.active=simulator
```

Or uncomment the simulator service in the docker-compose.yml file.

## Configuration

The application can be configured using environment variables or by modifying the `application.yaml` file:

- Database connection settings
- Redis connection settings
- Alert thresholds
- Processing parameters
- Simulator parameters

## License

This project is licensed under the MIT License - see the LICENSE file for details.
