#Debt Calculation Network Service
The Debt Calculation Network Service is a RESTful web service built using Spring Boot that allows users to manage and calculate debts between users by recording transactions. It includes endpoints for managing transactions, calculating debts, and changing user passwords.

Prerequisites
Java Development Kit (JDK) - 1.8 or higher
Apache Maven - Build and dependency management tool
PostgreSQL - Database for storing user and transaction data
Getting Started
Clone the project from the repository.

bash
Copy code
git clone <repository-url>
Navigate to the project directory.

bash
Copy code
cd debtsapp
Build and package the application using Maven.

bash
Copy code
mvn package
Create a PostgreSQL database and configure the database connection settings in application.properties:

properties
Copy code
spring.datasource.url=jdbc:postgresql://localhost:5432/debtsdb
spring.datasource.username=your-username
spring.datasource.password=your-password
Run the application.

bash
Copy code
java -jar target/debtsapp-0.0.1-SNAPSHOT.jar
The service should now be up and running on http://localhost:8080.

Endpoints

List All Transactions
GET /transactions
Returns a list of all transactions.

Find All Transactions Related to a User
GET /transactions/related?name={user}
Returns a list of all transactions related to a specific user.

Find All Transactions Between Users
GET /transactions/between?sender={sender}&recipient={recipient}
Returns a list of all transactions from one user to another.

Add New Transaction
POST /new
Add a new transaction by providing transaction details in the request body.

Get Debt Between Users
GET /debts/between?from={fromName}&to={toName}
Returns the debt amount between two users.

Find All Debts Related to a User
GET /debts/related?name={user}
Returns a list of all debts related to a specific user.

Find All Debts
GET /debts
Returns a list of all debts.

Change User Password
POST /change-password
Change a user's password by providing the necessary details in the request body.

Authenticate User
POST /login
Authenticate a user by providing login credentials in the request body.

Swagger API Documentation
The service includes Swagger for API documentation. You can access the Swagger UI at http://localhost:8080/swagger-ui.html.

Security
The application uses Spring Security to secure the endpoints and authenticate users.

Database
The application uses PostgreSQL as the database to store user and transaction data. You can find the database schema and table structure in the application.

Contributing
Contributions to the project are welcome. Please follow the standard GitHub fork and pull request workflow.

Authors
Macsia
