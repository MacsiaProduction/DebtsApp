# Debt Calculation Network API

The Debt Calculation Network API is a Spring-based REST service that facilitates transaction management and debt calculation among users. It leverages Spring JDBC, Spring Security, and PostgreSQL as the underlying technologies.

## Table of Contents
- [Getting Started](#getting-started)
- [Authentication](#authentication)
- [REST Endpoints](#rest-endpoints)
- [Telegram Bot Integration](#telegram-bot-integration)

## Getting Started
1. Clone the project from the repository.
2. Set up your PostgreSQL database and update the database configuration in the `application.properties` file.
3. Build and run the project using Maven or your preferred IDE.

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