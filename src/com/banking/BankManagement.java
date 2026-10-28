package com.banking;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

public class BankManagement {

	private static final int NULL = 0;
	static Connection con = DatabaseConnection.getConnection();

	// Create Account
	public static boolean createAccount(String name, int passCode) {
		if (name.isEmpty() || passCode == NULL) {
			System.out.println("All fields are required!");
			return false;
		}

		try {
			String sql = "INSERT INTO customer(cname, balance, pass_code) VALUES (?, 1000, ?)";
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, name);
			ps.setInt(2, passCode);

			int rows = ps.executeUpdate();
			if (rows == 1) {
				System.out.println("Account created successfully! You can now login.");
				return true;
			}
		} catch (SQLIntegrityConstraintViolationException e) {
			System.out.println("Username already exists! Try another one.");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	// Login Account with 3 Attempts and 5-Minute Lock
	public static boolean loginAccount(String name, int passCode) {

		if (name == null || name.trim().isEmpty()) {
			System.out.println("Username is required!");
			return false;
		}

		String sql = "SELECT * FROM customer WHERE cname = ?";

		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, name);

			try (ResultSet rs = ps.executeQuery()) {

				if (!rs.next()) {
					System.out.println("Invalid username or password!");
					return false;
				}

				int senderAc = rs.getInt("ac_no");
				int failedAttempts = rs.getInt("failed_attempts");
				java.sql.Timestamp lockedUntil = rs.getTimestamp("locked_until");
				int storedPass = rs.getInt("pass_code");

				// Check account lock
				if (lockedUntil != null) {
					if (lockedUntil.after(new java.sql.Timestamp(System.currentTimeMillis()))) {
						System.out.println("Account temporarily locked!");
						System.out.println("Try again after: " + lockedUntil);
						return false;
					}

					// Lock expired; reset attempts
					try (PreparedStatement reset = con.prepareStatement(
							"UPDATE customer SET failed_attempts = 0, " + "locked_until = NULL WHERE ac_no = ?")) {
						reset.setInt(1, senderAc);
						reset.executeUpdate();
					}

					failedAttempts = 0;
				}

				// Check password
				if (storedPass != passCode) {
					failedAttempts++;

					if (failedAttempts >= 3) {
						try (PreparedStatement lock = con.prepareStatement("UPDATE customer SET failed_attempts = 3, "
								+ "locked_until = DATE_ADD(NOW(), INTERVAL 5 MINUTE) " + "WHERE ac_no = ?")) {
							lock.setInt(1, senderAc);
							lock.executeUpdate();
						}

						System.out.println("Too many failed attempts!");
						System.out.println("Login blocked for 5 minutes.");

					} else {
						try (PreparedStatement update = con
								.prepareStatement("UPDATE customer SET failed_attempts = ? " + "WHERE ac_no = ?")) {
							update.setInt(1, failedAttempts);
							update.setInt(2, senderAc);
							update.executeUpdate();
						}

						System.out.println("Invalid password! Attempts remaining: " + (3 - failedAttempts));
					}

					return false;
				}

				// Successful login: reset attempts
				try (PreparedStatement reset = con.prepareStatement(
						"UPDATE customer SET failed_attempts = 0, " + "locked_until = NULL WHERE ac_no = ?")) {
					reset.setInt(1, senderAc);
					reset.executeUpdate();
				}

				// Existing banking menu
				BufferedReader sc = new BufferedReader(new InputStreamReader(System.in));

				int ch;

				while (true) {
					System.out.println("\nHello, " + name + "! What would you like to do?");
					System.out.println("1) Transfer Money");
					System.out.println("2) View Balance");
					System.out.println("3. View Account Details");
					System.out.println("4) Self Deposit");
					System.out.println("5) Withdraw Money");
					System.out.println("6) Transaction History");
					System.out.println("7) Mini Statement");
					System.out.println("8) Change Password");
					System.out.println("9) Logout");
					System.out.print("Enter Choice: ");

					try {
						ch = Integer.parseInt(sc.readLine());
					} catch (NumberFormatException e) {
						System.out.println("Invalid input! Please enter a number from 1 to 9.");
						continue;
					}

					if (ch == 1) {
						System.out.print("Enter Receiver A/c No: ");
						int receiverAc = Integer.parseInt(sc.readLine());

						System.out.print("Enter Amount: ");
						int amt;

						try {
							amt = Integer.parseInt(sc.readLine());
						} catch (NumberFormatException e) {
							System.out.println("Invalid amount! Please enter numbers only.");
							continue;
						}

						if (amt <= 0) {
							System.out.println("Amount must be greater than zero!");
							continue;
						}

						if (amt <= 0) {
							System.out.println("Amount must be greater than zero!");
							continue;
						}

						if (transferMoney(senderAc, receiverAc, amt)) {
							System.out.println("Transaction successful!");
						} else {
							System.out.println("Transaction failed!");
						}

					} else if (ch == 2) {
						getBalance(senderAc);

					} else if (ch == 3) {
						viewAccountDetails(senderAc);
					} else if (ch == 4) {

						System.out.print("Enter Deposit Amount: ");
						int amount;

						try {
							amount = Integer.parseInt(sc.readLine());
						} catch (NumberFormatException e) {
							System.out.println("Invalid amount! Please enter numbers only.");
							continue;
						}

						if (amount <= 0) {
							System.out.println("Deposit amount must be greater than zero!");
							continue;
						}

						if (depositMoney(senderAc, amount)) {
							System.out.println("Amount deposited successfully!");
							getBalance(senderAc);
						} else {
							System.out.println("Deposit failed!");
						}

					} else if (ch == 5) {

						System.out.print("Enter Withdrawal Amount: ");
						int amount;

						try {
							amount = Integer.parseInt(sc.readLine());
						} catch (NumberFormatException e) {
							System.out.println("Invalid amount! Please enter numbers only.");
							continue;
						}

						if (amount <= 0) {
							System.out.println("Withdrawal amount must be greater than zero!");
							continue;
						}

						if (amount <= 0) {
							System.out.println("Deposit amount must be greater than zero!");
							continue;
						}

						if (withdrawMoney(senderAc, amount)) {
							getBalance(senderAc);
						} else {
							System.out.println("Withdrawal failed!");
						}

					} else if (ch == 6) {
						getTransactionHistory(senderAc);

					} else if (ch == 7) {
						getMiniStatement(senderAc);

					} else if (ch == 8) {
						System.out.print("Enter Current Password: ");
						int currentPass = Integer.parseInt(sc.readLine());

						System.out.print("Enter New Password: ");
						int newPass = Integer.parseInt(sc.readLine());
						if (newPass <= 0) {
							System.out.println("New password must be greater than zero!");
							continue;
						}

						if (newPass == currentPass) {
							System.out.println("New password must be different from current password!");
							continue;
						}

						if (changePassword(senderAc, currentPass, newPass)) {
							System.out.println("Password changed successfully!");
						} else {
							System.out.println("Password change failed! Check your current password.");
						}

					} else if (ch == 9) {
						System.out.println("Logged out successfully.");
						break;

					} else {
						System.out.println("Invalid option! Please select 1-9.");
					}
				}

				return true;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	// Get Balance
	public static void getBalance(int acNo) {
		try {
			String sql = "SELECT * FROM customer WHERE ac_no = ?";
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setInt(1, acNo);
			ResultSet rs = ps.executeQuery();

			System.out.println("\n-------------------------------------------------");
			System.out.printf("%12s %15s %10s\n", "Account No", "Customer Name", "Balance");

			while (rs.next()) {
				System.out.printf("%12d %15s %10d.00\n", rs.getInt("ac_no"), rs.getString("cname"),
						rs.getInt("balance"));
			}
			System.out.println("-------------------------------------------------");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void viewAccountDetails(int acNo) {
		String query = "SELECT ac_no, cname, balance FROM customer WHERE ac_no = ?";

		try (PreparedStatement ps = con.prepareStatement(query)) {
			ps.setInt(1, acNo);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					System.out.println("\n========== ACCOUNT DETAILS ==========");
					System.out.println("Account Number : " + rs.getInt("ac_no"));
					System.out.println("Account Holder : " + rs.getString("cname"));
					System.out.println("Available Balance : ₹" + rs.getDouble("balance"));
					System.out.println("=====================================");
				} else {
					System.out.println("Account not found!");
				}
			}

		} catch (SQLException e) {
			System.out.println("Unable to fetch account details: " + e.getMessage());
		}
	}

	// Transfer Money
	public static boolean transferMoney(int sender_ac, int receiver_ac, int amount) {

		if (sender_ac <= 0 || receiver_ac <= 0 || amount <= 0) {
			System.out.println("Invalid account number or amount!");
			return false;
		}

		if (sender_ac == receiver_ac) {
			System.out.println("Cannot transfer to the same account!");
			return false;
		}

		boolean oldAutoCommit = true;

		try {
			oldAutoCommit = con.getAutoCommit();
			con.setAutoCommit(false);

			// Daily Transaction Limit: Rs. 10,000
			int dailyLimit = 10000;

			String dailyTotalSql = "SELECT COALESCE(SUM(amount), 0) AS total " + "FROM transactions "
					+ "WHERE sender_ac = ? " + "AND transaction_type = 'TRANSFER' " + "AND status = 'SUCCESS' "
					+ "AND DATE(transaction_date) = CURDATE()";

			try (PreparedStatement ps = con.prepareStatement(dailyTotalSql)) {
				ps.setInt(1, sender_ac);

				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						double totalTransferred = rs.getDouble("total");

						if (totalTransferred + amount > dailyLimit) {
							con.rollback();

							System.out.println("Daily transfer limit exceeded!");
							System.out.println("Daily Limit: Rs. " + dailyLimit);
							System.out.println("Transferred Today: Rs. " + totalTransferred);
							System.out.println("Remaining Limit: Rs. " + (dailyLimit - totalTransferred));

							return false;
						}
					}
				}
			}

			// 1. Check sender account and balance
			String checkBalance = "SELECT balance FROM customer WHERE ac_no = ? FOR UPDATE";

			try (PreparedStatement ps = con.prepareStatement(checkBalance)) {
				ps.setInt(1, sender_ac);

				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) {
						System.out.println("Sender account not found!");
						con.rollback();
						return false;
					}

					if (rs.getInt("balance") < amount) {
						System.out.println("Insufficient Balance!");
						con.rollback();
						return false;
					}
				}
			}

			// 2. Debit sender
			String debit = "UPDATE customer SET balance = balance - ? WHERE ac_no = ?";

			try (PreparedStatement psDebit = con.prepareStatement(debit)) {
				psDebit.setInt(1, amount);
				psDebit.setInt(2, sender_ac);

				int debitRows = psDebit.executeUpdate();

				if (debitRows != 1) {
					con.rollback();
					System.out.println("Sender account update failed!");
					return false;
				}
			}

			// 3. Credit receiver
			String credit = "UPDATE customer SET balance = balance + ? WHERE ac_no = ?";

			try (PreparedStatement psCredit = con.prepareStatement(credit)) {
				psCredit.setInt(1, amount);
				psCredit.setInt(2, receiver_ac);

				int creditRows = psCredit.executeUpdate();

				if (creditRows != 1) {
					con.rollback();
					System.out.println("Receiver account not found!");
					return false;
				}
			}

			// 4. Commit transaction

			if (!saveTransaction(sender_ac, receiver_ac, amount, "TRANSFER")) {
				con.rollback();
				System.out.println("Transaction history save failed!");
				return false;
			}
			con.commit();
			printTransactionReceipt(sender_ac, receiver_ac, amount);
			return true;

		} catch (Exception e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				e.addSuppressed(ex);
			}

			e.printStackTrace();
			return false;

		} finally {
			try {
				con.setAutoCommit(oldAutoCommit);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	// Save Transaction
	public static boolean saveTransaction(int senderAc, Integer receiverAc, int amount, String type) {

		String sql = "INSERT INTO transactions " + "(sender_ac, receiver_ac, amount, transaction_type, status) "
				+ "VALUES (?, ?, ?, ?, ?)";

		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, senderAc);

			if (receiverAc == null) {
				ps.setNull(2, java.sql.Types.INTEGER);
			} else {
				ps.setInt(2, receiverAc);
			}

			ps.setInt(3, amount);
			ps.setString(4, type);
			ps.setString(5, "SUCCESS");

			return ps.executeUpdate() == 1;

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	// Change Password
	public static boolean changePassword(int acNo, int currentPass, int newPass) {

		if (acNo <= 0 || currentPass <= 0 || newPass <= 0) {
			System.out.println("Invalid account or password!");
			return false;
		}

		if (currentPass == newPass) {
			System.out.println("New password must be different!");
			return false;
		}

		String sql = "UPDATE customer SET pass_code = ? " + "WHERE ac_no = ? AND pass_code = ?";

		try (PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, newPass);
			ps.setInt(2, acNo);
			ps.setInt(3, currentPass);

			int rows = ps.executeUpdate();

			if (rows == 1) {
				return true;
			} else {
				System.out.println("Current password is incorrect!");
				return false;
			}

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	// View Transaction History
	public static void getTransactionHistory(int acNo) {

		String sql = "SELECT * FROM transactions " + "WHERE sender_ac = ? OR receiver_ac = ? "
				+ "ORDER BY transaction_date DESC";

		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, acNo);
			ps.setInt(2, acNo);

			try (ResultSet rs = ps.executeQuery()) {
				System.out.println("\n========== TRANSACTION HISTORY ==========");

				boolean found = false;

				while (rs.next()) {
					found = true;

					int receiver = rs.getInt("receiver_ac");
					String receiverText = rs.wasNull() ? "-" : String.valueOf(receiver);

					System.out.printf("ID: %d | Type: %s | Sender: %d | Receiver: %s | Amount: %.2f | Status: %s%n",
							rs.getInt("transaction_id"), rs.getString("transaction_type"), rs.getInt("sender_ac"),
							receiverText, rs.getDouble("amount"), rs.getString("status"));
				}

				if (!found) {
					System.out.println("No transactions found.");
				}

				System.out.println("=========================================");

			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// Mini Statement - Last 5 Transactions
	public static void getMiniStatement(int acNo) {

		String sql = "SELECT * FROM transactions " + "WHERE sender_ac = ? OR receiver_ac = ? "
				+ "ORDER BY transaction_date DESC, transaction_id DESC " + "LIMIT 5";

		try (PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, acNo);
			ps.setInt(2, acNo);

			try (ResultSet rs = ps.executeQuery()) {

				System.out.println("\n========== APEXBANK MINI STATEMENT ==========");
				System.out.printf("%-5s %-10s %-10s %-10s %-12s %-10s%n", "ID", "Type", "Sender", "Receiver", "Amount",
						"Status");

				boolean found = false;

				while (rs.next()) {
					found = true;

					int receiver = rs.getInt("receiver_ac");
					String receiverText = rs.wasNull() ? "-" : String.valueOf(receiver);

					System.out.printf("%-5d %-10s %-10d %-10s %-12.2f %-10s%n", rs.getInt("transaction_id"),
							rs.getString("transaction_type"), rs.getInt("sender_ac"), receiverText,
							rs.getDouble("amount"), rs.getString("status"));
				}

				if (!found) {
					System.out.println("No transactions found.");
				}

				System.out.println("==============================================");

			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void printTransactionReceipt(int senderAc, int receiverAc, int amount) {

		String sql = "SELECT transaction_id, transaction_date, status " + "FROM transactions "
				+ "WHERE sender_ac = ? AND receiver_ac = ? " + "AND amount = ? AND transaction_type = 'TRANSFER' "
				+ "ORDER BY transaction_id DESC LIMIT 1";

		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, senderAc);
			ps.setInt(2, receiverAc);
			ps.setInt(3, amount);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					System.out.println("\n================================");
					System.out.println("       APEXBANK RECEIPT");
					System.out.println("================================");
					System.out.println("Transaction ID : " + rs.getInt("transaction_id"));
					System.out.println("Status         : " + rs.getString("status"));
					System.out.println("Sender Account : " + senderAc);
					System.out.println("Receiver Account: " + receiverAc);
					System.out.println("Amount         : Rs. " + amount);
					System.out.println("Date           : " + rs.getTimestamp("transaction_date"));
					System.out.println("================================");
					System.out.println("Thank you for banking with ApexBank!");
				} else {
					System.out.println("Transaction receipt not found.");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// Deposit and Withdraw Receipt
	public static void printDepositWithdrawReceipt(int acNo, int amount, String type) {

		String sql = "SELECT transaction_id, transaction_date, status " + "FROM transactions " + "WHERE sender_ac = ? "
				+ "AND receiver_ac IS NULL " + "AND amount = ? " + "AND transaction_type = ? "
				+ "ORDER BY transaction_id DESC LIMIT 1";

		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, acNo);
			ps.setInt(2, amount);
			ps.setString(3, type);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					System.out.println("\n================================");
					System.out.println("       APEXBANK RECEIPT");
					System.out.println("================================");
					System.out.println("Transaction ID : " + rs.getInt("transaction_id"));
					System.out.println("Account Number : " + acNo);
					System.out.println("Transaction Type: " + type);
					System.out.println("Amount         : Rs. " + amount);
					System.out.println("Status         : " + rs.getString("status"));
					System.out.println("Date           : " + rs.getTimestamp("transaction_date"));
					System.out.println("================================");
					System.out.println("Thank you for banking with ApexBank!");
				} else {
					System.out.println("Transaction receipt not found.");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// Self Deposit
	public static boolean depositMoney(int acNo, int amount) {

		if (acNo <= 0 || amount <= 0) {
			System.out.println("Invalid account or amount!");
			return false;
		}

		boolean oldAutoCommit = true;

		try {
			oldAutoCommit = con.getAutoCommit();
			con.setAutoCommit(false);

			String sql = "UPDATE customer " + "SET balance = balance + ? WHERE ac_no = ?";

			try (PreparedStatement ps = con.prepareStatement(sql)) {
				ps.setInt(1, amount);
				ps.setInt(2, acNo);

				if (ps.executeUpdate() != 1) {
					con.rollback();
					return false;
				}
			}

			// Save deposit history
			if (!saveTransaction(acNo, null, amount, "DEPOSIT")) {
				con.rollback();
				return false;
			}

			con.commit();
			printDepositWithdrawReceipt(acNo, amount, "DEPOSIT");
			return true;

		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				e.addSuppressed(ex);
			}
			e.printStackTrace();
			return false;

		} finally {
			try {
				con.setAutoCommit(oldAutoCommit);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	// Withdraw Money
	public static boolean withdrawMoney(int acNo, int amount) {
		if (acNo <= 0 || amount <= 0) {
			System.out.println("Invalid account number or amount!");
			return false;
		}

		boolean oldAutoCommit = true;

		try {
			oldAutoCommit = con.getAutoCommit();
			con.setAutoCommit(false);

			String sql = "SELECT balance FROM customer WHERE ac_no = ? FOR UPDATE";

			try (PreparedStatement ps = con.prepareStatement(sql)) {
				ps.setInt(1, acNo);

				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) {
						System.out.println("Account not found!");
						con.rollback();
						return false;
					}

					double balance = rs.getDouble("balance");

					if (balance < amount) {
						System.out.println("Insufficient balance!");
						con.rollback();
						return false;
					}
				}
			}

			String updateSql = "UPDATE customer SET balance = balance - ? WHERE ac_no = ?";

			try (PreparedStatement ps = con.prepareStatement(updateSql)) {
				ps.setInt(1, amount);
				ps.setInt(2, acNo);

				if (ps.executeUpdate() != 1) {
					con.rollback();
					return false;
				}
			}

			if (!saveTransaction(acNo, null, amount, "WITHDRAW")) {
				con.rollback();
				System.out.println("Transaction history save failed!");
				return false;
			}

			con.commit();
			printDepositWithdrawReceipt(acNo, amount, "WITHDRAW");

			System.out.println("Withdrawal successful!");
			return true;

		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}

			e.printStackTrace();
			return false;

		} finally {
			try {
				con.setAutoCommit(oldAutoCommit);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

}