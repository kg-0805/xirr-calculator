# 📊 XIRR Calculator

A sleek, secure Spring Boot web application that calculates **XIRR (Extended Internal Rate of Return)** from uploaded Excel workbooks containing mutual fund transactions.

> _Calculate your true annualized returns — accounting for every irregular cash flow._

---

## ✨ Features

| Category | Details |
|----------|---------|
| 📈 **XIRR Calculation** | Annualized return based on irregular cash flows using Newton-Raphson + Bisection |
| 💰 **Result Summary** | Total invested, total redeemed, profit/loss at a glance |
| 🛡️ **Admin Panel** | Full user management — create, activate, deactivate, delete, set expiry, reset password |
| 🔐 **Role-based Access** | Admins manage users; regular users access the calculator |
| ⏱️ **Login Expiry** | Configurable per-user expiry (default: 1 hour for new accounts) |
| 🔑 **Change Password** | All logged-in users can change their own password |
| 📧 **Forgot Password** | 6-digit verification code via email for all active users |
| 🔄 **Request Reactivation** | Expired/deactivated users can request reactivation from login page |
| 📝 **Auto Account Creation** | Access request form auto-creates user with random password + welcome email |
| 📬 **Email Notifications** | Beautiful HTML emails via Brevo API — welcome, activation, password reset, access requests |
| 🚦 **Rate Limiting** | In-memory per-IP rate limiting on login, upload, and access request endpoints |
| 🗄️ **PostgreSQL Backend** | User data stored in PostgreSQL (Neon DB) |
| 🔒 **BCrypt Passwords** | All passwords stored as hashes, never plain text |
| 📥 **Sample Excel Download** | One-click sample workbook download from the dashboard |
| 📜 **Disclaimer & Privacy** | Public legal pages accessible without login |
| 🧹 **No Data Persistence** | Uploaded files processed in memory and immediately discarded |

---

## 📋 Workbook Format

Use the first sheet with these column headers:

| Date | Type | Amount |
|------|------|--------|
| 2024-01-10 | BUY | 10000 |
| 2024-04-05 | BUY | 7500 |
| 2024-09-20 | SELL | 5000 |
| 2025-01-15 | PRESENT | 22000 |

**Rules:**
- ✅ First entry must be **BUY**
- ✅ Last entry must be **PRESENT** (current portfolio value)
- ✅ At least one BUY and one PRESENT required
- ✅ SELL entries are optional (partial redemptions)
- ✅ Dates must be in ascending order

**Supported date formats:**
`yyyy-MM-dd` · `dd/MM/yyyy` · `MM/dd/yyyy` · `dd-MMM-yyyy`

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Spring Boot 3.5 (Java 21) |
| **Security** | Spring Security — form login, role-based access, BCrypt |
| **Database** | Spring Data JPA + PostgreSQL (Neon DB) |
| **Excel Parsing** | Apache POI |
| **Templates** | Thymeleaf (server-side rendering) |
| **Email** | Brevo API (transactional HTML emails) |
| **Environment** | spring-dotenv (auto-loads `.env`) |
| **Deployment** | Render (Web Service) |
| **Database Hosting** | Neon (Serverless PostgreSQL) |

---

## ⚙️ Environment Variables

Create a `.env` file in the project root (see `.env.example`):

```env
DB_URL=jdbc:postgresql://ep-xxxxx.aws.neon.tech/xirr?sslmode=require
DB_USERNAME=neondb_owner
DB_PASSWORD=your-neon-password
BREVO_API_KEY=your-brevo-api-key
BREVO_FROM_EMAIL=your-email@example.com
ACCESS_REQUEST_NOTIFY_EMAIL=your-email@example.com
SITE_URL=https://your-app.onrender.com
```

---

## 🚀 Local Development

### Prerequisites
- Java 21
- Maven
- PostgreSQL (local or Neon)

### Run

```bash
mvn spring-boot:run
```

Open `http://localhost:8080`

**Default admin login (created on first startup):**
- 📧 Email: `admin@kartikgupta.in`
- 🔑 Password: `ChangeThisNow!2026`

### Package

```bash
mvn clean package -DskipTests
java -jar target/xirr-calculator-0.0.1-SNAPSHOT.jar
```

---

## ☁️ Deployment (Render + Neon)

### Database — Neon

1. Create a project at [neon.tech](https://neon.tech)
2. Create a database (e.g. `xirr`)
3. Copy the JDBC connection string for your `.env`
4. Tables are auto-created on first startup (`ddl-auto: update`)

### Web Service — Render

1. Push your code to GitHub
2. Go to [render.com](https://render.com) → **New Web Service**
3. Connect your repository
4. Configure:
   - **Runtime:** Docker (or Native with Java 21)
   - **Build Command:** `mvn clean package -DskipTests`
   - **Start Command:** `java -jar target/xirr-calculator-0.0.1-SNAPSHOT.jar`
5. Add all environment variables from `.env` in the Render dashboard
6. Deploy

---

## 👑 Admin Panel

Accessible at `/admin` for users with admin role.

| Action | Description |
|--------|-------------|
| ➕ Create User | Email as login ID, auto-generate or set password, optional admin rights |
| ✅ Activate | Re-enable a deactivated user |
| ⏸️ Deactivate | Disable user login |
| ⏱️ Set Expiry | Date & time picker for login expiry |
| 🔑 Reset Password | Generate or set new password (emails user automatically) |
| 🗑️ Delete | Permanently remove a user |
| 🔒 Change Password | Admin can change their own password |

New users get **1 hour** of access by default. Admin can extend via the expiry setting.

---

## 🔐 Password Reset Flow

| User Type | Flow |
|-----------|------|
| **All active users** | Forgot Password → enters email → receives 6-digit code → verifies → sets new password |
| **Inactive users** | Request Reactivation → admin receives notification → admin activates from panel |

> New password cannot be the same as the current password.

---

## 🚦 Rate Limiting

| Endpoint | Limit |
|----------|-------|
| `POST /login` | 6 requests / 60 seconds / IP |
| `POST /access-request` | 4 requests / 600 seconds / IP |
| `POST /api/xirr/calculate` | 20 requests / 60 seconds / IP |

Configure in `application.yml` under `app.rate-limit`.

---

## 📬 Email Notifications

All emails use beautiful HTML templates with:
- Clean card layout with branding
- "Log In Now" action buttons
- Credential cards with expiry info
- Sent via **Brevo API**

Triggers:
- 🆕 User created → welcome email to user + admin notification
- ✅ User activated → notification to user + admin
- 🔑 Password reset → new credentials to user + admin
- 📧 Verification code → code to user + copy to admin
- 📝 Access request → admin notification
- 🔄 Reactivation request → admin notification

---

## 📄 Public Pages

| Page | URL |
|------|-----|
| Disclaimer | `/disclaimer` |
| Privacy Policy | `/privacy` |

Accessible without login.

---

## 📁 Project Structure

```
src/main/java/com/xirr/calculator/
├── config/          # Security, rate limiting, data initializer
├── controller/      # REST + MVC controllers
├── exception/       # Custom exceptions
├── model/           # JPA entities, DTOs, enums
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic, email, Excel parsing
└── tools/           # BCrypt generator utility
```

---

## 📜 License

Private project by **Kartik Gupta** — [kartikgupta.in](https://kartikgupta.in)
