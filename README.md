# XIRR Calculator

Spring Boot web app that calculates XIRR from an uploaded Excel workbook containing mutual fund BUY and SELL entries.

## Features

- Login-protected dashboard using Spring Security
- Supports multiple configured login IDs with BCrypt password hashes
- Password stored as a bcrypt hash, not plain text
- In-memory rate limiting on login attempts and XIRR upload requests
- Public access-request form with validation and file-based request storage
- Excel upload support for `.xlsx` and `.xls`
- Validates that:
  - the first transaction is `BUY`
  - the last transaction is `SELL`
  - there is at least one `BUY` and one `SELL`
  - transactions are in ascending date order
- No database and no transaction persistence

## Workbook Format

Use the first sheet with these column headers:

| Date | Type | Amount |
| --- | --- | --- |
| 2024-01-10 | BUY | 10000 |
| 2024-04-05 | BUY | 7500 |
| 2025-01-15 | SELL | 22000 |

Supported date formats:

- `yyyy-MM-dd`
- `dd/MM/yyyy`
- `MM/dd/yyyy`
- `dd-MMM-yyyy`

## Local Run

```powershell
mvn spring-boot:run
```

Open `http://localhost:8080`.

Default login:

- Username: `investor`
- Password: `ChangeThisNow!2026`

Additional sample login:

- Username: `advisor`
- Password: `ChangeThisNow!2026`

## Package

```powershell
mvn clean package
java -jar target/xirr-calculator-0.0.1-SNAPSHOT.jar
```

## Docker

```powershell
docker build -t xirr-calculator .
docker run -p 8080:8080 xirr-calculator
```

## Change The Login Password

Run the standalone generator and replace the `password-hash` value for any user under `app.auth.users` in `src/main/resources/application.yml`.

```powershell
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.xirr.calculator.tools.BcryptGenerator"
```

You can also pass the password as an argument if you prefer:

```powershell
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.xirr.calculator.tools.BcryptGenerator" "-Dexec.args=MyNewPassword123!"
```

## Multiple Login IDs

Add as many users as you need under `app.auth.users` in `src/main/resources/application.yml`.

```yaml
app:
  auth:
    users:
      - username: investor
        password-hash: "..."
      - username: advisor
        password-hash: "..."
```

## Access Request Form

Unauthenticated users can submit a request for a new login ID from the login page.

- Requests are validated on the server
- Existing usernames are rejected
- Requests are stored in `data/access-requests.jsonl` by default
- The storage path is configurable with `app.access-request.storage-path`

## Rate Limiting

The app includes basic in-memory rate limiting to slow down brute-force and repeated upload attacks.

- `POST /login`: 6 requests per 60 seconds per client IP
- `POST /access-request`: 4 requests per 600 seconds per client IP
- `POST /api/xirr/calculate`: 20 requests per 60 seconds per client IP

Tune these values in `src/main/resources/application.yml` under `app.rate-limit`.
