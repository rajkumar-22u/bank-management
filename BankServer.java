import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class BankServer {

    static final int PORT = 8080;

    // Admin credentials
    static final String ADMIN_ID = "admin";
    static final String ADMIN_PIN = "1234";

    public static void main(String[] args) throws Exception {

        // Load previously saved data
        BankData.loadData();

        HttpServer server = HttpServer.create(
                new InetSocketAddress(PORT), 0
        );

        // Home page and static files
        server.createContext("/", BankServer::handleStatic);

        // Customer APIs
        server.createContext("/api/create", BankServer::createAccount);
        server.createContext("/api/login", BankServer::customerLogin);
        server.createContext("/api/deposit", BankServer::deposit);
        server.createContext("/api/withdraw", BankServer::withdraw);
        server.createContext("/api/balance", BankServer::balance);
        server.createContext("/api/transactions", BankServer::transactions);

        // Admin APIs
        server.createContext("/api/admin-login", BankServer::adminLogin);
        server.createContext("/api/customers", BankServer::customers);

        server.setExecutor(null);
        server.start();

        System.out.println("--------------------------------------");
        System.out.println(" BANK MANAGEMENT SYSTEM");
        System.out.println("--------------------------------------");
        System.out.println("Server started successfully!");
        System.out.println("Open: http://localhost:8080");
        System.out.println("Admin: http://localhost:8080/admin.html");
        System.out.println("--------------------------------------");
    }


    // =========================================================
    // CREATE ACCOUNT
    // =========================================================

    static void createAccount(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            send(exchange, "{\"success\":false,\"message\":\"POST required\"}");
            return;
        }

        Map<String, String> data = getRequestData(exchange);

        String name = data.get("name");
        String phone = data.get("phone");
        String pin = data.get("pin");
        String confirmPin = data.get("confirmPin");
        String depositText = data.get("deposit");

        if (name == null || phone == null || pin == null ||
                confirmPin == null || depositText == null) {

            send(exchange,
                    "{\"success\":false,\"message\":\"All fields are required\"}");
            return;
        }

        if (name.trim().isEmpty() || phone.trim().isEmpty()) {

            send(exchange,
                    "{\"success\":false,\"message\":\"Name and phone are required\"}");
            return;
        }

        if (!pin.matches("\\d{4}")) {

            send(exchange,
                    "{\"success\":false,\"message\":\"PIN must be exactly 4 digits\"}");
            return;
        }

        if (!pin.equals(confirmPin)) {

            send(exchange,
                    "{\"success\":false,\"message\":\"PIN does not match\"}");
            return;
        }

        double deposit;

        try {
            deposit = Double.parseDouble(depositText);
        } catch (Exception e) {

            send(exchange,
                    "{\"success\":false,\"message\":\"Invalid deposit amount\"}");
            return;
        }

        if (deposit < 0) {

            send(exchange,
                    "{\"success\":false,\"message\":\"Invalid deposit amount\"}");
            return;
        }

        if (!BankData.canCreateAccount()) {

            send(exchange,
                    "{\"success\":false,\"message\":\"Maximum 50 accounts reached\"}");
            return;
        }

        String accountNumber = BankData.generateAccountNumber();

        String date = getDate();

        BankData.Customer customer =
                new BankData.Customer(
                        accountNumber,
                        name.trim(),
                        phone.trim(),
                        pin,
                        deposit,
                        date
                );

        BankData.addCustomer(customer);

        // Initial deposit transaction
        if (deposit > 0) {

            BankData.addTransaction(
                    accountNumber,
                    "Initial Deposit",
                    deposit,
                    deposit,
                    date
            );
        }

        String response =
                "{"
                        + "\"success\":true,"
                        + "\"message\":\"Account created successfully\","
                        + "\"accountNumber\":\"" + escape(accountNumber) + "\","
                        + "\"name\":\"" + escape(name) + "\","
                        + "\"balance\":" + deposit
                        + "}";

        send(exchange, response);
    }


    // =========================================================
    // CUSTOMER LOGIN
    // =========================================================

    static void customerLogin(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            send(exchange, "{\"success\":false,\"message\":\"POST required\"}");
            return;
        }

        Map<String, String> data = getRequestData(exchange);

        String account = data.get("account");
        String pin = data.get("pin");

        if (account == null || pin == null) {

            send(exchange,
                    "{\"success\":false,\"message\":\"Account and PIN required\"}");
            return;
        }

        BankData.Customer customer =
                BankData.findCustomer(account.trim());

        if (customer == null) {

            send(exchange,
                    "{\"success\":false,\"message\":\"Account not found\"}");
            return;
        }

        if (!customer.pin.equals(pin)) {

            send(exchange,
                    "{\"success\":false,\"message\":\"Incorrect PIN\"}");
            return;
        }

        String response =
                "{"
                        + "\"success\":true,"
                        + "\"message\":\"Login successful\","
                        + "\"accountNumber\":\"" + escape(customer.accountNumber) + "\","
                        + "\"name\":\"" + escape(customer.name) + "\","
                        + "\"balance\":" + customer.balance
                        + "}";

        send(exchange, response);
    }


    // =========================================================
    // DEPOSIT
    // =========================================================

    static void deposit(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            send(exchange, "{\"success\":false,\"message\":\"POST required\"}");
            return;
        }

        Map<String, String> data = getRequestData(exchange);

        String account = data.get("account");
        String amountText = data.get("amount");

        if (account == null || amountText == null) {

            send(exchange,
                    "{\"success\":false,\"message\":\"Account and amount required\"}");
            return;
        }

        double amount;

        try {
            amount = Double.parseDouble(amountText);
        } catch (Exception e) {

            send(exchange,
                    "{\"success\":false,\"message\":\"Invalid amount\"}");
            return;
        }

        if (amount <= 0) {

            send(exchange,
                    "{\"success\":false,\"message\":\"Amount must be greater than 0\"}");
            return;
        }

        BankData.Customer customer =
                BankData.findCustomer(account);

        if (customer == null) {

            send(exchange,
                    "{\"success\":false,\"message\":\"Account not found\"}");
            return;
        }

        customer.balance += amount;

        String date = getDate();

        BankData.addTransaction(
                account,
                "Deposit",
                amount,
                customer.balance,
                date
        );

        send(exchange,
                "{"
                        + "\"success\":true,"
                        + "\"message\":\"Money deposited successfully\","
                        + "\"balance\":" + customer.balance
                        + "}"
        );
    }


    // =========================================================
    // WITHDRAW
    // =========================================================

    static void withdraw(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            send(exchange, "{\"success\":false,\"message\":\"POST required\"}");
            return;
        }

        Map<String, String> data = getRequestData(exchange);

        String account = data.get("account");
        String amountText = data.get("amount");

        if (account == null || amountText == null) {

            send(exchange,
                    "{\"success\":false,\"message\":\"Account and amount required\"}");
            return;
        }

        double amount;

        try {
            amount = Double.parseDouble(amountText);
        } catch (Exception e) {

            send(exchange,
                    "{\"success\":false,\"message\":\"Invalid amount\"}");
            return;
        }

        if (amount <= 0) {

            send(exchange,
                    "{\"success\":false,\"message\":\"Amount must be greater than 0\"}");
            return;
        }

        BankData.Customer customer =
                BankData.findCustomer(account);

        if (customer == null) {

            send(exchange,
                    "{\"success\":false,\"message\":\"Account not found\"}");
            return;
        }

        if (amount > customer.balance) {

            send(exchange,
                    "{\"success\":false,\"message\":\"Insufficient balance\"}");
            return;
        }

        customer.balance -= amount;

        String date = getDate();

        BankData.addTransaction(
                account,
                "Withdrawal",
                amount,
                customer.balance,
                date
        );

        send(exchange,
                "{"
                        + "\"success\":true,"
                        + "\"message\":\"Money withdrawn successfully\","
                        + "\"balance\":" + customer.balance
                        + "}"
        );
    }


    // =========================================================
    // BALANCE
    // =========================================================

    static void balance(HttpExchange exchange) throws IOException {

        Map<String, String> data = getRequestData(exchange);

        String account = data.get("account");

        if (account == null) {

            send(exchange,
                    "{\"success\":false,\"message\":\"Account required\"}");
            return;
        }

        BankData.Customer customer =
                BankData.findCustomer(account);

        if (customer == null) {

            send(exchange,
                    "{\"success\":false,\"message\":\"Account not found\"}");
            return;
        }

        send(exchange,
                "{"
                        + "\"success\":true,"
                        + "\"balance\":" + customer.balance
                        + "}"
        );
    }


    // =========================================================
    // TRANSACTIONS
    // =========================================================

    static void transactions(HttpExchange exchange) throws IOException {

        Map<String, String> data = getRequestData(exchange);

        String account = data.get("account");

        if (account == null) {

            send(exchange,
                    "{\"success\":false,\"message\":\"Account required\"}");
            return;
        }

        ArrayList<BankData.Transaction> list =
                BankData.getTransactions(account);

        StringBuilder json = new StringBuilder();

        json.append("{\"success\":true,\"transactions\":[");

        for (int i = 0; i < list.size(); i++) {

            BankData.Transaction t = list.get(i);

            if (i > 0) {
                json.append(",");
            }

            json.append("{")
                    .append("\"type\":\"")
                    .append(escape(t.type))
                    .append("\",")
                    .append("\"amount\":")
                    .append(t.amount)
                    .append(",")
                    .append("\"balance\":")
                    .append(t.balance)
                    .append(",")
                    .append("\"date\":\"")
                    .append(escape(t.date))
                    .append("\"")
                    .append("}");
        }

        json.append("]}");

        send(exchange, json.toString());
    }


    // =========================================================
    // ADMIN LOGIN
    // =========================================================

    static void adminLogin(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            send(exchange, "{\"success\":false,\"message\":\"POST required\"}");
            return;
        }

        Map<String, String> data = getRequestData(exchange);

        String id = data.get("id");
        String pin = data.get("pin");

        if (ADMIN_ID.equals(id) && ADMIN_PIN.equals(pin)) {

            send(exchange,
                    "{\"success\":true,\"message\":\"Admin login successful\"}");

        } else {

            send(exchange,
                    "{\"success\":false,\"message\":\"Invalid Admin ID or PIN\"}");
        }
    }


    // =========================================================
    // ADMIN - ALL CUSTOMERS
    // =========================================================

    static void customers(HttpExchange exchange) throws IOException {

        StringBuilder json = new StringBuilder();

        json.append("{")
                .append("\"success\":true,")
                .append("\"totalCustomers\":")
                .append(BankData.getTotalCustomers())
                .append(",")
                .append("\"maxCustomers\":50,")
                .append("\"totalBalance\":")
                .append(BankData.getTotalBalance())
                .append(",")
                .append("\"customers\":[");

        for (int i = 0; i < BankData.customers.size(); i++) {

            BankData.Customer c =
                    BankData.customers.get(i);

            if (i > 0) {
                json.append(",");
            }

            json.append("{")
                    .append("\"accountNumber\":\"")
                    .append(escape(c.accountNumber))
                    .append("\",")
                    .append("\"name\":\"")
                    .append(escape(c.name))
                    .append("\",")
                    .append("\"phone\":\"")
                    .append(escape(c.phone))
                    .append("\",")
                    .append("\"balance\":")
                    .append(c.balance)
                    .append(",")
                    .append("\"createdDate\":\"")
                    .append(escape(c.createdDate))
                    .append("\"")
                    .append("}");
        }

        json.append("]}");

        send(exchange, json.toString());
    }


    // =========================================================
    // STATIC FILE SERVER
    // =========================================================

    static void handleStatic(HttpExchange exchange) throws IOException {

        String path = exchange.getRequestURI().getPath();

        if (path.equals("/")) {
            path = "/index.html";
        }

        if (path.equals("/admin")) {
            path = "/admin.html";
        }

        File file = new File("web" + path);

        if (!file.exists() || file.isDirectory()) {

            exchange.sendResponseHeaders(404, 0);

            OutputStream os = exchange.getResponseBody();

            os.write("404 - File Not Found".getBytes(StandardCharsets.UTF_8));

            os.close();

            return;
        }

        String contentType = "text/plain";

        if (path.endsWith(".html")) {
            contentType = "text/html";
        } else if (path.endsWith(".css")) {
            contentType = "text/css";
        } else if (path.endsWith(".js")) {
            contentType = "application/javascript";
        }

        exchange.getResponseHeaders()
                .set("Content-Type", contentType + "; charset=UTF-8");

        byte[] bytes = readFile(file);

        exchange.sendResponseHeaders(200, bytes.length);

        OutputStream os = exchange.getResponseBody();

        os.write(bytes);

        os.close();
    }


    // =========================================================
    // READ FILE
    // =========================================================

    static byte[] readFile(File file) throws IOException {

        FileInputStream fis = new FileInputStream(file);

        ByteArrayOutputStream bos =
                new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];

        int length;

        while ((length = fis.read(buffer)) != -1) {

            bos.write(buffer, 0, length);
        }

        fis.close();

        return bos.toByteArray();
    }


    // =========================================================
    // REQUEST DATA
    // =========================================================

    static Map<String, String> getRequestData(
            HttpExchange exchange) throws IOException {

        String method =
                exchange.getRequestMethod();

        String query = "";

        if (method.equalsIgnoreCase("GET")) {

            query = exchange.getRequestURI()
                    .getRawQuery();

        } else {

            InputStream input =
                    exchange.getRequestBody();

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];

            int length;

            while ((length = input.read(buffer)) != -1) {

                output.write(buffer, 0, length);
            }

            query = output.toString(
                    StandardCharsets.UTF_8
            );
        }

        Map<String, String> result =
                new HashMap<>();

        if (query == null || query.isEmpty()) {
            return result;
        }

        String[] pairs = query.split("&");

        for (String pair : pairs) {

            String[] parts = pair.split("=", 2);

            if (parts.length == 2) {

                String key =
                        URLDecoder.decode(
                                parts[0],
                                StandardCharsets.UTF_8
                        );

                String value =
                        URLDecoder.decode(
                                parts[1],
                                StandardCharsets.UTF_8
                        );

                result.put(key, value);
            }
        }

        return result;
    }


    // =========================================================
    // SEND RESPONSE
    // =========================================================

    static void send(
            HttpExchange exchange,
            String response) throws IOException {

        exchange.getResponseHeaders()
                .set("Content-Type",
                        "application/json; charset=UTF-8");

        byte[] bytes =
                response.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(
                200,
                bytes.length
        );

        OutputStream os =
                exchange.getResponseBody();

        os.write(bytes);

        os.close();
    }


    // =========================================================
    // DATE
    // =========================================================

    static String getDate() {

        return new SimpleDateFormat(
                "dd-MM-yyyy HH:mm:ss"
        ).format(new Date());
    }


    // =========================================================
    // JSON ESCAPE
    // =========================================================

    static String escape(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}