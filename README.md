# 🔐 AuthForge

> Production-ready authentication starter kit for Spring Boot

![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot 3.2](https://img.shields.io/badge/Spring%20Boot-3.2-green?logo=spring)
![Spring Security 6](https://img.shields.io/badge/Spring%20Security-6-green?logo=springsecurity)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Ready-blue?logo=docker)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow)

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
| Two-Factor Authentication (TOTP) | 🔜 v1.2 |
| Rate Limiting | 🔜 v1.2 |

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

### Default ports

| Service | Port | URL |
|---------|------|-----|
| Frontend (Nginx) | 4000 | http://localhost:4000 |
| Backend (Spring Boot) | 8090 | http://localhost:8090 |
| PostgreSQL | 5433 | — |

---

## 📡 API Endpoints

### Authentication (Public)

```
POST /api/auth/register        → Register a new user
POST /api/auth/login           → Login → JWT tokens
POST /api/auth/refresh         → Refresh access token
POST /api/auth/logout          → Invalidate refresh token
POST /api/auth/forgot-password → Generate reset token
POST /api/auth/reset-password  → Reset with token
```

### User (Authenticated)

```
GET  /api/users/me             → Current user profile
```

### Admin (ADMIN role)

```
GET  /api/admin/users          → List all users
PUT  /api/admin/users/{id}/role → Change user role
```

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────┐
│                Docker Compose               │
├──────────┬──────────────┬───────────────────┤
│ Frontend │   Backend    │    PostgreSQL      │
│ Nginx    │ Spring Boot  │                   │
│ :4000    │ :8090        │    :5433           │
│          │              │                   │
│ HTML/CSS │ Controllers  │  users table      │
│ JS       │ Services     │  refresh_tokens   │
│          │ Security     │                   │
│  /api/* ──►  JWT Filter │                   │
│          │  BCrypt      │                   │
└──────────┴──────────────┴───────────────────┘
```

---

## 📁 Project Structure

```
authforge/
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/authforge/
│       ├── AuthForgeApplication.java
│       ├── config/
│       │   └── SecurityConfig.java
│       ├── controller/
│       │   ├── AuthController.java
│       │   ├── UserController.java
│       │   └── AdminController.java
│       ├── dto/
│       │   ├── RegisterRequest.java
│       │   ├── LoginRequest.java
│       │   ├── AuthResponse.java
│       │   ├── TokenRefreshRequest.java
│       │   └── PasswordResetRequest.java
│       ├── exception/
│       │   └── GlobalExceptionHandler.java
│       ├── model/
│       │   ├── User.java
│       │   ├── Role.java
│       │   └── RefreshToken.java
│       ├── repository/
│       │   ├── UserRepository.java
│       │   └── RefreshTokenRepository.java
│       ├── security/
│       │   ├── JwtTokenProvider.java
│       │   └── JwtAuthFilter.java
│       └── service/
│           ├── AuthService.java
│           ├── CustomUserDetailsService.java
│           ├── RefreshTokenService.java
│           └── UserService.java
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── index.html
│   ├── css/style.css
│   └── js/
│       ├── api.js
│       ├── auth.js
│       └── app.js
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
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

## 🧪 Testing the API

```bash
# Register
curl -X POST http://localhost:8090/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"John","email":"john@test.com","password":"password123"}'

# Login
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@test.com","password":"password123"}'

# Get profile (replace TOKEN)
curl -X GET http://localhost:8090/api/users/me \
  -H "Authorization: Bearer TOKEN"
```

---

## 📋 Roadmap

- [x] **v1.0** — JWT Auth, Roles, Password Recovery, Docker
- [x] **v1.1** — OAuth2 (Google, GitHub)
- [ ] **v1.2** — 2FA (TOTP), Rate Limiting
- [ ] **v2.0** — Email Service, Account Verification

---

## 📜 License

[MIT](LICENSE) — Use this starter kit freely in your projects.

---

<p align="center">
  Built with ☕ Java 21 + 🍃 Spring Boot 3 + 🛡️ Spring Security 6
  <br>
  <strong>by <a href="https://github.com/FirstOnDie">Carlos Expósito</a></strong>
</p>
