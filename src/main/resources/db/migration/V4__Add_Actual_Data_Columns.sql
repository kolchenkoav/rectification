-- Добавляем колонки для фактических показателей
ALTER TABLE rectification_history ADD COLUMN actual_commercial_alcohol DOUBLE PRECISION;
ALTER TABLE rectification_history ADD COLUMN actual_heads DOUBLE PRECISION;
ALTER TABLE rectification_history ADD COLUMN actual_tails DOUBLE PRECISION;
