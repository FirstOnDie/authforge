# 🔐 AuthForge

[English](#english) | [Español](#español)

<a id="english"></a>
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

## 📡 API Endpoints

### Authentication (Public)
```text
POST /api/auth/register        → Register a new user
POST /api/auth/login           → Login → JWT tokens
POST /api/auth/refresh         → Refresh access token
POST /api/auth/logout          → Invalidate refresh token
POST /api/auth/forgot-password → Generate reset token
POST /api/auth/reset-password  → Reset with token
```

### User (Authenticated)
```text
GET  /api/users/me             → Current user profile
```

### Admin (ADMIN role)
```text
GET  /api/admin/users          → List all users
PUT  /api/admin/users/{id}/role → Change user role
```

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

## 📁 Project Structure

```text
authforge/
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/authforge/
│       ├── config/
│       ├── controller/
│       ├── dto/
│       ├── exception/
│       ├── model/
│       ├── repository/
│       ├── security/
│       └── service/
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── index.html
│   ├── css/
│   └── js/
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

## 🛡️ Two-Factor Authentication (TOTP)

AuthForge supports TOTP-based 2FA compatible with Google Authenticator, Authy, and similar apps.

**Flow:**
1. User enables 2FA from the dashboard → scans QR code with authenticator app
2. Confirms with a 6-digit code → 2FA is activated
3. On next login, after entering email/password, a TOTP code is required
4. User can disable 2FA from the dashboard at any time

**API Endpoints:**
- `POST /api/2fa/setup` — Generate TOTP secret + QR URI (authenticated)
- `POST /api/2fa/enable` — Verify code and enable 2FA (authenticated)
- `POST /api/2fa/disable` — Disable 2FA (authenticated)
- `POST /api/auth/2fa/verify` — Verify TOTP code during login (public)

---

## ⏱️ Rate Limiting

Auth endpoints (`/api/auth/**`) are rate-limited to prevent brute-force attacks.

| Setting | Default | Env Variable |
|---------|---------|-------------|
| Requests per minute (per IP) | 30 | `RATE_LIMIT_RPM` |

When the limit is exceeded, the API returns HTTP `429 Too Many Requests`.

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

<br>
<hr>
<br>

<a id="español"></a>
# 🔐 AuthForge (Español)

> Kit de inicio de autenticación listo para producción para Spring Boot

![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot 3.2](https://img.shields.io/badge/Spring%20Boot-3.2-green?logo=spring)
![Spring Security 6](https://img.shields.io/badge/Spring%20Security-6-green?logo=springsecurity)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Ready-blue?logo=docker)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow)
![Coverage](https://img.shields.io/badge/Coverage-100%25-brightgreen.svg)

---

## ✨ Características

| Característica | Estado |
|---------|--------|
| Autenticación JWT (Tokens de Acceso + Refresco) | ✅ |
| Hashing de Contraseñas con BCrypt | ✅ |
| Control de Acceso Basado en Roles (USER, ADMIN) | ✅ |
| Refresco de Tokens con Rotación | ✅ |
| Recuperación de Contraseña (token de reseteo) | ✅ |
| Configuración de CORS | ✅ |
| Manejo Global de Excepciones | ✅ |
| Demostración Frontend (Login, Registro, Panel, Admin) | ✅ |
| Docker Compose (PostgreSQL + Backend + Frontend) | ✅ |
| OAuth2 (Google, GitHub) | ✅ |
| Autenticación de Dos Factores (TOTP) | ✅ |
| Límite de Peticiones - Rate Limiting (Bucket4j) | ✅ |
| Feature Flags (Activar características mediante variables de entorno) | ✅ |
| Servicio de Email (Verificación + Reseteo de contraseña) | ✅ |
| Pruebas Unitarias + Cobertura JaCoCo (100%) | ✅ |
| Calidad de Código SonarQube (0 Bugs/Smells) | ✅ |

---

## 🚀 Inicio Rápido

### Requisitos Previos

- [Docker](https://docs.docker.com/get-docker/) y [Docker Compose](https://docs.docker.com/compose/install/)

### Ejecutar con Docker (recomendado)

```bash
git clone https://github.com/FirstOnDie/authforge.git
cd authforge
docker-compose up --build
```

Abre **http://localhost:4000** → ¡Listo! 🎉

### Puertos por defecto

| Servicio | Puerto | URL |
|---------|------|-----|
| Frontend (Nginx) | 4000 | http://localhost:4000 |
| Backend (Spring Boot) | 8090 | http://localhost:8090 |
| PostgreSQL | 5433 | — |
| MailHog (Interfaz de Email) | 8025 | http://localhost:8025 |
| SonarQube (separado) | 9000 | http://localhost:9000 |

---

## 🎛️ Feature Flags (Banderas de Características)

Activa o desactiva funcionalidades a través de variables de entorno — sin necesidad de cambiar el código.

| Bandera | Variable de Entorno | Por Defecto |
|------|-------------|---------|
| Login con OAuth2 | `FEATURE_OAUTH2` | `true` |
| Autenticación 2FA | `FEATURE_2FA` | `true` |
| Límite de Peticiones | `FEATURE_RATE_LIMIT` | `true` |
| Verificación de Email | `FEATURE_EMAIL` | `true` |

Las banderas activas están expuestas en `GET /api/admin/features` (solo para el rol admin).

---

## 📧 Servicio de Email

Utiliza **MailHog** en Docker para pruebas de correo locales (no se envían correos reales).

- **Correos de verificación**: se envían al registrarse cuando `FEATURE_EMAIL=true`
- **Correos de recuperación de contraseña**: Correos en HTML con enlaces de reseteo
- **Interfaz MailHog**: http://localhost:8025 para ver todos los correos capturados

---

## 🧪 Pruebas, Cobertura y SonarQube

AuthForge asegura una alta calidad de código y fiabilidad.

### Ejecutar pruebas localmente
```bash
cd backend
mvn clean test
```
Reporte de cobertura de JaCoCo: `backend/target/site/jacoco/index.html`

### Análisis con SonarQube
Se ejecuta en un archivo **Docker Compose independiente** para mantener el stack principal ligero. Aplica una estricta Quality Gate (0 Bugs, 0 Vulnerabilidades, 0 Code Smells, 100% de Cobertura).

```bash
# Iniciar SonarQube
docker-compose -f docker-compose.sonar.yml up -d

# Esperar ~1 minuto para que inicie, luego ejecutar análisis
cd backend
mvn clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.login=admin -Dsonar.password=admin
```

Credenciales por defecto: `admin` / `admin` (cámbialas en el primer inicio de sesión).

---

## 📡 Endpoints de la API

### Autenticación (Público)
```text
POST /api/auth/register        → Registrar un nuevo usuario
POST /api/auth/login           → Iniciar sesión → tokens JWT
POST /api/auth/refresh         → Refrescar token de acceso
POST /api/auth/logout          → Invalidar token de refresco
POST /api/auth/forgot-password → Generar token de reseteo de contraseña
POST /api/auth/reset-password  → Resetear contraseña con el token
```

### Usuario (Autenticado)
```text
GET  /api/users/me             → Perfil del usuario actual
```

### Admin (Rol ADMIN)
```text
GET  /api/admin/users          → Listar todos los usuarios
PUT  /api/admin/users/{id}/role → Cambiar el rol de un usuario
```

---

## 🏗️ Arquitectura

```text
┌─────────────────────────────────────────────┐
│                Docker Compose               │
├──────────┬──────────────┬───────────────────┤
│ Frontend │   Backend    │    PostgreSQL     │
│ Nginx    │ Spring Boot  │                   │
│ :4000    │ :8090        │    :5433          │
│          │              │                   │
│ HTML/CSS │ Controladores│  tabla users      │
│ JS       │ Servicios    │  refresh_tokens   │
│          │ Seguridad    │                   │
│  /api/* ──► Filtro JWT  │                   │
│          │  BCrypt      │                   │
└──────────┴──────────────┴───────────────────┘
```

---

## 📁 Estructura del Proyecto

```text
authforge/
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/authforge/
│       ├── config/
│       ├── controller/
│       ├── dto/
│       ├── exception/
│       ├── model/
│       ├── repository/
│       ├── security/
│       └── service/
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── index.html
│   ├── css/
│   └── js/
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

---

## ⚙️ Configuración

Toda la configuración se realiza a través de variables de entorno (ver `.env.example`):

| Variable | Por Defecto | Descripción |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://postgres:5432/authforge` | URL de la base de datos |
| `DB_USERNAME` | `authforge` | Usuario de la base de datos |
| `DB_PASSWORD` | `authforge` | Contraseña de la base de datos |
| `JWT_SECRET` | (¡cámbiame!) | Clave de firma HMAC-SHA256 |
| `CORS_ORIGINS` | `http://localhost:4000` | Orígenes CORS permitidos |
| `GOOGLE_CLIENT_ID` | — | Client ID de Google OAuth2 |
| `GOOGLE_CLIENT_SECRET` | — | Client Secret de Google OAuth2 |
| `GITHUB_CLIENT_ID` | — | Client ID de GitHub OAuth2 |
| `GITHUB_CLIENT_SECRET` | — | Client Secret de GitHub OAuth2 |
| `OAUTH2_REDIRECT_URI` | `http://localhost:4000` | URL de redirección al Frontend tras OAuth2 |

---

## 🔑 Configuración de OAuth2 (Google y GitHub)

### Google
1. Ve a [Google Cloud Console](https://console.cloud.google.com/) → APIs & Services → Credentials
2. Crea un **OAuth 2.0 Client ID** (Aplicación Web)
3. Configura la URI de redirección autorizada: `http://localhost:8090/login/oauth2/code/google`
4. Copia el Client ID y el Client Secret en tu archivo `.env`

### GitHub
1. Ve a [GitHub Settings](https://github.com/settings/developers) → OAuth Apps → New OAuth App
2. Configura la URL de Autorización (callback): `http://localhost:8090/login/oauth2/code/github`
3. Copia el Client ID y el Client Secret en tu archivo `.env`

---

## 🛡️ Autenticación de Dos Factores (TOTP)

AuthForge soporta 2FA basado en TOTP, compatible con Google Authenticator, Authy, y aplicaciones similares.

**Flujo:**
1. El usuario habilita el 2FA desde el panel de control → escanea el código QR con la app autenticadora
2. Confirma con un código de 6 dígitos → 2FA es activado
3. En el siguiente inicio de sesión, tras introducir email/contraseña, se requiere un código TOTP
4. El usuario puede deshabilitar el 2FA desde el panel en cualquier momento

**Endpoints de la API:**
- `POST /api/2fa/setup` — Generar secreto TOTP + URI del QR (autenticado)
- `POST /api/2fa/enable` — Verificar código y habilitar 2FA (autenticado)
- `POST /api/2fa/disable` — Deshabilitar 2FA (autenticado)
- `POST /api/auth/2fa/verify` — Verificar código TOTP durante el login (público)

---

## ⏱️ Límite de Peticiones (Rate Limiting)

Los endpoints de autenticación (`/api/auth/**`) tienen un límite de peticiones para prevenir ataques de fuerza bruta.

| Configuración | Por Defecto | Variable de Entorno |
|---------|---------|-------------|
| Peticiones por minuto (por IP) | 30 | `RATE_LIMIT_RPM` |

Cuando se supera el límite, la API retorna un error HTTP `429 Too Many Requests`.

---

## 🧪 Probando la API

```bash
# Registrar
curl -X POST http://localhost:8090/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"John","email":"john@test.com","password":"password123"}'

# Iniciar Sesión
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@test.com","password":"password123"}'

# Obtener perfil (reemplaza TOKEN)
curl -X GET http://localhost:8090/api/users/me \
  -H "Authorization: Bearer TOKEN"
```

---

## 📋 Hoja de Ruta (Roadmap)

- [x] **v1.0** — Autenticación JWT, Roles, Recuperación de Contraseña, Docker
- [x] **v1.1** — OAuth2 (Google, GitHub)
- [x] **v1.2** — 2FA (TOTP), Rate Limiting
- [x] **v2.0** — Servicio de Email, Verificación de Cuentas, Feature Flags, 100% Cobertura, Calidad en SonarQube

---

## 📜 Licencia

[MIT](LICENSE) — Usa este kit de inicio libremente en tus proyectos.

---

<p align="center">
  Construido con ☕ Java 21 + 🍃 Spring Boot 3 + 🛡️ Spring Security 6
  <br>
  <strong>por <a href="https://github.com/FirstOnDie">Carlos Expósito</a></strong>
</p>
