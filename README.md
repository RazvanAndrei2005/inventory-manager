# 📦 Inventory Manager REST API

![Status](https://img.shields.io/badge/Status-In%20Progress-yellow?style=for-the-badge&logo=github)

A RESTful backend API built with **Spring Boot 4** for managing products, product categories, and users in an inventory system. It features stateless **JWT authentication**, **role-based authorization**, dynamic product filtering, pagination, optimistic locking, **PostgreSQL with Flyway migrations**, and OpenAPI/Swagger documentation.

---

## 🛠️ Tech Stack

* **Java**: 23
* **Framework**: Spring Boot 4.1.1
* **Security**: Spring Security + JWT (JJWT 0.12.6)
* **Database**: PostgreSQL 16 (via Docker Compose)
* **Migrations**: Flyway (`V1__init_schema.sql`), Hibernate set to `ddl-auto=validate`
* **ORM & Persistence**: Spring Data JPA / Hibernate
* **Validation**: Spring Validation (Jakarta Bean Validation)
* **Documentation**: SpringDoc OpenAPI 3 (Swagger UI)
* **Boilerplate reduction**: Lombok
* **Build Tool**: Maven
* **Containers**: Docker Compose (PostgreSQL)

---

## ✨ Key Features

* **Authentication & Authorization**: Register and log in to receive a JWT; access is controlled by roles (`ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_USER`) using `@PreAuthorize`.
* **User Administration**: Admins can list, view, delete users and change their roles.
* **Category Management**: Full CRUD with a guard that blocks deleting a category that still has products assigned (`409 CONFLICT`).
* **Product Management**: Full CRUD, restocking endpoint (rejects quantities `<= 0`), and low-stock queries.
* **Dynamic Filtering**: Filter products by name, SKU, price range, category, and stock range using JPA Specifications.
* **Pagination & Sorting**: Paginated responses via `Pageable`, integrated with Swagger through `@ParameterObject`.
* **N+1 Prevention**: `@EntityGraph` on product queries.
* **Optimistic Locking**: `@Version` field on `Product` prevents concurrent stock overwrites.
* **Transactional Correctness**: Mutating service methods are `@Transactional`; reads use `@Transactional(readOnly = true)`.
* **Database Versioning**: Schema is managed by Flyway migrations instead of Hibernate auto-DDL.
* **Config & Secrets Hygiene**: All secrets come from environment variables with no secret defaults; separate `dev` / `prod` profiles; `.env` is gitignored.
* **Global Exception Handling**: Unified `ErrorResponseDto` covering validation errors, missing resources, duplicates, access denied, bad credentials, data integrity violations, and generic errors.
* **Data Seeding**: An admin and a manager account are created on startup from environment variables.

---

## 🏗️ Project Structure

Layered architecture under `com.inventory.manager`:

| Package | Contents |
| --- | --- |
| `entity` | `User`, `Product`, `Category`, `Role` |
| `repository` | `UserRepository`, `ProductRepository`, `CategoryRepository` |
| `dto` | Request/response DTOs for all resources |
| `service` | `AuthService`, `AdminService`, `ProductService`, `CategoryService` |
| `controller` | `AuthController`, `AdminController`, `ProductController`, `CategoryController` |
| `security` | Security filter chain (`SpringConfig`), `JwtTokenProvider`, `JwtAuthenticationFilter`, custom user details |
| `specification` | `ProductSpecification` (dynamic filtering) |
| `exception` | `GlobalExceptionHandler`, custom exceptions, `ErrorResponseDto` |
| `config` | `DataSeeder`, `OpenApiConfig` |

Database migrations live in `src/main/resources/db/migration`.

---

## 🚀 Getting Started

### Prerequisites

* **JDK 23**
* **Maven 3.8+**
* **Docker & Docker Compose** (for PostgreSQL)

### Environment Variables

The application reads its secrets and database settings from environment variables (locally you can keep them in a gitignored `.env` file). No secret has a default value, so set all of them before running:

| Variable | Description |
| --- | --- |
| `JWT_SECRET` | Base64-encoded secret key used to sign JWTs |
| `ADMIN_USERNAME` | Seeded admin username |
| `ADMIN_EMAIL` | Seeded admin email |
| `ADMIN_PASSWORD` | Seeded admin password |
| `MANAGER_USERNAME` | Seeded manager username |
| `MANAGER_EMAIL` | Seeded manager email |
| `MANAGER_PASSWORD` | Seeded manager password |
| Database variables | PostgreSQL connection settings (URL, user, password) as defined in `application.properties` / `.env` |

### Installation & Execution

1. **Clone the repository:**
```bash
git clone https://github.com/RazvanAndrei2005/inventory-manager.git
cd inventory-manager
```

2. **Set the environment variables** (example for Linux/macOS):
```bash
export JWT_SECRET=your-base64-encoded-secret
export ADMIN_USERNAME=admin
export ADMIN_EMAIL=admin@example.com
export ADMIN_PASSWORD=change-me
export MANAGER_USERNAME=manager
export MANAGER_EMAIL=manager@example.com
export MANAGER_PASSWORD=change-me
```

3. **Start PostgreSQL:**
```bash
docker compose up -d
```

4. **Build the application:**
```bash
mvn clean package
```

5. **Run the application** (Flyway applies migrations automatically on startup):
```bash
mvn spring-boot:run
```

The API runs on port **8081**.

6. **Useful URLs:**

| Tool | URL |
| --- | --- |
| Swagger UI | `http://localhost:8081/swagger-ui.html` |

---

## 🔐 Authentication

All routes require a valid JWT except `/api/auth/**` and Swagger UI.

1. Register via `POST /api/auth/register` (new users receive `ROLE_USER`), or log in with a seeded account.
2. Call `POST /api/auth/login` to receive a token (response contains `accessToken`, `tokenType`, `username`, `role`).
3. Send it with each request:
```
Authorization: Bearer <token>
```

> Access tokens are currently valid for **7 days**. Refresh tokens are not implemented yet (see Roadmap).

---

## 📑 API Endpoints Reference

### Auth (`/api/auth`)

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Register a new user (unique username/email required) |
| `POST` | `/api/auth/login` | Log in and receive a JWT, token type, username, and role |

### Admin (`/api/admin/users`) — ADMIN only

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/admin/users` | List all users |
| `GET` | `/api/admin/users/{id}` | Get user by ID |
| `PATCH` | `/api/admin/users/{id}/role` | Update a user's role |
| `DELETE` | `/api/admin/users/{id}` | Delete a user |

### Categories (`/api/categories`)

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/categories` | Create a category (ADMIN/MANAGER) |
| `GET` | `/api/categories` | Retrieve all categories (paginated) |
| `GET` | `/api/categories/{id}` | Get category by ID |
| `PUT` | `/api/categories/{id}` | Update a category |
| `DELETE` | `/api/categories/{id}` | Delete a category (`409` if products are still assigned) |

### Products (`/api/products`)

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/products` | Create a product with an associated category |
| `GET` | `/api/products` | Get all products (paginated, sorted, and filterable) |
| `GET` | `/api/products/{id}` | Get product by ID |
| `GET` | `/api/products/search` | Search products by name |
| `GET` | `/api/products/filter` | Filter products by price range |
| `GET` | `/api/products/category/{categoryId}` | Get products by category |
| `GET` | `/api/products/low-stock` | Retrieve products below a stock threshold |
| `PUT` | `/api/products/{id}` | Full update of a product |
| `PATCH` | `/api/products/{id}/restock` | Add stock quantity (`quantity` request param, must be > 0) |
| `DELETE` | `/api/products/{id}` | Delete a product (ADMIN only) |

**Query parameters for `GET /api/products`:** `name`, `sku`, `minPrice`, `maxPrice`, `categoryId`, `minStock`, `maxStock`, plus standard `page`, `size`, and `sort`.

---

## ⚠️ Error Format

All errors return a consistent structure:

```json
{
  "localDateTime": "2026-11-01T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: 42",
  "path": "/api/products/42"
}
```

---

## 📊 Current Status

| Area | State |
| --- | --- |
| Core CRUD (products, categories, users) | ✅ Done |
| JWT auth & role-based access | ✅ Done |
| PostgreSQL + Flyway migration | ✅ Done |
| Config & secrets hardening, dev/prod profiles | ✅ Done |
| Stock movements | 🔜 Planned |
| Orders | 🔜 Planned |
| Refresh tokens | 🔜 Planned |
| Unit & integration tests | 🔜 Planned (none yet) |

---

## 🗺️ Roadmap

In order of priority:

1. **Stock movements** – track every stock change (restock, sale, adjustment)
2. **Orders** – order creation and stock deduction
3. **Refresh tokens** – shorter-lived access tokens with refresh flow
4. **Tests** – unit and integration test coverage
5. **Extras** – Redis caching, mail alerts, CSV/PDF export, image upload
6. **Frontend** – Next.js client
7. **DevOps** – CI, containerized app, deployment
