CREATE TABLE detail (
    id BIGSERIAL PRIMARY KEY,
    history_id BIGINT REFERENCES rectification_history(id) ON DELETE CASCADE,
    temperature_cube DOUBLE PRECISION,
    temperature_tsar DOUBLE PRECISION,
    temperature_atmosphere DOUBLE PRECISION,
    temperature_water DOUBLE PRECISION,
    record_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);