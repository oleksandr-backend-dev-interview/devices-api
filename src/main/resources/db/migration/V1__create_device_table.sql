CREATE TABLE device (
    id uuid PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    brand VARCHAR(255) NOT NULL,
    state VARCHAR(20) CHECK (state IN ('AVAILABLE', 'IN_USE', 'INACTIVE')) NOT NULL,
    creation_time TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_device_brand_lower ON device (LOWER(brand));
CREATE INDEX idx_state ON device (state);