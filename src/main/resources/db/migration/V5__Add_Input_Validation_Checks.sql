-- Cleanup before adding validation constraints.
-- Core calculation inputs cannot be safely reconstructed, so invalid history rows are deleted.
-- The detail foreign key uses ON DELETE CASCADE, so related measurements are removed with them.
DELETE FROM rectification_history
WHERE NOT (amount_of_raw_alcohol BETWEEN 1 AND 1000
    AND alcohol_strength >= 0.1 AND alcohol_strength <= 100
    AND power >= 0.1 AND power <= 100
    AND water >= 0 AND water <= 10000);

-- Optional actual values are user-entered post-fact measurements; invalid legacy values are nulled
-- instead of deleting the whole calculation row.
UPDATE rectification_history
SET actual_commercial_alcohol = NULL
WHERE actual_commercial_alcohol IS NOT NULL
  AND NOT (actual_commercial_alcohol > 0 AND actual_commercial_alcohol <= 100000);

UPDATE rectification_history
SET actual_heads = NULL
WHERE actual_heads IS NOT NULL
  AND NOT (actual_heads >= 0 AND actual_heads <= 100000);

UPDATE rectification_history
SET actual_tails = NULL
WHERE actual_tails IS NOT NULL
  AND NOT (actual_tails >= 0 AND actual_tails <= 100000);

-- Temperature columns are nullable; preserve valid parts of legacy detail rows and null only
-- individual invalid measurements before enforcing the range.
UPDATE detail
SET temperature_cube = NULL
WHERE temperature_cube IS NOT NULL
  AND NOT (temperature_cube >= -50 AND temperature_cube <= 150);

UPDATE detail
SET temperature_tsar = NULL
WHERE temperature_tsar IS NOT NULL
  AND NOT (temperature_tsar >= -50 AND temperature_tsar <= 150);

UPDATE detail
SET temperature_atmosphere = NULL
WHERE temperature_atmosphere IS NOT NULL
  AND NOT (temperature_atmosphere >= -50 AND temperature_atmosphere <= 150);

UPDATE detail
SET temperature_water = NULL
WHERE temperature_water IS NOT NULL
  AND NOT (temperature_water >= -50 AND temperature_water <= 150);

ALTER TABLE rectification_history
    ADD CONSTRAINT chk_rectification_amount_of_raw_alcohol
        CHECK (amount_of_raw_alcohol BETWEEN 1 AND 1000);

ALTER TABLE rectification_history
    ADD CONSTRAINT chk_rectification_alcohol_strength
        CHECK (alcohol_strength >= 0.1 AND alcohol_strength <= 100);

ALTER TABLE rectification_history
    ADD CONSTRAINT chk_rectification_power
        CHECK (power >= 0.1 AND power <= 100);

ALTER TABLE rectification_history
    ADD CONSTRAINT chk_rectification_water
        CHECK (water >= 0 AND water <= 10000);

ALTER TABLE rectification_history
    ADD CONSTRAINT chk_rectification_actual_values
        CHECK ((actual_commercial_alcohol IS NULL OR (actual_commercial_alcohol > 0 AND actual_commercial_alcohol <= 100000))
            AND (actual_heads IS NULL OR (actual_heads >= 0 AND actual_heads <= 100000))
            AND (actual_tails IS NULL OR (actual_tails >= 0 AND actual_tails <= 100000)));

ALTER TABLE detail
    ADD CONSTRAINT chk_detail_temperatures
        CHECK ((temperature_cube IS NULL OR (temperature_cube >= -50 AND temperature_cube <= 150))
            AND (temperature_tsar IS NULL OR (temperature_tsar >= -50 AND temperature_tsar <= 150))
            AND (temperature_atmosphere IS NULL OR (temperature_atmosphere >= -50 AND temperature_atmosphere <= 150))
            AND (temperature_water IS NULL OR (temperature_water >= -50 AND temperature_water <= 150)));
