# RoleGuard API

A RESTful API built with **Spring Boot 4** and **Java 21** that provides user management with role-based access control (RBAC) foundations. It features a clean **Hexagonal Architecture**, BCrypt password hashing, optimistic locking, paginated endpoints, and a full test suite — ready to be extended with JWT authentication.

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
| Security | Spring Security 6 + BCrypt |
| Mapping | MapStruct 1.6.3 |
| Validation | Jakarta Bean Validation |
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
│          (IUserUseCase, IRoleUseCase — interfaces)       │
└──────────────────────┬───────────────────────────────────┘
                       │ implemented by
┌──────────────────────▼───────────────────────────────────┐
│                  Application Services                    │
│             (UserService, RoleService — @Service)        │
└──────────────────────┬───────────────────────────────────┘
                       │ calls
┌──────────────────────▼───────────────────────────────────┐
│                    Output Ports                          │
│    (IUserRepositoryPort, IRoleRepositoryPort — interfaces)│
└──────────────────────┬───────────────────────────────────┘
                       │ implemented by
┌──────────────────────▼───────────────────────────────────┐
│                  Outbound Adapters                       │
│    (UserRepositoryAdapter, RoleRepositoryAdapter — JPA)  │
└──────────────────────────────────────────────────────────┘
```

---

## Project Structure

```
roleguard/roleguard/
├── src/main/java/com/nsalazar/roleguard/
│   ├── RoleguardApplication.java
│   ├── user/
│   │   ├── domain/
│   │   │   └── model/User.java                   ← JPA entity
│   │   ├── application/
│   │   │   ├── port/
│   │   │   │   ├── in/IUserUseCase.java           ← input port
│   │   │   │   └── out/IUserRepositoryPort.java   ← output port
│   │   │   ├── service/UserService.java           ← business logic
│   │   │   └── mapper/IUserMapper.java            ← MapStruct mapper
│   │   └── infrastructure/
│   │       ├── adapter/
│   │       │   ├── in/UserController.java         ← REST controller
│   │       │   └── out/UserRepositoryAdapter.java ← JPA adapter
│   │       ├── persistence/IJpaUserRepository.java
│   │       └── dto/
│   │           ├── CreateUserRequest.java
│   │           ├── UpdateUserRequest.java
│   │           └── UserResponse.java
│   ├── role/                                      ← identical structure
│   └── shared/
│       ├── exception/
│       │   ├── ResourceNotFoundException.java
│       │   ├── DuplicateResourceException.java
│       │   ├── ErrorResponse.java
│       │   └── GlobalExceptionHandler.java
│       └── config/SecurityConfig.java
├── src/main/resources/
│   ├── application.yaml
│   └── application-test.yaml
└── src/test/java/com/nsalazar/roleguard/
    ├── user/application/service/UserServiceTest.java
    ├── user/infrastructure/adapter/in/UserControllerTest.java
    ├── role/application/service/RoleServiceTest.java
    ├── role/infrastructure/adapter/in/RoleControllerTest.java
    └── RoleguardApplicationTests.java
```

---

## Key Concepts Applied

### Hexagonal Architecture (Ports & Adapters)
Domain logic lives in `application/service` and is completely decoupled from HTTP, JPA, or any framework. Controllers and repositories are interchangeable adapters — swapping MySQL for PostgreSQL or REST for gRPC requires zero changes to the domain.

### Role-Based Access Control (RBAC) Foundation
Users are assigned a single `Role` (e.g., `ADMIN`, `USER`). The schema and service layer are designed to support Spring Security authority resolution once JWT is added.

### Optimistic Locking
Both `User` and `Role` entities carry a `@Version Long version` field. If two concurrent requests try to update the same record simultaneously, Hibernate throws `ObjectOptimisticLockingFailureException`, which the global handler converts to **HTTP 409 Conflict** — preventing silent data overwrites.

### BCrypt Password Hashing
Passwords are hashed with `BCryptPasswordEncoder` before persistence. The raw password never touches the database, is never logged (excluded from `toString()`), and is never returned in responses.

### JPA Lifecycle Callbacks (`@PreUpdate`)
`createdAt` is set once on INSERT using `@CreationTimestamp`. `updatedAt` uses a `@PreUpdate` callback so it stays `null` on creation and is only populated when the record is actually modified — a semantic distinction lost when using `@UpdateTimestamp` alone.

### MapStruct DTO Mapping
Entity-to-DTO and DTO-to-entity conversions are handled by generated code (MapStruct), keeping service methods free of manual field assignments and ensuring compile-time safety.

### Bean Validation + Global Exception Handler
All request bodies are validated with Jakarta annotations (`@NotBlank`, `@Email`, `@Size`, `@Pattern`). A single `@RestControllerAdvice` translates every exception — validation, business, concurrency, FK violations — into a consistent JSON error envelope.

### Paginated Endpoints
All list endpoints accept `page`, `size`, and `sort` query parameters and return Spring's `Page<T>` structure, enabling cursor-free pagination from day one.

### Test Strategy
- **Unit tests** (Mockito): service methods are tested in isolation with mocked repository ports.
- **Integration tests** (MockMvc + `@WebMvcTest`): controllers are tested end-to-end through the HTTP layer with a mocked service, covering happy paths, validation failures, and business error responses.
- **In-memory DB**: the `test` Spring profile swaps MySQL for H2 with `create-drop`, so tests never require an external database.

---

## API Reference

Base URL: `http://localhost:8080/api/v1`

### Users

| Method | Endpoint | Description | Success |
|--------|----------|-------------|---------|
| `GET` | `/users` | List all users (paginated) | 200 |
| `GET` | `/users/{id}` | Get user by ID | 200 |
| `GET` | `/users/email/{email}` | Get user by email | 200 |
| `POST` | `/users` | Create a new user | 201 |
| `PUT` | `/users/{id}` | Partially update a user | 200 |
| `DELETE` | `/users/{id}` | Delete a user | 204 |

#### Query Parameters (list endpoints)

| Param | Default | Constraints |
|-------|---------|-------------|
| `page` | `0` | `>= 0` |
| `size` | `20` | `1 – 100` |
| `sort` | — | e.g. `username,asc` |

#### `POST /users` — Request Body

```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "secret123",
  "enabled": true,
  "roleId": 1
}
```

| Field | Required | Rules |
|-------|----------|-------|
| `username` | Yes | 3–50 characters |
| `email` | Yes | Valid email format |
| `password` | Yes | Minimum 8 characters |
| `enabled` | No | Defaults to `true` |
| `roleId` | No | Must reference an existing role |

#### `PUT /users/{id}` — Request Body (all fields optional)

```json
{
  "username": "newname",
  "email": "newemail@example.com",
  "password": "newpassword123",
  "enabled": false
}
```

#### `GET /users/{id}` — Response

```json
{
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "roleName": "USER",
  "enabled": true,
  "version": 0,
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": null
}
```

---

### Roles

| Method | Endpoint | Description | Success |
|--------|----------|-------------|---------|
| `GET` | `/roles` | List all roles (paginated) | 200 |
| `GET` | `/roles/{id}` | Get role by ID | 200 |
| `GET` | `/roles/name/{name}` | Get role by name | 200 |
| `POST` | `/roles` | Create a new role | 201 |
| `PUT` | `/roles/{id}` | Partially update a role | 200 |
| `DELETE` | `/roles/{id}` | Delete a role | 204 |

#### `POST /roles` — Request Body

```json
{
  "name": "ADMIN"
}
```

| Field | Required | Rules |
|-------|----------|-------|
| `name` | Yes | 2–50 chars, uppercase, pattern `^[A-Z][A-Z0-9_]*$` |

> **Note:** Deleting a role that has users assigned returns **409 Conflict**.

---

## Error Responses

All errors follow a consistent envelope:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: '99'",
  "timestamp": "2025-01-15T10:30:00"
}
```

| HTTP Status | Cause |
|-------------|-------|
| 400 | Validation failure (field errors concatenated in `message`) |
| 404 | Resource not found |
| 409 | Duplicate resource, FK constraint violation, or optimistic lock conflict |
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

### 2. Create the database

```sql
CREATE DATABASE roleguard_db;
```

> The datasource URL includes `createDatabaseIfNotExist=true`, so this step is optional if your MySQL user has `CREATE` privileges.

### 3. Configure credentials

The application reads credentials from environment variables with sensible defaults:

```bash
export DB_USERNAME=your_mysql_user
export DB_PASSWORD=your_mysql_password
```

Or edit `roleguard/roleguard/src/main/resources/application.yaml` directly.

### 4. Run the application

```bash
cd roleguard/roleguard
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

### 5. Quick smoke test

```bash
# Create a role
curl -s -X POST http://localhost:8080/api/v1/roles \
  -H "Content-Type: application/json" \
  -d '{"name":"USER"}' | jq .

# Create a user
curl -s -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"username":"johndoe","email":"john@example.com","password":"secret123","roleId":1}' | jq .

# List users
curl -s "http://localhost:8080/api/v1/users?page=0&size=10" | jq .
```

---

## Running Tests

Tests use an H2 in-memory database — no MySQL required.

```bash
cd roleguard/roleguard

# Run all tests
./mvnw test -Dspring.profiles.active=test

# Run a specific test class
./mvnw test -Dtest=UserServiceTest -Dspring.profiles.active=test

# Run a specific test method
./mvnw test -Dtest=UserServiceTest#createUser -Dspring.profiles.active=test

# Build without tests
./mvnw clean package -DskipTests
```

Current test count: **63 tests, 0 failures**.

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | `root` | MySQL password |

---

## Roadmap

- [ ] JWT authentication (access + refresh tokens)
- [ ] Role-based endpoint authorization (`@PreAuthorize`)
- [ ] Token blacklist / logout support
- [ ] Refresh token rotation
- [ ] Swagger / OpenAPI documentation
- [ ] Docker Compose setup
