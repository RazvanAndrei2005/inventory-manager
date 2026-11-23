-- Stock movements: an audit log of every change to a product's stock

CREATE TABLE stock_movements (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    quantity_change INT NOT NULL,
    quantity_after INT NOT NULL,
    reason VARCHAR(255),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_stock_movement_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_movement_user FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_stock_movement_change_not_zero CHECK (quantity_change <> 0),
    CONSTRAINT chk_stock_movement_after_non_negative CHECK (quantity_after >= 0)
);

CREATE INDEX idx_stock_movements_product_created ON stock_movements(product_id, created_at DESC);
CREATE INDEX idx_stock_movements_created ON stock_movements(created_at DESC);

-- Backfill: one INITIAL_STOCK movement for every product that already has stock
INSERT INTO stock_movements (product_id, type, quantity_change, quantity_after, reason, created_by)
SELECT p.id, 'INITIAL_STOCK', p.stock_quantity, p.stock_quantity,
       'Backfilled when stock movements were introduced',
       (SELECT id FROM users WHERE role = 'ROLE_ADMIN' ORDER BY id LIMIT 1)
FROM products p
WHERE p.stock_quantity > 0;
