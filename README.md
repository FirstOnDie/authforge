# 🔐 AuthForge

> Production-ready authentication starter kit for Spring Boot

![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot 3.2](https://img.shields.io/badge/Spring%20Boot-3.2-green?logo=spring)
![Spring Security 6](https://img.shields.io/badge/Spring%20Security-6-green?logo=springsecurity)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Ready-blue?logo=docker)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow)
![Coverage](https://img.shields.io/badge/Coverage-100%25-brightgreen.svg)

---

## ✨ Features

| Feature | Status |
|---------|--------|
| JWT Authentication (Access + Refresh Tokens) | ✅ |
| BCrypt Password Hashing | ✅ |
| Role-Based Access Control (USER, ADMIN) | ✅ |
| Token Refresh with Rotation | ✅ |
| Password Recovery (reset token) | ✅ |
| CORS Configuration | ✅ |
| Global Exception Handling | ✅ |
| Frontend Demo (Login, Register, Dashboard, Admin) | ✅ |
| Docker Compose (PostgreSQL + Backend + Frontend) | ✅ |
| OAuth2 (Google, GitHub) | ✅ |
| Two-Factor Authentication (TOTP) | ✅ |
| Rate Limiting (Bucket4j) | ✅ |
| Feature Flags (Toggle features via env vars) | ✅ |
| Email Service (Verification + Password Reset) | ✅ |
| Unit Tests + JaCoCo Coverage (100%) | ✅ |
| SonarQube Code Quality (0 Bugs/Smells) | ✅ |

---

## 🚀 Quick Start

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/) & [Docker Compose](https://docs.docker.com/compose/install/)

### Run with Docker (recommended)

```bash
git clone https://github.com/FirstOnDie/authforge.git
cd authforge
docker-compose up --build
```

Open **http://localhost:4000** → Ready! 🎉

### Updating containers (after code changes)

If you modify the source code, you can apply the changes by rebuilding the containers:

```bash
docker-compose down
docker-compose up --build -d
```

### Default ports

| Service | Port | URL |
|---------|------|-----|
| Frontend (Nginx) | 4000 | http://localhost:4000 |
| Backend (Spring Boot) | 8090 | http://localhost:8090 |
| PostgreSQL | 5433 | — |
| MailHog (Email UI) | 8025 | http://localhost:8025 |
| SonarQube (separate) | 9000 | http://localhost:9000 |

---

## 🎛️ Feature Flags

Toggle features on/off via environment variables — no code changes needed.

| Flag | Env Variable | Default |
|------|-------------|---------|
| OAuth2 Login | `FEATURE_OAUTH2` | `true` |
| Two-Factor Auth | `FEATURE_2FA` | `true` |
| Rate Limiting | `FEATURE_RATE_LIMIT` | `true` |
| Email Verification | `FEATURE_EMAIL` | `true` |

Active flags are exposed at `GET /api/admin/features` (admin only).

---

## 📧 Email Service

Uses **MailHog** in Docker for local email testing (no real emails sent).

- **Verification emails**: sent on registration when `FEATURE_EMAIL=true`
- **Password reset emails**: HTML emails with reset links
- **MailHog UI**: http://localhost:8025 to view all captured emails

---

## 🧪 Tests, Coverage & SonarQube

AuthForge ensures high code quality and reliability.

### Running Tests locally
```bash
cd backend
mvn clean test
```
JaCoCo coverage report: `backend/target/site/jacoco/index.html`

### SonarQube Analysis
Runs in a **separate** Docker Compose file to keep the main stack lightweight. It enforces a Strict Quality Gate (0 Bugs, 0 Vulnerabilities, 0 Code Smells, 100% Coverage).

```bash
# Start SonarQube
docker-compose -f docker-compose.sonar.yml up -d

# Wait ~1 min for startup, then run analysis
cd backend
mvn clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.login=admin -Dsonar.password=admin
```

Default credentials: `admin` / `admin` (change on first login).

---

## 📖 API Endpoint Guide

AuthForge uses **Swagger UI (OpenAPI 3)** for interactive API documentation.
Once the application is running, you can explore and test all endpoints directly from your browser:

👉 **[http://localhost:8090/swagger-ui.html](http://localhost:8090/swagger-ui.html)**

*(To use protected endpoints in Swagger, click the **Authorize** button and input your `Bearer <accessToken>`)*

Below is a brief summary of the available endpoints.

### 1. Authentication
Endpoints for login, registration, and managing access tokens.

#### `POST /api/auth/register`
Creates a new account.
- **Access**: Public
- **Request Body**:
  ```json
  {
    "name": "John Doe",
    "email": "john@example.com",
    "password": "SecurePassword123"
  }
  ```

#### `POST /api/auth/login`
Authenticates a user and issues JWT tokens.
- **Access**: Public
- **Request Body**:
  ```json
  {
    "email": "john@example.com",
    "password": "SecurePassword123"
  }
  ```
- **Response**: Returns an `accessToken` and a `refreshToken`. If 2FA is enabled, it returns `requiresTwoFactor: true` and no tokens.

#### `POST /api/auth/refresh`
Exchanges a valid Refresh Token for a new Access Token.
- **Access**: Public
- **Request Body**:
  ```json
  {
    "refreshToken": "your-refresh-token-here"
  }
  ```

#### `POST /api/auth/logout`
Invalidates the current Refresh Token.
- **Access**: Authenticated (Requires Bearer Token)
- **Request Body**:
  ```json
  {
    "refreshToken": "your-refresh-token-here"
  }
  ```

#### `POST /api/auth/forgot-password`
Initates the password reset flow by email.
- **Access**: Public
- **Request Body**:
  ```json
  {
    "email": "john@example.com"
  }
  ```

#### `POST /api/auth/reset-password`
Resets the password using the token sent to the email.
- **Access**: Public
- **Request Body**:
  ```json
  {
    "token": "reset-token-received-via-email",
    "newPassword": "NewSecurePassword123"
  }
  ```

### 2. Two-Factor Authentication (TOTP)
Endpoints for setting up and verifying TOTP codes.

#### `POST /api/2fa/setup`
Generates a TOTP secret and QR code URI for Authenticator apps.
- **Access**: Authenticated (Requires Bearer Token)
- **Response**: Returns `secretKey` and `qrCodeUrl`.

#### `POST /api/2fa/enable`
Verifies a TOTP code and activates 2FA on the account.
- **Access**: Authenticated (Requires Bearer Token)
- **Request Body**:
  ```json
  {
    "code": "123456"
  }
  ```

#### `POST /api/2fa/disable`
Disables Two-Factor Authentication.
- **Access**: Authenticated (Requires Bearer Token)

#### `POST /api/auth/2fa/verify`
Step 2 of login when 2FA is enabled. Validates code and issues JWT tokens.
- **Access**: Public
- **Request Body**:
  ```json
  {
    "email": "john@example.com",
    "code": "123456"
  }
  ```

### 3. User Profile
Endpoints related to the logged-in user.

#### `GET /api/users/me`
Fetches the current user's profile information.
- **Access**: Authenticated (Requires Bearer Token)
- **Response Example**:
  ```json
  {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "role": "USER",
    "emailVerified": true,
    "twoFactorEnabled": false,
    "oauth2Provider": "local"
  }
  ```

### 4. Admin Management
Administrative actions, available only to accounts with the `ADMIN` role.

#### `GET /api/admin/users`
Fetches a list of all registered users.
- **Access**: `ADMIN` Only

#### `PUT /api/admin/users/{id}/role`
Changes the role of a user.
- **Access**: `ADMIN` Only
- **Path Parameter**: `id` - The numeric ID of the user.
- **Request Example**: `?role=ADMIN` (as Request Parameter)

#### `GET /api/admin/features`
Fetches the active states of system feature flags.
- **Access**: `ADMIN` Only

---

## 🏗️ Architecture

```text
┌─────────────────────────────────────────────┐
│                Docker Compose               │
├──────────┬──────────────┬───────────────────┤
│ Frontend │   Backend    │    PostgreSQL     │
│ Nginx    │ Spring Boot  │                   │
│ :4000    │ :8090        │    :5433          │
│          │              │                   │
│ HTML/CSS │ Controllers  │  users table      │
│ JS       │ Services     │  refresh_tokens   │
│          │ Security     │                   │
│  /api/* ──►  JWT Filter │                   │
│          │  BCrypt      │                   │
└──────────┴──────────────┴───────────────────┘
```

---

## ⚙️ Configuration

All configuration is done via environment variables (see `.env.example`):

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://postgres:5432/authforge` | Database URL |
| `DB_USERNAME` | `authforge` | Database user |
| `DB_PASSWORD` | `authforge` | Database password |
| `JWT_SECRET` | (change me!) | HMAC-SHA256 signing key |
| `CORS_ORIGINS` | `http://localhost:4000` | Allowed CORS origins |
| `GOOGLE_CLIENT_ID` | — | Google OAuth2 Client ID |
| `GOOGLE_CLIENT_SECRET` | — | Google OAuth2 Client Secret |
| `GITHUB_CLIENT_ID` | — | GitHub OAuth2 Client ID |
| `GITHUB_CLIENT_SECRET` | — | GitHub OAuth2 Client Secret |
| `OAUTH2_REDIRECT_URI` | `http://localhost:4000` | Frontend redirect after OAuth2 |

---

## 🔑 OAuth2 Setup (Google & GitHub)

### Google
1. Go to [Google Cloud Console](https://console.cloud.google.com/) → APIs & Services → Credentials
2. Create an **OAuth 2.0 Client ID** (Web Application)
3. Set Authorized redirect URI: `http://localhost:8090/login/oauth2/code/google`
4. Copy the Client ID and Client Secret into your `.env` file

### GitHub
1. Go to [GitHub Settings](https://github.com/settings/developers) → OAuth Apps → New OAuth App
2. Set Authorization callback URL: `http://localhost:8090/login/oauth2/code/github`
3. Copy the Client ID and Client Secret into your `.env` file

---

## ⏱️ Rate Limiting

Auth endpoints (`/api/auth/**`) are rate-limited to prevent brute-force attacks.

| Setting | Default | Env Variable |
|---------|---------|-------------|
| Requests per minute (per IP) | 30 | `RATE_LIMIT_RPM` |

When the limit is exceeded, the API returns HTTP `429 Too Many Requests`.

---

## 📋 Roadmap

- [x] **v1.0** — JWT Auth, Roles, Password Recovery, Docker
- [x] **v1.1** — OAuth2 (Google, GitHub)
- [x] **v1.2** — 2FA (TOTP), Rate Limiting
- [x] **v2.0** — Email Service, Account Verification, Feature Flags, 100% Coverage, 0 SonarQube issues

---

## 📜 License

[MIT](LICENSE) — Use this starter kit freely in your projects.

---

<p align="center">
  Built with ☕ Java 21 + 🍃 Spring Boot 3 + 🛡️ Spring Security 6
  <br>
  <strong>by <a href="https://github.com/FirstOnDie">Carlos Expósito</a></strong>
</p>
