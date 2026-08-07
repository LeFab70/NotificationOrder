
CREATE TABLE orders (
    id          VARCHAR(36)  NOT NULL,
    status      VARCHAR(30)  NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id)
);

-- Journal d'evolution : une ligne par changement de statut, horodatee.
CREATE TABLE order_status_history (
    id         VARCHAR(36)  NOT NULL,
    order_id   VARCHAR(36)  NOT NULL,
    status     VARCHAR(30)  NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    CONSTRAINT pk_order_status_history PRIMARY KEY (id),
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX idx_order_status_history_order_id ON order_status_history (order_id);
