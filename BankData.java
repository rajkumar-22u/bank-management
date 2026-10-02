import java.io.*;
import java.util.*;

public class BankData {

    private static final String DATA_FILE = "bank_data.txt";
    private static final int MAX_USERS = 50;

    // Customer data
    static class Customer {
        String accountNumber;
        String name;
        String phone;
        String pin;
        double balance;
        String createdDate;

        Customer(String accountNumber, String name, String phone,
                 String pin, double balance, String createdDate) {

            this.accountNumber = accountNumber;
            this.name = name;
            this.phone = phone;
            this.pin = pin;
            this.balance = balance;
            this.createdDate = createdDate;
        }
    }

    // Transaction data
    static class Transaction {
        String accountNumber;
        String type;
        double amount;
        double balance;
        String date;

        Transaction(String accountNumber, String type,
                    double amount, double balance, String date) {

            this.accountNumber = accountNumber;
            this.type = type;
            this.amount = amount;
            this.balance = balance;
            this.date = date;
        }
    }

    static ArrayList<Customer> customers = new ArrayList<>();
    static ArrayList<Transaction> transactions = new ArrayList<>();

    // Load saved data
    public static void loadData() {

        File file = new File(DATA_FILE);

        if (!file.exists()) {
            return;
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));

            String line;

            while ((line = br.readLine()) != null) {

                if (line.startsWith("CUSTOMER|")) {

                    String[] d = line.split("\\|");

                    if (d.length >= 7) {

                        Customer c = new Customer(
                                d[1],
                                d[2],
                                d[3],
                                d[4],
                                Double.parseDouble(d[5]),
                                d[6]
                        );

                        customers.add(c);
                    }
                }

                else if (line.startsWith("TRANSACTION|")) {

                    String[] d = line.split("\\|");

                    if (d.length >= 6) {

                        Transaction t = new Transaction(
                                d[1],
                                d[2],
                                Double.parseDouble(d[3]),
                                Double.parseDouble(d[4]),
                                d[5]
                        );

                        transactions.add(t);
                    }
                }
            }

            br.close();

        } catch (Exception e) {

            System.out.println("Data loading error: " + e.getMessage());
        }
    }

    // Save all data
    public static void saveData() {

        try {

            PrintWriter pw = new PrintWriter(
                    new FileWriter(DATA_FILE)
            );

            for (Customer c : customers) {

                pw.println(
                        "CUSTOMER|" +
                        c.accountNumber + "|" +
                        c.name + "|" +
                        c.phone + "|" +
                        c.pin + "|" +
                        c.balance + "|" +
                        c.createdDate
                );
            }

            for (Transaction t : transactions) {

                pw.println(
                        "TRANSACTION|" +
                        t.accountNumber + "|" +
                        t.type + "|" +
                        t.amount + "|" +
                        t.balance + "|" +
                        t.date
                );
            }

            pw.close();

        } catch (Exception e) {

            System.out.println("Data saving error: " + e.getMessage());
        }
    }

    // Check maximum users
    public static boolean canCreateAccount() {

        return customers.size() < MAX_USERS;
    }

    // Generate account number
    public static String generateAccountNumber() {

        int number = 100001 + customers.size();

        return "AC" + number;
    }

    // Find customer
    public static Customer findCustomer(String accountNumber) {

        for (Customer c : customers) {

            if (c.accountNumber.equals(accountNumber)) {
                return c;
            }
        }

        return null;
    }

    // Add customer
    public static void addCustomer(Customer customer) {

        customers.add(customer);

        saveData();
    }

    // Add transaction
    public static void addTransaction(
            String accountNumber,
            String type,
            double amount,
            double balance,
            String date) {

        transactions.add(
                new Transaction(
                        accountNumber,
                        type,
                        amount,
                        balance,
                        date
                )
        );

        saveData();
    }

    // Get transactions of customer
    public static ArrayList<Transaction> getTransactions(
            String accountNumber) {

        ArrayList<Transaction> result = new ArrayList<>();

        for (Transaction t : transactions) {

            if (t.accountNumber.equals(accountNumber)) {

                result.add(t);
            }
        }

        return result;
    }

    // Total bank balance
    public static double getTotalBalance() {

        double total = 0;

        for (Customer c : customers) {

            total += c.balance;
        }

        return total;
    }

    // Total customers
    public static int getTotalCustomers() {

        return customers.size();
    }
}