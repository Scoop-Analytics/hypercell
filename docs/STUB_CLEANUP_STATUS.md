# Stub Cleanup Status
*Tracking the removal of the `scoop.*` package from `hypercell-core`*

> **Status: COMPLETE** - All stub cleanup tasks have been finished. The `oss/` directory is clean.

## Completed Tasks

### 1. ✅ Refactor `MathFunction.java`
**Status:** Complete - Moved to `oss/hypercell-core/src/main/java/io/hypercell/core/expression/MathFunction.java`
- Removed legacy `populateIfNecessary` method
- Uses `EvaluationContext` for data source refresh

### 2. ✅ Update `CompileContext`
**Status:** Complete - Updated to accept `EvaluationContext`
- Added `EvaluationContext` field and getter
- Constructor updated

### 3. ✅ The Purge
**Status:** Complete - All scoop references removed
- Deleted root-level zombie directories (470 files, 35,291 lines)
- `oss/` directory has zero Scoop references
- Verified via `grep -r "scoop\|Scoop" oss/*/src/main --include="*.java"` → 0 matches

### 4. ✅ Grammar Cleanup
**Status:** Complete
- `HyperCellExpression.g4` is clean in `oss/hypercell-formula`
- No SCOOP-specific tokens or rules
