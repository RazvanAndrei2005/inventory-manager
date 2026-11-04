-- Schema inițială pentru utilizatori, categorii și produse

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL
);

CREATE TABLE categories (
                            id BIGSERIAL PRIMARY KEY,
                            name VARCHAR(100) NOT NULL UNIQUE,
                            description VARCHAR(255)
);

CREATE TABLE products (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(100) NOT NULL,
                          sku VARCHAR(50) NOT NULL UNIQUE,
                          price DOUBLE PRECISION NOT NULL,
                          stock_quantity INT NOT NULL,
                          category_id BIGINT NOT NULL,
                          version BIGINT NOT NULL DEFAULT 0,
                          CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id)
);