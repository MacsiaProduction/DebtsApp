# Debt Calculation Network API

The Debt Calculation Network API is a Spring-based REST service that allows users to manage transactions and calculate debts between users. This service uses Spring JDBC, Spring Security, and PostgreSQL as the database.

## Table of Contents
- [API Endpoints](#api-endpoints)
- [Getting Started](#getting-started)
- [Authentication](#authentication)
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

## API Endpoints
This API provides the following endpoints:

- `GET /transactions`: Get a list of all transactions.
- `GET /transactions/related?name={user}`: Get a list of transactions related to a specific user.
- `GET /transactions/between?sender={sender}&recipient={recipient}`: Get a list of transactions between two users.
- `POST /new`: Add a new transaction.
- `GET /debts/between?from={fromName}&to={toName}`: Get the debt between two users.
- `GET /debts/related?name={user}`: Get a list of debts related to a specific user.
- `GET /debts`: Get a list of all debts.
- `POST /change-password`: Change the user's password.
- `POST /login`: Authenticate a user.

## Getting Started
1. Clone the project from the repository.
2. Set up your PostgreSQL database and update the database configuration in the `application.properties` file.
3. Build and run the project using Maven or your preferred IDE.

## Authentication
Some endpoints require user authentication using Spring Security. You need to provide valid credentials to access these endpoints. To authenticate, use the `/login` endpoint and provide a valid username and password in the request body.

## Endpoints

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

---

This README provides an overview of the Debt Calculation Network API and its endpoints. For detailed information on request and response formats, please refer to the API documentation or code comments in the `WebController` class.
