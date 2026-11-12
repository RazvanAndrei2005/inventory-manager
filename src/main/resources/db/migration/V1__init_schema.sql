
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL,
                       enabled BOOLEAN NOT NULL DEFAULT TRUE
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
                          price NUMERIC(12,2) NOT NULL,
                          stock_quantity INT NOT NULL,
                          category_id BIGINT NOT NULL,
                          version BIGINT NOT NULL DEFAULT 0,
                          CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id),
                          CONSTRAINT chk_product_price_positive CHECK (price > 0),
                          CONSTRAINT chk_product_stock_non_negative CHECK (stock_quantity >= 0)
);

CREATE INDEX idx_products_category_id ON products(category_id);