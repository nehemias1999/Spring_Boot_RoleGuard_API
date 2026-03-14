# RoleGuard API

A RESTful API built with **Spring Boot 4** and **Java 21** that provides JWT authentication, full role-based access control (RBAC) with granular permissions, and protected endpoints. Built on **Hexagonal Architecture** with BCrypt password hashing, refresh token rotation, rate limiting, optimistic locking, paginated endpoints, and a full test suite.

---

## Table of Contents

- [Technologies](#technologies)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Key Concepts Applied](#key-concepts-applied)
- [API Reference](#api-reference)
- [Error Responses](#error-responses)
- [Getting Started](#getting-started)
- [Running Tests](#running-tests)
- [Environment Variables](#environment-variables)
- [Roadmap](#roadmap)

---

## Technologies

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.0.3 |
| Language | Java 21 |
| Persistence | Spring Data JPA + Hibernate |
| Database | MySQL 8+ (production) / H2 (tests) |
| Security | Spring Security 6 + JWT (jjwt 0.12) + BCrypt |
| Mapping | MapStruct 1.6.3 |
| Validation | Jakarta Bean Validation |
| Rate Limiting | Bucket4j |
| Boilerplate | Lombok |
| Build | Maven Wrapper |
| Testing | JUnit 5 + Mockito + MockMvc |

---

## Architecture

The project follows **Hexagonal Architecture** (Ports & Adapters), keeping the domain completely isolated from frameworks and infrastructure.

```
┌──────────────────────────────────────────────────────────┐
│                     Inbound Adapters                     │
│              (@RestController — HTTP layer)              │
└──────────────────────┬───────────────────────────────────┘
                       │ calls
┌──────────────────────▼───────────────────────────────────┐
│                    Input Ports                           │
│   (IUserUseCase, IRoleUseCase, IAuthUseCase — interfaces)│
└──────────────────────┬───────────────────────────────────┘
                       │ implemented by
┌──────────────────────▼───────────────────────────────────┐
│                  Application Services                    │
│        (UserService, RoleService, AuthService)           │
└──────────────────────┬───────────────────────────────────┘
                       │ calls
┌──────────────────────▼───────────────────────────────────┐
│                    Output Ports                          │
│  (IUserRepositoryPort, IRoleRepositoryPort — interfaces) │
└──────────────────────┬───────────────────────────────────┘
                       │ implemented by
┌──────────────────────▼───────────────────────────────────┐
│                  Outbound Adapters                       │
│    (UserRepositoryAdapter, RoleRepositoryAdapter — JPA)  │
└──────────────────────────────────────────────────────────┘
```

**Security filter chain** (runs before controllers):

```
Request → RateLimitFilter → JwtAuthFilter → Spring Security → Controller
```

---

## Project Structure

```
roleguard/roleguard/
├── src/main/java/com/nsalazar/roleguard/
│   ├── RoleguardApplication.java
│   ├── auth/                              ← authentication slice
│   │   ├── domain/model/RefreshToken.java
│   │   ├── domain/port/in/IAuthUseCase.java
│   │   ├── domain/port/out/IRefreshTokenRepositoryPort.java
│   │   ├── application/service/AuthService.java
│   │   ├── application/service/RefreshTokenService.java
│   │   ├── application/dto/               ← LoginRequest, RegisterRequest, AuthResponse, …
│   │   └── infrastructure/adapter/in/AuthController.java
│   ├── user/
│   │   ├── domain/model/User.java
│   │   ├── domain/port/in/IUserUseCase.java
│   │   ├── domain/port/out/IUserRepositoryPort.java
│   │   ├── application/service/UserService.java
│   │   ├── application/mapper/IUserMapper.java
│   │   ├── application/dto/               ← CreateUserRequest, UpdateUserRequest, UserResponse
│   │   └── infrastructure/adapter/in/UserController.java
│   ├── role/                              ← identical structure
│   ├── permission/                        ← identical structure
│   └── shared/
│       ├── config/
│       │   ├── SecurityConfig.java
│       │   ├── DataInitializer.java       ← seeds roles, permissions, admin user
│       │   ├── JpaAuditingConfig.java
│       │   └── AuditorAwareImpl.java
│       ├── security/
│       │   ├── JwtService.java
│       │   ├── JwtAuthFilter.java
│       │   ├── RateLimitFilter.java
│       │   ├── UserDetailsServiceImpl.java
│       │   ├── UserPrincipal.java
│       │   ├── UserSecurityService.java
│       │   ├── JwtAuthEntryPoint.java
│       │   └── JwtAccessDeniedHandler.java
│       └── exception/
│           ├── GlobalExceptionHandler.java
│           ├── ResourceNotFoundException.java
│           ├── DuplicateResourceException.java
│           ├── ResourceInUseException.java
│           └── ErrorResponse.java
├── src/main/resources/
│   ├── application.yaml
│   └── application-test.yaml
└── src/test/java/com/nsalazar/roleguard/
    ├── auth/application/service/AuthServiceTest.java
    ├── auth/infrastructure/adapter/in/AuthControllerTest.java
    ├── auth/AuthIntegrationTest.java
    ├── user/application/service/UserServiceTest.java
    ├── user/infrastructure/adapter/in/UserControllerTest.java
    ├── role/application/service/RoleServiceTest.java
    ├── role/infrastructure/adapter/in/RoleControllerTest.java
    ├── permission/infrastructure/adapter/in/PermissionControllerTest.java
    └── RoleguardApplicationTests.java
```

---

## Key Concepts Applied

### Hexagonal Architecture (Ports & Adapters)
Domain logic lives in `application/service` and is completely decoupled from HTTP, JPA, or any framework. Controllers and repositories are interchangeable adapters — swapping MySQL for PostgreSQL or REST for gRPC requires zero changes to the domain.

### JWT Authentication (Stateless)
Every request is authenticated via a signed JWT (`Authorization: Bearer <token>`). The `JwtAuthFilter` validates the token and populates the `SecurityContext` before the request reaches any controller. Sessions are `STATELESS` — no `HttpSession` is ever created.

### Refresh Token Rotation
On login or register, both an **access token** (24h) and a **refresh token** (7d) are issued. `POST /auth/refresh` validates the refresh token, deletes it, and issues a new pair — preventing replay attacks. One refresh token per user is enforced.

### Permission-Based RBAC
Authorization is enforced at the method level using `@PreAuthorize` with granular permissions (e.g., `USER_READ`, `ROLE_ASSIGN`, `PERMISSION_CREATE`). Permissions are stored in the database and assigned to roles. The default seed on startup creates four roles:

| Role | Permissions |
|------|-------------|
| `ADMIN` | All permissions |
| `MODERATOR` | User/role management, read permissions |
| `SUPPORT` | `USER_READ`, `ROLE_READ` |
| `USER` | `USER_READ` |

### Self-Update Authorization
`PUT /users/{id}` allows the resource owner to update their own account (via `@userSecurity.isSelf`) **or** any caller with `USER_UPDATE` authority. The `enabled` field is restricted to `USER_UPDATE` holders only — a regular user cannot disable their own account.

### Rate Limiting
`POST /auth/login` is rate-limited to **10 requests per minute per IP** using Bucket4j token buckets. Excess requests receive `429 Too Many Requests`.

### BCrypt Password Hashing
Passwords are hashed with `BCryptPasswordEncoder` before persistence. The raw password never touches the database, is never logged, and is never returned in responses.

### Optimistic Locking
`User` and `Role` entities carry a `@Version Long version` field. Concurrent updates on the same row throw `ObjectOptimisticLockingFailureException`, which the global handler converts to **HTTP 409 Conflict**.

### JPA Auditing
`createdBy` / `updatedBy` are populated automatically by Spring Data JPA Auditing via `AuditorAwareImpl`, which reads the current principal's username from the `SecurityContext`.

### MapStruct DTO Mapping
Entity-to-DTO and DTO-to-entity conversions are handled by generated MapStruct code, keeping service methods free of manual field assignments.

### Bean Validation + Global Exception Handler
All request bodies are validated with Jakarta annotations (`@NotBlank`, `@Email`, `@Size`, `@Pattern`). A single `@RestControllerAdvice` translates every exception — validation, business, security, concurrency, FK violations — into a consistent JSON error envelope.

### Paginated Endpoints
All list endpoints accept `page`, `size`, and `sort` query parameters and return Spring's `Page<T>` structure.

---

## API Reference

Base URL: `http://localhost:8080/api/v1`

### Authentication

| Method | Endpoint | Auth required | Description | Success |
|--------|----------|:---:|-------------|---------:|
| `POST` | `/auth/register` | — | Register a new user | 201 |
| `POST` | `/auth/login` | — | Login and get tokens | 200 |
| `POST` | `/auth/refresh` | — | Rotate refresh token | 200 |
| `POST` | `/auth/logout` | — | Revoke refresh token | 204 |
| `GET`  | `/auth/me` | JWT | Get own profile | 200 |
| `PUT`  | `/auth/me/password` | JWT | Change own password | 204 |

#### `POST /auth/register` — Request Body

```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "secret123"
}
```

#### `POST /auth/login` — Request Body

```json
{
  "username": "johndoe",
  "password": "secret123"
}
```

#### `POST /auth/login` and `/auth/register` — Response

```json
{
  "token": "<access-jwt>",
  "type": "Bearer",
  "username": "johndoe",
  "role": "USER",
  "refreshToken": "<refresh-token-uuid>"
}
```

#### `POST /auth/refresh` — Request Body

```json
{ "refreshToken": "<refresh-token-uuid>" }
```

#### `PUT /auth/me/password` — Request Body

```json
{
  "currentPassword": "secret123",
  "newPassword": "newSecret456"
}
```

---

### Users

> All endpoints require a valid JWT. Fine-grained access is controlled by `@PreAuthorize`.

| Method | Endpoint | Required authority | Description | Success |
|--------|----------|--------------------|-------------|---------:|
| `GET` | `/users` | `USER_READ` | List all users (paginated) | 200 |
| `GET` | `/users/{id}` | `USER_READ` | Get user by ID | 200 |
| `GET` | `/users/email/{email}` | `USER_READ` | Get user by email | 200 |
| `POST` | `/users` | `USER_CREATE` | Create a user (admin) | 201 |
| `PUT` | `/users/{id}` | `USER_UPDATE` or self | Partially update a user | 200 |
| `PUT` | `/users/{id}/roles/{roleId}` | `ROLE_ASSIGN` | Assign a role to a user | 200 |
| `DELETE` | `/users/{id}` | `USER_DELETE` | Delete a user | 204 |

> **Self-update rule:** a user can call `PUT /users/{id}` on their own account without `USER_UPDATE`. The `enabled` field can only be modified by a caller with `USER_UPDATE`.

#### `POST /users` — Request Body

```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "secret123",
  "enabled": true,
  "roleId": "<uuid>"
}
```

| Field | Required | Rules |
|-------|:--------:|-------|
| `username` | Yes | 3–50 characters |
| `email` | Yes | Valid email format |
| `password` | Yes | Minimum 8 characters |
| `enabled` | No | Defaults to `true` |
| `roleId` | No | UUID of an existing role |

#### `PUT /users/{id}` — Request Body (all fields optional)

```json
{
  "username": "newname",
  "email": "newemail@example.com",
  "password": "newpassword123",
  "enabled": false
}
```

#### User Response

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "username": "johndoe",
  "email": "john@example.com",
  "roleName": "USER",
  "enabled": true,
  "version": 0,
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": null
}
```

#### Query Parameters (list endpoints)

| Param | Default | Constraints |
|-------|---------|-------------|
| `page` | `0` | `>= 0` |
| `size` | `20` | `1 – 100` |
| `sort` | `id` | field name |

---

### Roles

| Method | Endpoint | Required authority | Description | Success |
|--------|----------|--------------------|-------------|---------:|
| `GET` | `/roles` | `ROLE_READ` | List all roles (paginated) | 200 |
| `GET` | `/roles/{id}` | `ROLE_READ` | Get role by ID | 200 |
| `GET` | `/roles/name/{name}` | `ROLE_READ` | Get role by name | 200 |
| `POST` | `/roles` | `ROLE_CREATE` | Create a role | 201 |
| `PUT` | `/roles/{id}` | `ROLE_UPDATE` | Partially update a role | 200 |
| `DELETE` | `/roles/{id}` | `ROLE_DELETE` | Delete a role | 204 |
| `PUT` | `/roles/{id}/permissions/{permissionId}` | `PERMISSION_ASSIGN` | Assign permission to role | 200 |
| `DELETE` | `/roles/{id}/permissions/{permissionId}` | `PERMISSION_ASSIGN` | Remove permission from role | 200 |

> Deleting a role that has users assigned returns **409 Conflict**.

---

### Permissions

| Method | Endpoint | Required authority | Description | Success |
|--------|----------|--------------------|-------------|---------:|
| `GET` | `/permissions` | `PERMISSION_READ` | List all permissions (paginated) | 200 |
| `GET` | `/permissions/{id}` | `PERMISSION_READ` | Get permission by ID | 200 |
| `GET` | `/permissions/name/{name}` | `PERMISSION_READ` | Get permission by name | 200 |
| `POST` | `/permissions` | `PERMISSION_CREATE` | Create a permission | 201 |
| `PUT` | `/permissions/{id}` | `PERMISSION_UPDATE` | Partially update a permission | 200 |
| `DELETE` | `/permissions/{id}` | `PERMISSION_DELETE` | Delete a permission | 204 |

> Deleting a permission assigned to a role returns **409 Conflict**.

---

## Error Responses

All errors follow a consistent envelope:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: '99999999-9999-9999-9999-999999999999'",
  "timestamp": "2025-01-15T10:30:00"
}
```

| HTTP Status | Cause |
|:-----------:|-------|
| 400 | Validation failure (field errors concatenated in `message`) |
| 401 | Missing or invalid JWT / wrong credentials |
| 403 | Valid JWT but insufficient authority |
| 404 | Resource not found |
| 409 | Duplicate resource, FK constraint violation, or optimistic lock conflict |
| 429 | Rate limit exceeded (login endpoint) |
| 500 | Unexpected server error |

---

## Getting Started

### Prerequisites

- Java 21+
- MySQL 8+ running locally (or via Docker)
- Maven (or use the included `./mvnw` wrapper)

### 1. Clone the repository

```bash
git clone <repo-url>
cd Spring_Boot_RoleGuard_API
```

### 2. Configure environment variables

```bash
export DB_USERNAME=your_mysql_user
export DB_PASSWORD=your_mysql_password

# Optional — defaults are provided for development only
export JWT_SECRET=<base64-encoded-secret-min-32-bytes>
```

> **Important:** the default `JWT_SECRET` is a dev-only placeholder. Always override it in production with a strong, randomly generated Base64-encoded key.

### 3. Run the application

```bash
cd roleguard/roleguard
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

On first startup, `DataInitializer` automatically seeds:
- Default permissions (`USER_READ`, `USER_CREATE`, … `PERMISSION_DELETE`)
- Default roles (`ADMIN`, `MODERATOR`, `SUPPORT`, `USER`) with their permission sets
- A built-in `ADMIN` user (username: `ADMIN`, password: `ADMIN`) — **change this immediately in production**

### 4. Quick smoke test

```bash
# Login as the default admin
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"ADMIN","password":"ADMIN"}' | jq -r '.token')

# Get own profile
curl -s http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer $TOKEN" | jq .

# List users
curl -s "http://localhost:8080/api/v1/users?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN" | jq .

# Create a new user
curl -s -X POST http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"johndoe","email":"john@example.com","password":"secret123"}' | jq .
```

---

## Running Tests

Tests use an H2 in-memory database — no MySQL required.

```bash
cd roleguard/roleguard

# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=UserServiceTest

# Run a specific test method
./mvnw test -Dtest=UserServiceTest#createUser

# Build without tests
./mvnw clean package -DskipTests
```

Current test count: **145 tests, 0 failures**.

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | `root` | MySQL password |
| `JWT_SECRET` | *(dev placeholder)* | Base64-encoded HMAC-SHA256 signing key |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Access token lifetime in milliseconds |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` (7d) | Refresh token lifetime in milliseconds |

---

## Roadmap

- [x] JWT authentication (access + refresh tokens)
- [x] Refresh token rotation
- [x] Role-based endpoint authorization (`@PreAuthorize` with granular permissions)
- [x] Self-update rule (resource owner or privileged authority)
- [x] Rate limiting on login endpoint (Bucket4j)
- [x] JPA Auditing (`createdBy` / `updatedBy`)
- [ ] Access token revocation on logout (blacklist / JTI)
- [ ] Swagger / OpenAPI documentation
- [ ] Docker Compose setup
