# Debt Calculation Network

A monorepo application for transaction management and debt calculation among users. The project consists of a Spring-based REST API backend and a frontend application.

## Project Structure

```
.
├── backend/          # Spring Boot REST API
├── frontend/         # Frontend application (to be implemented)
└── docker-compose.yml # Docker orchestration
```

## Table of Contents
- [Getting Started](#getting-started)
- [Backend](#backend)
- [Frontend](#frontend)
- [Authentication](#authentication)
- [REST Endpoints](#rest-endpoints)
- [Telegram Bot Integration](#telegram-bot-integration)

## Getting Started

### Using Docker Compose (Recommended)
1. Clone the project from the repository.
2. Run `docker-compose up` from the root directory.
3. The backend will be available at `http://localhost:8080`.

### Manual Setup
1. Set up PostgreSQL and Neo4j databases.
2. Navigate to the `backend/` directory.
3. Update the database configuration in `src/main/resources/application.properties`.
4. Build and run the project using Gradle or your preferred IDE.

### Quick Commands (Using Makefile)

From the root directory, you can use these commands:

```bash
# Backend
make build-backend    # Build the backend application
make test-backend     # Run backend tests
make run-backend      # Run the backend application
make clean-backend    # Clean backend build artifacts

# Docker
make docker-up        # Start all services with Docker Compose
make docker-down      # Stop all services

# Help
make help            # Show all available commands
```

Alternatively, you can work directly in the subdirectories:

```bash
# Backend
cd backend
./gradlew build
./gradlew test
./gradlew bootRun
```

## Backend

The backend is a Spring-based REST service that leverages Spring JDBC, Spring Security, PostgreSQL, and Neo4j.

### Technologies
- Spring Boot
- Spring Security
- PostgreSQL
- Neo4j
- Liquibase for database migrations

For more details, see the backend directory.

## Frontend

The frontend directory is ready for your frontend framework of choice (React, Vue, Angular, etc.).

For more details, see the frontend directory.

## Authentication
Certain REST endpoints require user authentication using Spring Security. Valid credentials must be provided to access these endpoints. To authenticate, use the `/login` endpoint and provide a valid username and password in the request body.

## REST Endpoints
The API provides various endpoints for managing transactions and debts between users. Some key endpoints include:
- `/transactions`: Retrieve a list of all transactions.
- `/transactions/related?name={user}`: Retrieve transactions related to a specific user.
- `/transactions/between?sender={sender}&recipient={recipient}`: Retrieve transactions between two users.
- `/new`: Add a new transaction.
- `/debts/between?from={fromName}&to={toName}`: Get the debt between two users.
- `/debts/related?name={user}`: Retrieve debts related to a specific user.
- `/debts`: Retrieve a list of all debts.
- `/login`: Authenticate a user.
- `/session`: Get a token to get access to website using telegram

## Telegram Bot Integration
The API includes a Telegram bot integration through the `TelegramController` class. The bot allows users to interact with the service conveniently via Telegram. Some available commands include:
- `/start`: Start a chat with the bot.
- `/add TgUsername {sum}`: Add a new transaction from yourself to another user with a specified amount.
- `/get TgUsername`: Check the debt between you and another user.
- `/history`: Retrieve a list of all related transactions.
- `/debts`: Retrieve a list of all related debts.