# XIRR Calculator

Spring Boot web app that calculates XIRR (Extended Internal Rate of Return) from an uploaded Excel workbook containing mutual fund transactions.

## Features

- **XIRR Calculation** — annualized return based on irregular cash flows
- **Result Summary** — shows total invested, total redeemed, and profit/loss
- **Admin Panel** — full user management (create, activate, deactivate, delete, set expiry, reset password)
- **Role-based Access** — admin users manage the panel; regular users access the calculator
- **Login Expiry** — each user has a configurable expiry time (default: 1 hour for new users)
- **Change Password** — all logged-in users can change their own password
- **Forgot Password** — admin gets a 2FA code via email; regular users request admin to reset
- **Request Reactivation** — expired/deactivated users can request reactivation from the login page
- **Access Request Form** — unauthenticated users can request a new account
- **Email Notifications** — via Brevo API on user creation, activation, password reset, and access requests
- **Rate Limiting** — in-memory per-IP rate limiting on login, upload, and access request endpoints
- **PostgreSQL Backend** — user data stored in PostgreSQL (Neon compatible)
- **BCrypt Passwords** — all passwords stored as hashes, never plain text
- **Sample Excel Download** — users can download a sample workbook from the dashboard
- **Disclaimer & Privacy** — public pages accessible without login
- **No Transaction Persistence** — uploaded files are processed in memory and immediately discarded

## Workbook Format

Use the first sheet with these column headers:

| Date | Type | Amount |
| --- | --- | --- |
| 2024-01-10 | BUY | 10000 |
| 2024-04-05 | BUY | 7500 |
| 2024-09-20 | SELL | 5000 |
| 2025-01-15 | PRESENT | 22000 |

**Rules:**
- First entry must be **BUY**
- Last entry must be **PRESENT** (current portfolio value)
- At least one BUY and one PRESENT required
- SELL entries are optional (partial redemptions)
- Dates must be in ascending order

**Supported date formats:** `yyyy-MM-dd`, `dd/MM/yyyy`, `MM/dd/yyyy`, `dd-MMM-yyyy`

## Prerequisites

- Java 21
- Maven
- PostgreSQL database (local or Neon)

## Environment Variables

Create a `.env` file in the project root (see `.env.example`):

```
DB_URL=jdbc:postgresql://localhost:5432/xirr
DB_USERNAME=xirr
DB_PASSWORD=xirr
BREVO_API_KEY=your-brevo-api-key
BREVO_FROM_EMAIL=your-email@example.com
ACCESS_REQUEST_NOTIFY_EMAIL=your-email@example.com
```

## Local Run

```powershell
mvn spring-boot:run
```

Open `http://localhost:8080`

**Default admin login (created on first startup):**
- Email: `admin@kartikgupta.in`
- Password: `ChangeThisNow!2026`

## Package

```powershell
mvn clean package -DskipTests
java -jar target/xirr-calculator-0.0.1-SNAPSHOT.jar
```

## Docker

```powershell
docker build -t xirr-calculator .
docker run --env-file .env -p 8080:8080 xirr-calculator
```

## Admin Panel

Accessible at `/admin` for users with admin role.

- Create users (email as login ID, password, optional admin rights)
- Activate / Deactivate users
- Set login expiry (date & time picker)
- Reset user passwords
- Delete users
- Change your own password

New users get 1 hour of access by default. Admin can extend via the expiry setting.

## Access Request Form

Unauthenticated users can submit a request from the login page:
- Full name, email, and purpose are required
- Admin receives an email notification
- Requests are also stored in `data/access-requests.jsonl`

## Password Reset Flow

**Admin:** Forgot Password → enters email → receives 6-digit code via email → verifies code → sets new password.

**Regular user:** Forgot Password → clicks "Request Admin to Reset" → admin receives email notification → admin resets from the panel.

**All users:** New password cannot be the same as the current password.

## Rate Limiting

- `POST /login`: 6 requests per 60 seconds per IP
- `POST /access-request`: 4 requests per 600 seconds per IP
- `POST /api/xirr/calculate`: 20 requests per 60 seconds per IP

Configure in `application.yml` under `app.rate-limit`.

## Deployment (AWS EC2 Spot)

1. Launch a `t2.micro` spot instance (Amazon Linux 2023, Mumbai region)
2. Install Java 21: `sudo yum install java-21-amazon-corretto -y`
3. Upload the jar and create `.env` on the server
4. Create a systemd service for auto-start
5. Attach an Elastic IP for a fixed address
6. Open port 8080 in the security group

Estimated cost: ~₹100–150/month

## Tech Stack

- Spring Boot 3.5
- Spring Security (form login, role-based)
- Spring Data JPA + PostgreSQL
- Apache POI (Excel parsing)
- Thymeleaf (server-side templates)
- Brevo API (transactional email)
- spring-dotenv (auto-loads `.env`)
