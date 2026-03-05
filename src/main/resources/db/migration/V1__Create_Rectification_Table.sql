CREATE TABLE rectification_history (
    id BIGSERIAL PRIMARY KEY,
    amount_of_raw_alcohol INT NOT NULL,
    alcohol_strength DOUBLE PRECISION NOT NULL,
    power DOUBLE PRECISION NOT NULL,
    water INT NOT NULL,
    calculation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);