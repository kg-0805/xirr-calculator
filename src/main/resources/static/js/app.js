const uploadForm = document.getElementById("upload-form");
const fileInput = document.getElementById("file");
const submitButton = uploadForm?.querySelector("button[type='submit']");
const messageElement = document.getElementById("message");
const resultState = document.getElementById("result-state");
const resultContent = document.getElementById("result-content");
const xirrValue = document.getElementById("xirr-value");
const totalInvestedEl = document.getElementById("total-invested");
const totalRedeemedEl = document.getElementById("total-redeemed");
const profitLossEl = document.getElementById("profit-loss");
const transactionSummary = document.getElementById("transaction-summary");
const transactionsTableBody = document.querySelector("#transactions-table tbody");
const csrfToken = document.querySelector("meta[name='_csrf']")?.content;
const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.content;

function setMessage(text, variant) {
    if (!messageElement) {
        return;
    }

    messageElement.textContent = text;
    messageElement.className = `status-banner ${variant}`;
}

function clearMessage() {
    if (!messageElement) {
        return;
    }

    messageElement.textContent = "";
    messageElement.className = "status-banner hidden";
}

function renderTransactions(transactions) {
    transactionsTableBody.innerHTML = "";

    transactions.forEach((transaction) => {
        const row = document.createElement("tr");
        const typeClass = transaction.type.toLowerCase().replace("_", "-");
        row.innerHTML = `
            <td>${transaction.date}</td>
            <td><span class="chip ${typeClass}">${transaction.type}</span></td>
            <td>${Number(transaction.amount).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
            <td>${Number(transaction.signedCashFlow).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
        `;
        transactionsTableBody.appendChild(row);
    });
}

async function handleUpload(event) {
    event.preventDefault();
    clearMessage();

    const file = fileInput?.files?.[0];
    if (!file) {
        setMessage("Choose an Excel file before calculating XIRR.", "error");
        return;
    }

    submitButton.disabled = true;
    submitButton.textContent = "Calculating...";

    const formData = new FormData();
    formData.append("file", file);

    try {
        const response = await fetch("/api/xirr/calculate", {
            method: "POST",
            headers: csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {},
            body: formData
        });

        const payload = await response.json();
        if (!response.ok) {
            throw new Error(payload.message || "Unable to calculate XIRR.");
        }

        xirrValue.textContent = payload.formattedXirr;

        const fmt = (v) => Number(v).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        totalInvestedEl.textContent = "₹" + fmt(payload.totalInvested);
        totalRedeemedEl.textContent = "₹" + fmt(payload.totalRedeemed);
        const pl = Number(payload.profitOrLoss);
        profitLossEl.textContent = (pl >= 0 ? "+₹" : "-₹") + fmt(Math.abs(pl));
        profitLossEl.className = "summary-value " + (pl >= 0 ? "profit" : "loss");

        transactionSummary.textContent = `${payload.transactionCount} transactions validated from the uploaded workbook.`;
        renderTransactions(payload.transactions);
        resultState.classList.add("hidden");
        resultContent.classList.remove("hidden");
        setMessage("Workbook processed successfully.", "success");
    } catch (error) {
        setMessage(error.message || "Unable to calculate XIRR.", "error");
        resultState.classList.remove("hidden");
        resultContent.classList.add("hidden");
    } finally {
        submitButton.disabled = false;
        submitButton.textContent = "Calculate XIRR";
    }
}

if (uploadForm) {
    uploadForm.addEventListener("submit", handleUpload);
}
