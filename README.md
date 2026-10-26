# 📦 Inventory Manager REST API
![Status](https://img.shields.io/badge/Status-In%20Progress-yellow?style=for-the-badge&logo=github)

A robust, enterprise-grade **Spring Boot 4 RESTful API** designed for modern inventory and category management. Built with clean architecture principles, DTO pattern separation, relational JPA mapping, advanced pagination, and OpenAPI/Swagger documentation.

---

## 🛠️ Tech Stack

* **Java**: 25
* **Framework**: Spring Boot 4.x
* **Database**: PostgreSQL / H2 Database
* **ORM & Persistence**: Spring Data JPA / Hibernate
* **Documentation**: Springdoc OpenAPI (Swagger UI)
* **Build Tool**: Maven

---

## ✨ Key Features

* **Category Management**: Full CRUD operations for product categories.
* **Product Management**: Complete inventory control with stock level updates and restocking endpoints.
* **JPA Entity Mapping**: Efficient `@ManyToOne` / `@OneToMany` relationships between `Product` and `Category`.
* **Advanced Pagination & Sorting**: High-performance querying using `Pageable` and seamless Swagger integration with `@ParameterObject`.
* **Category Filtering**: Filter products by category ID with paginated responses.
* **Low-Stock Alert**: Dedicated query to identify products needing replenishment.
* **Global Exception Handling**: Unified error format (`ErrorResponseDto`) handling missing resources, duplicate SKUs, and validation errors.

---

## 🚀 Getting Started

### Prerequisites

* **JDK 17** or higher
* **Maven 3.8+**

### Installation & Execution

1. **Clone the repository:**
```bash
git clone https://github.com/RazvanAndrei2005/inventory-manager.git
cd inventory-manager

```


2. **Build the application:**
```bash
mvn clean package

```


3. **Run the application:**
```bash
mvn spring-boot:run

```


4. **Interactive API Documentation:**
   Access Swagger UI at:
```text
http://localhost:8080/swagger-ui/index.html

```



---

## 📑 API Endpoints Reference

### Categories (`/api/categories`)

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/categories` | Create a new category |
| `GET` | `/api/categories` | Retrieve all categories |
| `GET` | `/api/categories/{id}` | Get category details by ID |

### Products (`/api/products`)

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/products` | Create a product with associated category |
| `GET` | `/api/products` | Get all products (paginated & sorted) |
| `GET` | `/api/products/{id}` | Get product details by ID |
| `GET` | `/api/products/search` | Search products by name (paginated) |
| `GET` | `/api/products/filter` | Filter products by price range (paginated) |
| `GET` | `/api/products/category/{categoryId}` | Get products by category (paginated) |
| `GET` | `/api/products/low-stock` | Retrieve products below stock threshold |
| `PUT` | `/api/products/{id}` | Update product details |
| `PATCH` | `/api/products/{id}/restock` | Restock product quantity |
| `DELETE` | `/api/products/{id}` | Delete product |