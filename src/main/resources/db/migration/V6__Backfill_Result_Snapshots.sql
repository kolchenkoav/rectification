-- Backfill immutable result snapshots for history rows created while V2 columns were unused.
-- Values mirror the formulas that existed before snapshots became the history source of truth.
UPDATE rectification_history
SET absolute_alcohol = FLOOR(((alcohol_strength / 100.0) * amount_of_raw_alcohol * 1000.0) + 0.5)
WHERE absolute_alcohol IS NULL;


UPDATE rectification_history
SET heads = FLOOR(absolute_alcohol * 0.03)
WHERE heads IS NULL;

UPDATE rectification_history
SET commercial_alcohol = FLOOR(absolute_alcohol * 0.65)
WHERE commercial_alcohol IS NULL;

UPDATE rectification_history
SET tails = FLOOR(absolute_alcohol * 0.035)
WHERE tails IS NULL;

ALTER TABLE rectification_history
    ALTER COLUMN absolute_alcohol SET NOT NULL;

ALTER TABLE rectification_history
    ALTER COLUMN heads SET NOT NULL;

ALTER TABLE rectification_history
    ALTER COLUMN commercial_alcohol SET NOT NULL;

ALTER TABLE rectification_history
    ALTER COLUMN tails SET NOT NULL;
