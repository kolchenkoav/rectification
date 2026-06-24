# Refactoring Plan — Rectification Calculator

## Phase 1: Safe Mechanical Fixes (Zero behavioral change)

**Goal**: Fix typos, dead code, redundant saves, and config issues. Each is a trivial find-and-replace.

### 1a. Fix the `headFactions` → `headFractions` typo (Issue #4)
- **Files**: `OutData.java`, `RectificationHistory.java:101,104`, all 4 templates (`OutData.html:138,142`, `Print.html:99,103`), `RectificationServiceImpl.java:37`, `RectificationControllerTest.java:234,567`
- Rename the field and its getter/setter (Lombok generates them from field name).
- In templates: change `outData.headFactions` → `outData.headFractions`.
- In tests: change `setHeadFactions` → `setHeadFractions`, `getHeadFactions` → `getHeadFractions`.
- **Verify**: `mvn test` — all 55+ tests pass, JaCoCo ≥ 80%.

### 1b. Remove dead code (Issue #10)
- **Files**: `RectificationServiceImpl.java`, `RectificationService.java`, `RectificationServiceImplTest.java`
- Delete `resultToStringForHtml()` from service impl and interface.
- Remove the test `resultToStringForHtml_shouldReturnList` (line 139-150 of test).
- In `RectificationController.java:221`: remove `List<String> value = service.resultToStringForHtml(...)` and `model.addAttribute("result", value)`.
- Rename `ContactListApplicationTests` → `RectificationApplicationTests` (fix wrong project name).
- **Verify**: `mvn test`.

### 1c. Remove redundant `historyRepository.save()` in @Transactional methods (Issue #14)
- **Files**: `RectificationServiceImpl.java:135,157`
- In `addDetail()` (line 135): remove `historyRepository.save(history)` — dirty checking handles it.
- In `saveActualData()` (line 157): remove `historyRepository.save(history)` — dirty checking handles it.
- Update corresponding tests: remove `verify(historyRepository).save(history)` assertions at lines 195, 220, 236.
- **Verify**: `mvn test`.

### 1d. Fix application.yml config (Issues #12, #8)
- **Files**: `application.yml`, `RectificationController.java`
- Change `show-sql: true` → `show-sql: ${SHOW_SQL:false}` or remove and set only in test profile.
- Change `logging.level.org.hibernate.SQL: DEBUG` → `INFO` (or use `${HIBERNATE_SQL_LOG_LEVEL:INFO}`).
- Change `logging.level.org.hibernate.type.descriptor.sql.BasicBinder: TRACE` → `INFO`.
- Replace `Environment` injection with `@Value` for `app.version` and `app.tag` (remove `Environment` field entirely).
- **Verify**: `mvn test`, manual startup check that logging is quieter.

### 1e. Fix port mismatch (Issue #20)
- **Files**: `README.md`, `Dockerfile`, `docker-compose.yml` — align to 8099 or 8089.
- **Verify**: grep for port references, ensure consistent.

---

## Phase 2: Extract RectificationCalculator (High value, medium risk)

**Goal**: Separate pure fraction math from persistence. This is the single highest-value refactoring.

### 2a. Create `RectificationCalculator` class
- **New file**: `src/main/java/com/example/rectificat/services/RectificationCalculator.java`
- Move all fraction constants from `RectificationServiceImpl` (lines 24-29) into this class.
- Move the `calc(InData) → OutData` logic (lines 32-44) into this class as a public method.
- This class has zero Spring dependencies — pure Java, trivially testable.

### 2b. Create `FractionConstants` or just put constants on the calculator
- Remove the duplicate constants from `RectificationHistory.java` (lines 23-24: `HEAD_FRACTION`, `HEADS_AND_COMMERCIAL_FRACTION`). These are only used in `toOutData()` — move the math into the calculator or keep constants only in one place.
- `RectificationHistory.toOutData()` uses `HEAD_FRACTION` and `HEADS_AND_COMMERCIAL_FRACTION` to recompute `headFactions` and `headsAndCommercialAlcohol` from the stored `absoluteAlcohol`. Instead, store these values in the snapshot columns (V2 migration already has `heads` column but not `headFactions`/`headsAndCommercialAlcohol`). Two options:
  - **Option A (simpler)**: Add two columns to store `headFractions` and `headsAndCommercialAlcohol` in the snapshot, set via `setResultSnapshot()`. Then `toOutData()` just reads stored values.
  - **Option B**: Keep recomputing in `toOutData()` but move the constants to a shared location (e.g., `RectificationCalculator` exposes them or a `FractionConstants` enum).
  - **Recommended: Option A** — eliminates recomputation drift and removes the need for `RectificationHistory` to know fraction math. Requires a new Flyway migration (`V7__Add_Fraction_Snapshot_Columns.sql`).

### 2c. Update `RectificationServiceImpl`
- Inject `RectificationCalculator` (or just call it as a static utility if no state).
- `calc()` delegates to calculator.
- `saveCalculation()` uses calculator.

### 2d. Write `RectificationCalculatorTest`
- Pure unit tests for fraction math (no mocks needed).
- Cover: normal input, zero values, boundary values.

### 2e. Update existing tests
- `RectificationServiceImplTest.calc_*` tests can stay (they test the service delegates correctly) or move to calculator tests.
- `RectificationControllerTest` — no changes needed (it mocks the service).

**Verify**: `mvn test` — all tests pass including new calculator tests.

---

## Phase 3: Fix Entity/Type Issues (Medium risk)

**Goal**: Fix the @EqualsAndHashCode risk and type inconsistencies.

### 3a. Fix `@Data` on JPA entities (Issue #6)
- **Files**: `RectificationHistory.java`, `Detail.java`
- Replace `@Data` with `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`.
- Add explicit `equals()` and `hashCode()` using `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` with only `@Id` field included. Or just omit equals/hashCode (JPA entities shouldn't rely on object identity via Lombok-generated methods).
- **Verify**: `mvn test`.

### 3b. Fix type inconsistencies (Issue #5)
- **Files**: `InData.java`, `RectificationHistory.java`
- `InData.water`: change `Integer` → `int` (with `@NotNull` becoming unnecessary, use `@Min(0)`). Or keep `Integer` for null-safety and map to `int` in the converter. Current behavior: `@NotNull` on `Integer` ensures it's never null, so `int` is fine.
- `RectificationHistory.commercialAlcohol`: change `int` → `double` for consistency with other fraction fields. **This requires a Flyway migration** if the DB column is integer. Check V2 migration.
- **Verify**: `mvn test` + manual check that DB migration works.

### 3c. Remove unused `DetailRepository.findByHistoryIdOrderByRecordTimeDesc` (Issue #13)
- **File**: `DetailRepository.java:11`
- Delete the method.
- **Verify**: `mvn test`.

---

## Phase 4: Controller Improvements (Low-medium risk)

**Goal**: Clean up error handling and controller structure.

### 4a. Replace broad `catch (Exception e)` with specific exception (Issue #7)
- **File**: `RectificationController.java:215`
- Catch `DataAccessException` (or `RuntimeException`) instead of `Exception`.
- **Verify**: `mvn test`.

### 4b. Remove `resultToString` from controller (it's never called from controller)
- **File**: `RectificationServiceImpl.java` — the `resultToString()` method is defined in the interface and implemented but never called from the controller. Check if it's called from templates or anywhere else. If not, remove it from the interface and impl.
- Actually, looking at the code, `resultToString()` is defined in the service interface but never called from the controller. The controller calls `resultToStringForHtml()` (which we're deleting in Phase 1). After Phase 1, `resultToString()` is orphaned too. Delete it.
- **Verify**: `mvn test`.

### 4c. Clean up `RectificationHistoryRepository` — remove overbroad `@EntityGraph` (Issue #15)
- **File**: `RectificationHistoryRepository.java:15-16`
- Remove `@EntityGraph(attributePaths = {"details"})` from `findById()`. Callers that need details should use `fetch` joins or a separate query. The `getHistoryWithDetails()` method can have its own `@EntityGraph`.
- Add a new method: `@EntityGraph(attributePaths = {"details"}) Optional<RectificationHistory> findByIdWithDetails(Long id)`.
- Update `RectificationServiceImpl.getHistoryWithDetails()` to use the new method.
- Default `findById()` becomes lazy (no detail loading).
- **Verify**: `mvn test`.

---

## Phase 5: Cleanup and Polish (Low risk)

### 5a. Rename package (Issue #16) — **DEFER / SKIP**
- Renaming `com.example.rectificat` → `com.example.rectification` affects every file, every test, every config, Flyway references, etc. For a small project with 11 files this is doable but high merge-conflict risk and low value. **Recommend skipping** unless there's a strong reason.

### 5b. Clean up test class name (done in Phase 1)

### 5c. Add `@Transactional` test annotation to service tests
- The service tests use mocks so this is already fine. No integration tests to add right now.

### 5d. Document port in README
- Ensure README.md matches actual configured port.

---

## Execution Order Summary

| Phase | Risk | Files changed | Tests impact |
|-------|------|---------------|--------------|
| 1a-1e | Very low | 6-8 files, trivial edits | All pass, minor test edits |
| 2a-2e | Medium | 5 files + 1 new + 1 migration | New tests, existing tests updated |
| 3a-3c | Medium | 3 files + possibly 1 migration | Tests updated for type changes |
| 4a-4c | Low | 3 files | Minimal test changes |
| 5 | Low | 1-2 files | No test changes |

**Critical constraint**: After each phase, run `mvn test` and verify JaCoCo passes at 80%.
