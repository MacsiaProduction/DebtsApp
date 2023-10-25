# Debt Calculation Network API

The Debt Calculation Network API is a Spring-based REST service that allows users to manage transactions and calculate debts between users. This service uses Spring JDBC, Spring Security, and PostgreSQL as the database.

## Table of Contents
- [API Endpoints](#api-endpoints)
- [Getting Started](#getting-started)
- [Authentication](#authentication)
- [REST Endpoints](#rest-endpoints)
- [Endpoints](#endpoints)
  - [List of All Transactions](#list-of-all-transactions)
  - [List of Transactions Related to a User](#list-of-transactions-related-to-a-user)
  - [List of Transactions Between Users](#list-of-transactions-between-users)
  - [Add a New Transaction](#add-a-new-transaction)
  - [Get Debt Between Users](#get-debt-between-users)
  - [List of Debts Related to a User](#list-of-debts-related-to-a-user)
  - [List of All Debts](#list-of-all-debts)
  - [Change User Password](#change-user-password)
  - [User Login](#user-login)
- [Telegram Bot Integration](#telegram-bot-integration)
- [Telegram Bot Commands](#telegram-bot-commands)

## API Endpoints
This API provides the following REST endpoints:

- `GET /transactions`: Get a list of all transactions.
- `GET /transactions/related?name={user}`: Get a list of transactions related to a specific user.
- `GET /transactions/between?sender={sender}&recipient={recipient}`: Get a list of transactions between two users.
- `POST /new`: Add a new transaction.
- `GET /debts/between?from={fromName}&to={toName}`: Get the debt between two users.
- `GET /debts/related?name={user}`: Get a list of debts related to a specific user.
- `GET /debts`: Get a list of all debts.
- `POST /change-password`: Change a user's password.
- `POST /login`: Authenticate a user.

## Getting Started
1. Clone the project from the repository.
2. Set up your PostgreSQL database and update the database configuration in the `application.properties` file.
3. Build and run the project using Maven or your preferred IDE.

## Authentication
Some REST endpoints require user authentication using Spring Security. You need to provide valid credentials to access these endpoints. To authenticate, use the `/login` endpoint and provide a valid username and password in the request body.

## REST Endpoints

### List of All Transactions
- **Endpoint:** `GET /transactions`
- **Description:** Retrieve a list of all transactions.

### List of Transactions Related to a User
- **Endpoint:** `GET /transactions/related?name={user}`
- **Description:** Retrieve a list of transactions related to a specific user.

### List of Transactions Between Users
- **Endpoint:** `GET /transactions/between?sender={sender}&recipient={recipient}`
- **Description:** Retrieve a list of transactions between two users.

### Add a New Transaction
- **Endpoint:** `POST /new`
- **Description:** Add a new transaction.
- **Request Body:** Provide a JSON object containing transaction details.

### Get Debt Between Users
- **Endpoint:** `GET /debts/between?from={fromName}&to={toName}`
- **Description:** Get the debt between two users.

### List of Debts Related to a User
- **Endpoint:** `GET /debts/related?name={user}`
- **Description:** Retrieve a list of debts related to a specific user.

### List of All Debts
- **Endpoint:** `GET /debts`
- **Description:** Retrieve a list of all debts.

### Change User Password
- **Endpoint:** `POST /change-password`
- **Description:** Change a user's password.
- **Request Body:** Provide a JSON object with the old and new passwords.

### User Login
- **Endpoint:** `POST /login`
- **Description:** Authenticate a user. Provide valid credentials in the request body.

Please note that some endpoints may return a `BAD_REQUEST` status if there are validation or authentication issues.

## Telegram Bot Integration
This API also includes a Telegram bot integration using the `TelegramController` class. The bot provides a convenient way to interact with the service.

To use the Telegram bot, search for it by its username, and start a chat. The bot will respond to the following commands:

## Telegram Bot Commands

- `/start`: Start a chat with the bot. The bot will welcome you and register your username.
- `/add TgUsername(no @) {sum}`: Add a new transaction from yourself to another user (without '@' symbol) with a specified amount.
- `/get TgUsername`: Check the size of the debt between you and another user.
- `/history`: Retrieve a list of all related transactions.
- `/debts`: Retrieve a list of all related debts.
- `/new_password`: Generate a new password for web interface.

## Command Examples

- `/add John 100`: Add a transaction from your username to 'John' with a sum of 100.
- `/get Alice`: Check the debt between you and 'Alice'.
- `/history`: View a list of all transactions related to you.
- `/debts`: View a list of all debts related to you.
- `/new_password`: Generate a new password for web interface access.

For any other commands, the bot will respond with "Not recognized."


This README provides an overview of the Debt Calculation Network API and its endpoints. For detailed information on request and response formats, please refer to the API documentation or code comments in the `WebController` class.
