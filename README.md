# ApexBank - Banking Management System

ApexBank is a console-based banking application developed using Java, JDBC, and MySQL. It allows users to create accounts, manage balances, transfer money, and view transaction details.

## Features

- Create a bank account with a unique username
- Login with password authentication
- View account details and balance
- Deposit and withdraw money
- Transfer money between accounts
- View transaction history and mini statements
- Generate transaction receipts
- Change account password
- Temporarily lock login after three failed password attempts
- Validate menu inputs and transaction amounts
- Use database transactions with commit and rollback

## Technologies Used

- Java
- JDBC
- MySQL
- SQL
- Object-Oriented Programming (OOP)

## Project Structure

```text
ApexBank/
├── src/
│   └── com/
│       └── banking/
│           ├── Bank.java
│           ├── BankManagement.java
│           └── DatabaseConnection.java
└── README.md
```

## Prerequisites

- Java JDK
- MySQL Server
- MySQL Connector/J
- Java IDE such as Eclipse, IntelliJ IDEA, or VS Code

## Database Setup

Create the database in MySQL:

```sql
CREATE DATABASE BANK;
USE BANK;
```

Create the customer table:

```sql
CREATE TABLE customer (
    ac_no INT PRIMARY KEY AUTO_INCREMENT,
    cname VARCHAR(100) UNIQUE NOT NULL,
    balance DECIMAL(12,2) NOT NULL DEFAULT 1000.00,
    pass_code INT NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until DATETIME NULL
);
```

Create the transactions table:

```sql
CREATE TABLE transactions (
    transaction_id INT PRIMARY KEY AUTO_INCREMENT,
    sender_ac INT NOT NULL,
    receiver_ac INT NULL,
    amount DECIMAL(12,2) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_ac) REFERENCES customer(ac_no),
    FOREIGN KEY (receiver_ac) REFERENCES customer(ac_no)
);
```

## Database Configuration

Open `DatabaseConnection.java` and configure your local MySQL credentials:

```java
String url = "jdbc:mysql://localhost:3306/BANK";
String user = "root";
String pass = "YOUR_MYSQL_PASSWORD";
```

Replace `YOUR_MYSQL_PASSWORD` with your local MySQL password. Do not upload real database credentials to GitHub.

Add the MySQL Connector/J library to your project.

## How to Run

1. Clone the repository:

   ```bash
   git clone https://github.com/Tirupathi-10/ApexBank-Banking-Management-System.git
   ```

2. Open the project in your Java IDE.
3. Create the database and tables using the SQL commands above.
4. Configure the MySQL connection in `DatabaseConnection.java`.
5. Add MySQL Connector/J to the project.
6. Run `Bank.java`.
7. Create an account and log in to explore the banking features.

## Transaction Management

The application uses JDBC and MySQL to process banking operations. Commit and rollback are used during deposits, withdrawals, and transfers to help maintain database consistency.

## Future Enhancements

- Graphical user interface
- Password hashing and stronger authentication
- Admin dashboard
- PDF transaction statements
- Automated testing

## Author

**Tirupathi Rao**

GitHub: [Tirupathi-10](https://github.com/Tirupathi-10)

## Repository

[View ApexBank on GitHub](https://github.com/Tirupathi-10/ApexBank-Banking-Management-System)
