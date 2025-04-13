import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        DEPOSIT, WITHDRAWAL, TRANSFER
    }

    private Type type;
    private double amount;
    private String sourceAccountNumber;
    private String targetAccountNumber; 
    private Date timestamp;

    public Transaction(Type type, double amount, String sourceAccountNumber) {
        this.type = type;
        this.amount = amount;
        this.sourceAccountNumber = sourceAccountNumber;
        this.timestamp = new Date();
    }

    public Transaction(Type type, double amount, String sourceAccountNumber, String targetAccountNumber) {
        this(type, amount, sourceAccountNumber);
        this.targetAccountNumber = targetAccountNumber;
    }

    public Type getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public String getTargetAccountNumber() {
        return targetAccountNumber;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = dateFormat.format(timestamp);

        switch (type) {
            case DEPOSIT:
                return String.format("[%s] DEPOSIT: $%.2f", formattedDate, amount);
            case WITHDRAWAL:
                return String.format("[%s] WITHDRAWAL: $%.2f", formattedDate, amount);
            case TRANSFER:
                return String.format("[%s] TRANSFER: $%.2f to account %s", formattedDate, amount, targetAccountNumber);
            default:
                return "Unknown transaction";
        }
    }
}

class BankAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    private String accountNumber;
    private String hashedPin;
    private double balance;
    private String userName;
    private List<Transaction> transactionHistory;
    private int failedPinAttempts;
    private boolean locked;
    private Date lastActivity;

    public BankAccount(String accountNumber, String userName, String pin, double initialBalance) {
        this.accountNumber = accountNumber;
        this.userName = userName;
        this.hashedPin = hashPin(pin);
        this.balance = initialBalance;
        this.transactionHistory = new ArrayList<>();
        this.failedPinAttempts = 0;
        this.locked = false;
        this.lastActivity = new Date();
    }

    private String hashPin(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pin.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {

            System.err.println("Warning: Using plain text PIN due to hashing failure");
            return pin;
        }
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getUserName() {
        return userName;
    }

    public boolean validatePin(String inputPin) {
        if (locked) {
            return false;
        }

        boolean isValid = hashPin(inputPin).equals(hashedPin);

        if (!isValid) {
            failedPinAttempts++;
            if (failedPinAttempts >= 3) {
                locked = true;
            }
        } else {
            failedPinAttempts = 0;
        }

        updateLastActivity();
        return isValid;
    }

    public boolean isLocked() {
        return locked;
    }

    public void unlockAccount() {
        locked = false;
        failedPinAttempts = 0;
    }

    public boolean changePin(String oldPin, String newPin) {
        if (validatePin(oldPin)) {
            hashedPin = hashPin(newPin);
            return true;
        }
        return false;
    }

    public double getBalance() {
        updateLastActivity();
        return balance;
    }

    public boolean deposit(double amount) {
        if (amount <= 0) {
            return false;
        }

        balance += amount;
        transactionHistory.add(new Transaction(Transaction.Type.DEPOSIT, amount, accountNumber));
        updateLastActivity();
        return true;
    }

    public boolean withdraw(double amount) {
        if (amount <= 0) {
            return false;
        }

        if (amount > balance) {
            return false;
        }

        balance -= amount;
        transactionHistory.add(new Transaction(Transaction.Type.WITHDRAWAL, amount, accountNumber));
        updateLastActivity();
        return true;
    }

    public boolean transfer(BankAccount targetAccount, double amount) {
        if (targetAccount == null || amount <= 0 || amount > balance) {
            return false;
        }

        balance -= amount;
        targetAccount.balance += amount;

        transactionHistory.add(new Transaction(Transaction.Type.TRANSFER, amount, 
                                              accountNumber, targetAccount.accountNumber));
        targetAccount.transactionHistory.add(new Transaction(Transaction.Type.DEPOSIT, amount, 
                                                           targetAccount.accountNumber, accountNumber));

        updateLastActivity();
        targetAccount.updateLastActivity();
        return true;
    }

    public List<Transaction> getTransactionHistory() {
        updateLastActivity();
        return new ArrayList<>(transactionHistory);
    }

    public List<Transaction> getRecentTransactions(int count) {
        updateLastActivity();
        int size = transactionHistory.size();
        if (size <= count) {
            return new ArrayList<>(transactionHistory);
        }

        return new ArrayList<>(transactionHistory.subList(size - count, size));
    }

    public Date getLastActivity() {
        return lastActivity;
    }

    private void updateLastActivity() {
        lastActivity = new Date();
    }
}

class ATM {
    private static final String DATA_FILE = "atm_data.ser";
    private static final int SESSION_TIMEOUT_SECONDS = 60;

    private Map<String, BankAccount> accounts;
    private BankAccount currentAccount;
    private Scanner scanner;
    private boolean isSessionActive;
    private Timer sessionTimer;

    public ATM() {
        this.scanner = new Scanner(System.in);
        this.isSessionActive = false;
        loadAccounts();

        if (accounts.isEmpty()) {
            initializeSampleAccounts();
        }
    }

    private void initializeSampleAccounts() {
        System.out.println("Initializing sample accounts...");
        addAccount(new BankAccount("123456", "John Doe", "1234", 5000.0));
        addAccount(new BankAccount("789012", "Jane Smith", "5678", 7500.0));
        saveAccounts();
    }

    @SuppressWarnings("unchecked")
    private void loadAccounts() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            accounts = (Map<String, BankAccount>) ois.readObject();
            System.out.println("Accounts loaded successfully.");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No existing accounts found. Creating new accounts database.");
            accounts = new HashMap<>();
        }
    }

    private void saveAccounts() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(accounts);
            System.out.println("Accounts saved successfully.");
        } catch (IOException e) {
            System.err.println("Error saving accounts: " + e.getMessage());
        }
    }

    public void addAccount(BankAccount account) {
        accounts.put(account.getAccountNumber(), account);
    }

    private void createNewAccount() {
        System.out.println("\n=== Create New Account ===");

        System.out.print("Enter your full name: ");
        String userName = scanner.nextLine().trim();

        String accountNumber;
        do {
            accountNumber = generateAccountNumber();
        } while (accounts.containsKey(accountNumber));

        System.out.print("Create a PIN (at least 4 digits): ");
        String pin = scanner.nextLine().trim();

        while (pin.length() < 4 || !pin.matches("\\d+")) {
            System.out.println("PIN must be at least 4 digits long and contain only numbers.");
            System.out.print("Create a PIN: ");
            pin = scanner.nextLine().trim();
        }

        System.out.print("Enter initial deposit amount: $");
        double initialDeposit = getDoubleInput();

        while (initialDeposit < 100) {
            System.out.println("Initial deposit must be at least $100.");
            System.out.print("Enter initial deposit amount: $");
            initialDeposit = getDoubleInput();
        }

        BankAccount newAccount = new BankAccount(accountNumber, userName, pin, initialDeposit);
        addAccount(newAccount);

        System.out.println("\nAccount created successfully!");
        System.out.println("Your account number is: " + accountNumber);
        System.out.println("Please remember your account number and PIN for future logins.");
    }

    private String generateAccountNumber() {

        return String.format("%06d", (int)(Math.random() * 900000) + 100000);
    }

    public void start() {
        System.out.println("=== Welcome to the ATM System ===");

        OUTER:
        while (true) {
            try {
                if (!isSessionActive) {
                    System.out.println("\nAccounts in system: " + accounts.size());
                    System.out.println("\n1. Login");
                    System.out.println("2. Create New Account");
                    System.out.println("3. Exit");
                    System.out.print("\nEnter your choice (1-3): ");
                    int choice = getIntInput();
                    switch (choice) {
                        case 1 -> authenticateUser();
                        case 2 -> {
                            createNewAccount();
                            saveAccounts();
                        }
                        case 3 -> {
                            System.out.println("\nThank you for using our ATM. Goodbye!");
                            scanner.close();
                            System.exit(1);
                        }
                        default -> System.out.println("\nInvalid option. Please try again.");
                    }
                } else {
                    displayMenu();
                    int choice = getIntInput();

                    switch (choice) {
                        case 1 -> checkBalance();
                        case 2 -> deposit();
                        case 3 -> withdraw();
                        case 4 -> transfer();
                        case 5 -> showTransactionHistory();
                        case 6 -> changePin();
                        case 7 -> logout();
                        case 8 -> {
                            System.out.println("\nThank you for using our ATM. Goodbye!");
                            saveAccounts();
                            scanner.close();
                            return;
                        }
                        default -> System.out.println("\nInvalid option. Please try again.");
                    }
                }
            }catch (Exception e) {
                System.err.println("An error occurred: " + e.getMessage());
                e.printStackTrace();
            }
            if (isSessionActive) {

                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
            saveAccounts();
        }

        //scanner.close();
    }

    private void authenticateUser() {
        System.out.println("\n=== Login ===");
        System.out.print("Enter Account Number: ");
        String accountNumber = scanner.nextLine().trim();

        BankAccount account = accounts.get(accountNumber);
        if (account == null) {
            System.out.println("Account not found.");
            System.out.print("Are you a new customer? (y/n): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                System.out.println("Let's create a new account for you.");
                createNewAccount();
            } else {
                System.out.println("Please try again with a valid account number.");
            }
            return;
        }

        if (account.isLocked()) {
            System.out.println("This account is locked due to too many failed attempts.");
            System.out.print("Would you like to unlock it? (admin function)2 (y/n): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                System.out.print("Enter admin password: ");
                String adminPassword = scanner.nextLine().trim();
                if (adminPassword.equals("admin123")) {  
                    account.unlockAccount();
                    System.out.println("Account unlocked successfully.");
                } else {
                    System.out.println("Invalid admin password. Account remains locked.");
                    return;
                }
            } else {
                return;
            }
        }

        for (int attempts = 1; attempts <= 3; attempts++) {
            System.out.print("Enter PIN: ");
            String pin = scanner.nextLine().trim();

            if (account.validatePin(pin)) {
                currentAccount = account;
                isSessionActive = true;
                System.out.println("\nWelcome, " + account.getUserName() + "!");
                startSessionTimer();
                return;
            } else {
                int remainingAttempts = 3 - attempts;
                if (remainingAttempts > 0) {
                    System.out.println("Incorrect PIN. Attempts remaining: " + remainingAttempts);
                } else {
                    System.out.println("Too many incorrect attempts. Account is now locked.");
                    return;
                }
            }
        }
    }

    private void startSessionTimer() {
        if (sessionTimer != null) {
            sessionTimer.cancel();
        }

        sessionTimer = new Timer();
        sessionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isSessionActive) {
                    System.out.println("\nSession timed out due to inactivity. Logging out...");
                    isSessionActive = false;
                    currentAccount = null;
                }
            }
        }, SESSION_TIMEOUT_SECONDS * 1000);
    }

    private void resetSessionTimer() {
        if (isSessionActive) {
            startSessionTimer();
        }
    }

    private void displayMenu() {
        System.out.println("\n=== ATM Menu ===");
        System.out.println("1. Check Balance");
        System.out.println("2. Deposit");
        System.out.println("3. Withdraw");
        System.out.println("4. Transfer Money");
        System.out.println("5. Transaction History");
        System.out.println("6. Change PIN");
        System.out.println("7. Logout");
        System.out.println("8. Exit");
        System.out.print("\nEnter your choice (1-8): ");
    }

    private int getIntInput() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1; 
        }
    }

    private double getDoubleInput() {
        try {
            return Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1; 
        }
    }

    private void checkBalance() {
        resetSessionTimer();
        System.out.println("\n=== Balance Inquiry ===");
        System.out.printf("Current Balance: $%.2f\n", currentAccount.getBalance());
        printReceipt("BALANCE INQUIRY", null);
    }

    private void deposit() {
        resetSessionTimer();
        System.out.println("\n=== Deposit ===");
        System.out.print("Enter amount to deposit: $");

        double amount = getDoubleInput();

        if (amount <= 0) {
            System.out.println("Invalid amount. Please enter a positive value.");
            return;
        }

        if (currentAccount.deposit(amount)) {
            System.out.printf("Successfully deposited: $%.2f\n", amount);
            System.out.printf("New Balance: $%.2f\n", currentAccount.getBalance());
            printReceipt("DEPOSIT", amount);
        } else {
            System.out.println("Deposit failed. Please try again.");
        }
    }

    private void withdraw() {
        resetSessionTimer();
        System.out.println("\n=== Withdrawal ===");
        System.out.printf("Available Balance: $%.2f\n", currentAccount.getBalance());
        System.out.print("Enter amount to withdraw: $");

        double amount = getDoubleInput();

        if (amount <= 0) {
            System.out.println("Invalid amount. Please enter a positive value.");
            return;
        }

        if (amount > currentAccount.getBalance()) {
            System.out.println("Insufficient funds. Withdrawal cancelled.");
            return;
        }

        if (currentAccount.withdraw(amount)) {
            System.out.printf("Successfully withdrawn: $%.2f\n", amount);
            System.out.printf("Remaining Balance: $%.2f\n", currentAccount.getBalance());
            printReceipt("WITHDRAWAL", amount);
        } else {
            System.out.println("Withdrawal failed. Please try again.");
        }
    }

    private void transfer() {
        resetSessionTimer();
        System.out.println("\n=== Transfer Money ===");
        System.out.printf("Available Balance: $%.2f\n", currentAccount.getBalance());

        System.out.print("Enter recipient's account number: ");
        String targetAccountNumber = scanner.nextLine().trim();

        if (targetAccountNumber.equals(currentAccount.getAccountNumber())) {
            System.out.println("Cannot transfer to same account.");
            return;
        }

        BankAccount targetAccount = accounts.get(targetAccountNumber);
        if (targetAccount == null) {
            System.out.println("Target account not found. Transfer cancelled.");
            return;
        }

        System.out.print("Enter amount to transfer: $");
        double amount = getDoubleInput();

        if (amount <= 0) {
            System.out.println("Invalid amount. Please enter a positive value.");
            return;
        }

        if (amount > currentAccount.getBalance()) {
            System.out.println("Insufficient funds. Transfer cancelled.");
            return;
        }

        if (currentAccount.transfer(targetAccount, amount)) {
            System.out.printf("Successfully transferred $%.2f to %s\n", 
                             amount, targetAccount.getUserName());
            System.out.printf("Remaining Balance: $%.2f\n", currentAccount.getBalance());
            printReceipt("TRANSFER", amount);
        } else {
            System.out.println("Transfer failed. Please try again.");
        }
    }

    private void showTransactionHistory() {
        resetSessionTimer();
        System.out.println("\n=== Transaction History ===");
        System.out.println("1. Recent Transactions (last 5)");
        System.out.println("2. All Transactions");
        System.out.print("\nEnter your choice (1-2): ");

        int choice = getIntInput();

        if (choice == 1) {
            List<Transaction> recentTransactions = currentAccount.getRecentTransactions(5);
            if (recentTransactions.isEmpty()) {
                System.out.println("No recent transactions found.");
            } else {
                System.out.println("\nRecent Transactions:");
                int i = 1;
                for (Transaction transaction : recentTransactions) {
                    System.out.println(i++ + ". " + transaction);
                }
            }
        } else if (choice == 2) {
            List<Transaction> allTransactions = currentAccount.getTransactionHistory();
            if (allTransactions.isEmpty()) {
                System.out.println("No transactions found.");
            } else {
                System.out.println("\nAll Transactions:");
                int i = 1;
                for (Transaction transaction : allTransactions) {
                    System.out.println(i++ + ". " + transaction);
                }
            }
        } else {
            System.out.println("Invalid choice.");
        }
    }

    private void changePin() {
        resetSessionTimer();
        System.out.println("\n=== Change PIN ===");
        System.out.print("Enter current PIN: ");
        String currentPin = scanner.nextLine().trim();

        if (!currentAccount.validatePin(currentPin)) {
            System.out.println("Incorrect PIN. Operation cancelled.");
            return;
        }

        System.out.print("Enter new PIN: ");
        String newPin = scanner.nextLine().trim();

        if (newPin.length() < 4) {
            System.out.println("PIN must be at least 4 digits long. Operation cancelled.");
            return;
        }

        System.out.print("Confirm new PIN: ");
        String confirmPin = scanner.nextLine().trim();

        if (!newPin.equals(confirmPin)) {
            System.out.println("PINs do not match. Operation cancelled.");
            return;
        }

        if (currentAccount.changePin(currentPin, newPin)) {
            System.out.println("PIN changed successfully.");
        } else {
            System.out.println("Failed to change PIN. Please try again.");
        }
    }

    private void logout() {
        if (sessionTimer != null) {
            sessionTimer.cancel();
        }

        System.out.println("\nThank you for using our ATM, " + currentAccount.getUserName() + ".");
        isSessionActive = false;
        currentAccount = null;
    }

    private void printReceipt(String transactionType, Double amount) {
        System.out.println("\n======== RECEIPT ========");
        System.out.println("Transaction Type: " + transactionType);
        System.out.println("Account Number: " + currentAccount.getAccountNumber());
        System.out.println("Date/Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        if (amount != null) {
            System.out.printf("Amount: $%.2f\n", amount);
        }

        System.out.printf("Current Balance: $%.2f\n", currentAccount.getBalance());
        System.out.println("==========================");

        System.out.print("Would you like to print this receipt? (y/n): ");
        String choice = scanner.nextLine().trim();


        if (choice.equalsIgnoreCase("y")) {
            System.out.println("Receipt printed successfully.");
        }
    }
}

public class ATMInterface {
    public static void main(String[] args) {
        System.out.println("Starting ATM System...");
        ATM atm = new ATM();
        atm.start();
    }
}