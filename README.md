# 📦 Inventory Manager REST API

![Status](https://img.shields.io/badge/Status-In%20Progress-yellow?style=for-the-badge&logo=github)

A RESTful backend API built with **Spring Boot 4** for managing products, product categories, and users in an inventory system. It features stateless **JWT authentication**, **role-based authorization**, dynamic product filtering, pagination, optimistic locking, and OpenAPI/Swagger documentation.

---

## 🛠️ Tech Stack

* **Java**: 23
* **Framework**: Spring Boot 4.1.1
* **Security**: Spring Security + JWT (JJWT 0.12.6)
* **Database**: H2 in-memory (development)
* **ORM & Persistence**: Spring Data JPA / Hibernate
* **Validation**: Spring Validation (Jakarta Bean Validation)
* **Documentation**: SpringDoc OpenAPI 3 (Swagger UI)
* **Build Tool**: Maven

---

## ✨ Key Features

* **Authentication & Authorization**: Register and log in to receive a JWT; access is controlled by roles (`ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_USER`).
* **User Administration**: Admins can list, view, delete users and change their roles.
* **Category Management**: Full CRUD with a guard that blocks deleting a category that still has products assigned (`409 CONFLICT`).
* **Product Management**: Full CRUD, restocking endpoint, and low-stock queries.
* **Dynamic Filtering**: Filter products by name, SKU, price range, category, and stock range using JPA Specifications.
* **Pagination & Sorting**: Paginated responses via `Pageable`, integrated with Swagger through `@ParameterObject`.
* **N+1 Prevention**: `@EntityGraph` on product queries.
* **Optimistic Locking**: `@Version` field on `Product` prevents concurrent stock overwrites.
* **Transactional Correctness**: Mutating service methods are `@Transactional`; reads use `@Transactional(readOnly = true)`.
* **Global Exception Handling**: Unified `ErrorResponseDto` (timestamp, status, error, message, path) covering validation errors, missing resources, duplicates, access denied, bad credentials, data integrity violations, and generic errors.
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
| `security` | Security filter chain, JWT provider and filter, custom user details |
| `specification` | `ProductSpecification` (dynamic filtering) |
| `exception` | `GlobalExceptionHandler`, custom exceptions, `ErrorResponseDto` |
| `config` | `DataSeeder`, `OpenApiConfig` |

---

## 🚀 Getting Started

### Prerequisites

* **JDK 23**
* **Maven 3.8+**

### Environment Variables

The application reads its secrets from system environment variables. Set all of these before running:

| Variable | Description |
| --- | --- |
| `JWT_SECRET` | Secret key used to sign JWTs |
| `ADMIN_USERNAME` | Seeded admin username |
| `ADMIN_EMAIL` | Seeded admin email |
| `ADMIN_PASSWORD` | Seeded admin password |
| `MANAGER_USERNAME` | Seeded manager username |
| `MANAGER_EMAIL` | Seeded manager email |
| `MANAGER_PASSWORD` | Seeded manager password |

### Installation & Execution

1. **Clone the repository:**
```bash
git clone https://github.com/RazvanAndrei2005/inventory-manager.git
cd inventory-manager
```

2. **Set the environment variables** (example for Linux/macOS):
```bash
export JWT_SECRET=your-long-random-secret
export ADMIN_USERNAME=admin
export ADMIN_EMAIL=admin@example.com
export ADMIN_PASSWORD=change-me
export MANAGER_USERNAME=manager
export MANAGER_EMAIL=manager@example.com
export MANAGER_PASSWORD=change-me
```

3. **Build the application:**
```bash
mvn clean package
```

4. **Run the application:**
```bash
mvn spring-boot:run
```

The API runs on port **8081**.

5. **Useful URLs:**

| Tool | URL |
| --- | --- |
| Swagger UI | `http://localhost:8081/swagger-ui.html` |
| H2 Console (dev only) | `http://localhost:8081/h2-console` |

---

## 🔐 Authentication

All routes require a valid JWT except `/api/auth/**`, Swagger UI, and the H2 console.

1. Register via `POST /api/auth/register` (new users receive `ROLE_USER`), or log in with a seeded account.
2. Call `POST /api/auth/login` to receive a token.
3. Send it with each request:
```
Authorization: Bearer <token>
```

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
| `PATCH` | `/api/products/{id}/restock` | Add stock quantity to a product |
| `DELETE` | `/api/products/{id}` | Delete a product (ADMIN only) |

**Query parameters for `GET /api/products`:** `name`, `sku`, `minPrice`, `maxPrice`, `categoryId`, `minStock`, `maxStock`, plus standard `page`, `size`, and `sort`.

---

## ⚠️ Error Format

All errors return a consistent structure:

```json
{
  "timestamp": "2026-11-01T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: 42",
  "path": "/api/products/42"
}
```

---

