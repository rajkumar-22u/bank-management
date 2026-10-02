// =====================================================
// BANK MANAGEMENT SYSTEM - JAVASCRIPT
// =====================================================

let currentAccount = null;
let adminLoggedIn = false;


// =====================================================
// SECTION CONTROL
// =====================================================

function showSection(sectionId) {

    document.querySelectorAll(".section").forEach(section => {
        section.classList.remove("active");
    });

    const section = document.getElementById(sectionId);

    if (section) {
        section.classList.add("active");
    }

    window.scrollTo({
        top: 0,
        behavior: "smooth"
    });
}


// =====================================================
// CREATE ACCOUNT
// =====================================================

document.getElementById("createForm").addEventListener("submit", async function (event) {

    event.preventDefault();

    const name = document.getElementById("name").value.trim();
    const phone = document.getElementById("phone").value.trim();
    const pin = document.getElementById("pin").value.trim();
    const confirmPin = document.getElementById("confirmPin").value.trim();
    const deposit = document.getElementById("deposit").value.trim();

    const message = document.getElementById("createMessage");

    message.className = "message";
    message.textContent = "Creating account...";


    // Name validation
    if (name.length < 2) {

        showMessage(
            "createMessage",
            "Please enter a valid name.",
            "error"
        );

        return;
    }


    // Mobile validation
    if (!/^[0-9]{10}$/.test(phone)) {

        showMessage(
            "createMessage",
            "Mobile number must contain 10 digits.",
            "error"
        );

        return;
    }


    // PIN validation
    if (!/^[0-9]{4}$/.test(pin)) {

        showMessage(
            "createMessage",
            "PIN must contain exactly 4 digits.",
            "error"
        );

        return;
    }


    // Confirm PIN
    if (pin !== confirmPin) {

        showMessage(
            "createMessage",
            "PIN and Confirm PIN do not match.",
            "error"
        );

        return;
    }


    // Deposit validation
    if (deposit === "" || Number(deposit) < 0) {

        showMessage(
            "createMessage",
            "Please enter a valid initial deposit.",
            "error"
        );

        return;
    }


    try {

        const response = await fetch("/api/create", {

            method: "POST",

            headers: {
                "Content-Type":
                    "application/x-www-form-urlencoded"
            },

            body:
                "name=" + encodeURIComponent(name) +
                "&phone=" + encodeURIComponent(phone) +
                "&pin=" + encodeURIComponent(pin) +
                "&confirmPin=" + encodeURIComponent(confirmPin) +
                "&deposit=" + encodeURIComponent(deposit)
        });


        const data = await response.json();


        if (data.success) {

            showMessage(
                "createMessage",
                data.message,
                "success"
            );


            document.getElementById("newAccountNumber")
                .textContent = data.accountNumber;


            document.getElementById("accountCreated")
                .classList.remove("hidden");


            document.getElementById("createForm")
                .reset();

        } else {

            showMessage(
                "createMessage",
                data.message,
                "error"
            );
        }

    } catch (error) {

        showMessage(
            "createMessage",
            "Server is not running. Please start Java server.",
            "error"
        );

        console.error(error);
    }
});


// =====================================================
// CUSTOMER LOGIN
// =====================================================

document.getElementById("loginForm").addEventListener("submit", async function (event) {

    event.preventDefault();

    const account =
        document.getElementById("loginAccount")
            .value.trim();

    const pin =
        document.getElementById("loginPin")
            .value.trim();


    if (account === "" || pin === "") {

        showMessage(
            "loginMessage",
            "Please enter Account Number and PIN.",
            "error"
        );

        return;
    }


    try {

        const response = await fetch("/api/login", {

            method: "POST",

            headers: {
                "Content-Type":
                    "application/x-www-form-urlencoded"
            },

            body:
                "account=" + encodeURIComponent(account) +
                "&pin=" + encodeURIComponent(pin)
        });


        const data = await response.json();


        if (data.success) {

            currentAccount = data.accountNumber;

            document.getElementById("customerName")
                .textContent = data.name;

            document.getElementById("customerAccount")
                .textContent = data.accountNumber;


            updateBalance(data.balance);


            document.getElementById("loginForm")
                .reset();


            showSection("dashboard");


            loadTransactions();

        } else {

            showMessage(
                "loginMessage",
                data.message,
                "error"
            );
        }

    } catch (error) {

        showMessage(
            "loginMessage",
            "Server is not running.",
            "error"
        );

        console.error(error);
    }
});


// =====================================================
// DEPOSIT MONEY
// =====================================================

async function depositMoney() {

    if (!currentAccount) {

        alert("Please login first.");

        return;
    }


    const input =
        document.getElementById("depositAmount");

    const amount = input.value;


    if (amount === "" || Number(amount) <= 0) {

        showMessage(
            "transactionMessage",
            "Enter a valid deposit amount.",
            "error"
        );

        return;
    }


    try {

        const response = await fetch("/api/deposit", {

            method: "POST",

            headers: {
                "Content-Type":
                    "application/x-www-form-urlencoded"
            },

            body:
                "account=" +
                encodeURIComponent(currentAccount) +
                "&amount=" +
                encodeURIComponent(amount)
        });


        const data = await response.json();


        if (data.success) {

            updateBalance(data.balance);

            input.value = "";

            showMessage(
                "transactionMessage",
                "₹" + Number(amount).toFixed(2) +
                " deposited successfully.",
                "success"
            );


            loadTransactions();

        } else {

            showMessage(
                "transactionMessage",
                data.message,
                "error"
            );
        }

    } catch (error) {

        showMessage(
            "transactionMessage",
            "Unable to connect to server.",
            "error"
        );

        console.error(error);
    }
}


// =====================================================
// WITHDRAW MONEY
// =====================================================

async function withdrawMoney() {

    if (!currentAccount) {

        alert("Please login first.");

        return;
    }


    const input =
        document.getElementById("withdrawAmount");

    const amount = input.value;


    if (amount === "" || Number(amount) <= 0) {

        showMessage(
            "transactionMessage",
            "Enter a valid withdrawal amount.",
            "error"
        );

        return;
    }


    try {

        const response = await fetch("/api/withdraw", {

            method: "POST",

            headers: {
                "Content-Type":
                    "application/x-www-form-urlencoded"
            },

            body:
                "account=" +
                encodeURIComponent(currentAccount) +
                "&amount=" +
                encodeURIComponent(amount)
        });


        const data = await response.json();


        if (data.success) {

            updateBalance(data.balance);

            input.value = "";

            showMessage(
                "transactionMessage",
                "₹" + Number(amount).toFixed(2) +
                " withdrawn successfully.",
                "success"
            );


            loadTransactions();

        } else {

            showMessage(
                "transactionMessage",
                data.message,
                "error"
            );
        }

    } catch (error) {

        showMessage(
            "transactionMessage",
            "Unable to connect to server.",
            "error"
        );

        console.error(error);
    }
}


// =====================================================
// UPDATE BALANCE
// =====================================================

function updateBalance(balance) {

    document.getElementById("balance")
        .textContent =
        Number(balance).toLocaleString(
            "en-IN",
            {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
            }
        );
}


// =====================================================
// LOAD TRANSACTIONS
// =====================================================

async function loadTransactions() {

    if (!currentAccount) {
        return;
    }


    try {

        const response = await fetch(
            "/api/transactions?account=" +
            encodeURIComponent(currentAccount)
        );


        const data = await response.json();


        const table =
            document.getElementById("transactionTable");


        table.innerHTML = "";


        if (!data.success ||
            data.transactions.length === 0) {

            table.innerHTML = `
                <tr>
                    <td colspan="4">
                        No transactions found
                    </td>
                </tr>
            `;

            return;
        }


        data.transactions
            .slice()
            .reverse()
            .forEach(transaction => {

                const row =
                    document.createElement("tr");


                let typeClass = "";

                if (
                    transaction.type
                        .toLowerCase()
                        .includes("deposit")
                ) {

                    typeClass = "success";

                } else if (
                    transaction.type
                        .toLowerCase()
                        .includes("withdraw")
                ) {

                    typeClass = "error";
                }


                row.innerHTML = `

                    <td>
                        ${escapeHtml(transaction.date)}
                    </td>

                    <td class="${typeClass}">
                        ${escapeHtml(transaction.type)}
                    </td>

                    <td>
                        ₹${formatMoney(transaction.amount)}
                    </td>

                    <td>
                        ₹${formatMoney(transaction.balance)}
                    </td>

                `;


                table.appendChild(row);
            });


    } catch (error) {

        console.error(
            "Transaction loading error:",
            error
        );
    }
}


// =====================================================
// ADMIN LOGIN
// =====================================================

document.getElementById("adminLoginForm")
    .addEventListener("submit", async function (event) {

        event.preventDefault();


        const id =
            document.getElementById("adminId")
                .value.trim();

        const pin =
            document.getElementById("adminPin")
                .value.trim();


        if (id === "" || pin === "") {

            showMessage(
                "adminMessage",
                "Enter Admin ID and PIN.",
                "error"
            );

            return;
        }


        try {

            const response = await fetch(
                "/api/admin-login",
                {

                    method: "POST",

                    headers: {
                        "Content-Type":
                            "application/x-www-form-urlencoded"
                    },

                    body:
                        "id=" + encodeURIComponent(id) +
                        "&pin=" + encodeURIComponent(pin)
                }
            );


            const data = await response.json();


            if (data.success) {

                adminLoggedIn = true;

                document.getElementById("adminLoginForm")
                    .reset();

                showSection("adminDashboard");

                loadCustomers();

            } else {

                showMessage(
                    "adminMessage",
                    data.message,
                    "error"
                );
            }

        } catch (error) {

            showMessage(
                "adminMessage",
                "Server is not running.",
                "error"
            );

            console.error(error);
        }
    });


// =====================================================
// LOAD ALL CUSTOMERS - ADMIN
// =====================================================

async function loadCustomers() {

    if (!adminLoggedIn) {
        return;
    }


    try {

        const response =
            await fetch("/api/customers");


        const data =
            await response.json();


        if (!data.success) {
            return;
        }


        document.getElementById("totalCustomers")
            .textContent =
            data.totalCustomers;


        document.getElementById("totalBalance")
            .textContent =
            formatMoney(data.totalBalance);


        const table =
            document.getElementById("customerTable");


        table.innerHTML = "";


        if (data.customers.length === 0) {

            table.innerHTML = `
                <tr>
                    <td colspan="5">
                        No customers found
                    </td>
                </tr>
            `;

            return;
        }


        data.customers.forEach(customer => {

            const row =
                document.createElement("tr");


            row.innerHTML = `

                <td>
                    <strong>
                        ${escapeHtml(customer.accountNumber)}
                    </strong>
                </td>

                <td>
                    ${escapeHtml(customer.name)}
                </td>

                <td>
                    ${escapeHtml(customer.phone)}
                </td>

                <td>
                    ₹${formatMoney(customer.balance)}
                </td>

                <td>
                    ${escapeHtml(customer.createdDate)}
                </td>

            `;


            table.appendChild(row);
        });


    } catch (error) {

        console.error(
            "Customer loading error:",
            error
        );
    }
}


// =====================================================
// CUSTOMER LOGOUT
// =====================================================

function logout() {

    currentAccount = null;

    document.getElementById("transactionTable")
        .innerHTML = `
            <tr>
                <td colspan="4">
                    No transactions found
                </td>
            </tr>
        `;

    showSection("home");
}


// =====================================================
// ADMIN LOGOUT
// =====================================================

function adminLogout() {

    adminLoggedIn = false;

    document.getElementById("customerTable")
        .innerHTML = `
            <tr>
                <td colspan="5">
                    No customers found
                </td>
            </tr>
        `;

    showSection("home");
}


// =====================================================
// MESSAGE
// =====================================================

function showMessage(
    elementId,
    message,
    type
) {

    const element =
        document.getElementById(elementId);


    element.textContent = message;

    element.className =
        "message " + type;
}


// =====================================================
// MONEY FORMAT
// =====================================================

function formatMoney(amount) {

    return Number(amount).toLocaleString(
        "en-IN",
        {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }
    );
}


// =====================================================
// SECURITY - HTML ESCAPE
// =====================================================

function escapeHtml(value) {

    if (value === null ||
        value === undefined) {

        return "";
    }


    return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}


// =====================================================
// STARTUP
// =====================================================

console.log(
    "Bank Management System loaded successfully."
);