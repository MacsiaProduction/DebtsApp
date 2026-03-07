# Backend - Debt Calculation Network API

Spring Boot REST API for transaction management and debt calculation among users.

## Technologies

- **Framework**: Spring Boot
- **Security**: Spring Security with JWT
- **Databases**:
  - PostgreSQL (relational data)
  - Neo4j (graph-based debt relationships)
- **Database Migrations**: Liquibase
- **Build Tool**: Gradle

## Prerequisites

- Java 17 or higher
- PostgreSQL 15
- Neo4j 5
- Gradle (or use the included Gradle wrapper)

## Configuration

Update `src/main/resources/application.properties` with your database credentials:

```properties
# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/debtsbot
spring.datasource.username=your_username
spring.datasource.password=your_password

# Neo4j
spring.neo4j.uri=bolt://localhost:7687
spring.neo4j.authentication.username=neo4j
spring.neo4j.authentication.password=your_password
```

## Building and Running

### Using Gradle Wrapper (Recommended)

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run the application
./gradlew bootRun

# Clean build artifacts
./gradlew clean
```

### Using Docker

From the root directory:

```bash
docker-compose up
```

This will start PostgreSQL, Neo4j, and the Spring Boot application.

## Project Structure

```
src/
├── main/
│   ├── java/ru/m_polukhin/debtsapp/
│   │   ├── configs/          # Security, JWT, and application configs
│   │   ├── controllers/      # REST endpoints
│   │   ├── dto/              # Data transfer objects
│   │   ├── exceptions/       # Custom exceptions
│   │   ├── models/           # Entity models
│   │   ├── repository/       # Data access layer
│   │   ├── services/         # Business logic
│   │   └── utils/            # Utility classes
│   └── resources/
│       ├── application.properties
│       └── db/changelog/     # Liquibase migrations
└── test/                     # Unit and integration tests
```

## API Endpoints

See the main README for detailed API documentation.

## Testing

```bash
# Run all tests
./gradlew test

# Run tests with coverage
./gradlew test jacocoTestReport
```

## Database Migrations

Liquibase migrations are located in `src/main/resources/db/changelog/`.

Migrations run automatically on application startup.
