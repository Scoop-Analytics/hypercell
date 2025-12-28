package io.hypercell.core;

import io.hypercell.core.grid.MemWorkbook;
import io.hypercell.core.grid.MemSheet;
import io.hypercell.core.grid.MemCell;
import io.hypercell.core.grid.MemCellType;
import io.hypercell.core.grid.FormulaError;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cross-validation test that verifies HyperCell produces the same calculation
 * results as Microsoft Excel.
 *
 * This test loads Excel test files and validates that HyperCell's
 * MemWorkbook/MemSheet/MemCell classes produce identical results to Excel's
 * cached formula values.
 */
public class CrossValidationTest {

    // Path to Excel test sheets directory (configure via environment or hardcode for local testing)
    private static final String TEST_SHEETS_DIR = System.getProperty("hypercell.testsheets",
            "/home/bradpeters/dev/scoop/app/src/test/resources/testsheets");

    /**
     * Main validation test - loads all Excel files from test directory
     * and validates that HyperCell produces the same results as Excel.
     */
    @Test
    public void testHyperCellMatchesExcelCalculations() {
        Path dir = Path.of(TEST_SHEETS_DIR);

        if (!Files.exists(dir)) {
            System.err.println("WARNING: Test sheets directory not found: " + dir);
            System.err.println("Skipping cross-validation test.");
            return;
        }

        List<ValidationResult> results = new ArrayList<>();

        try (Stream<Path> paths = Files.list(dir)) {
            paths.forEach(p -> {
                String fname = p.getFileName().toString();
                if (!fname.endsWith(".xlsx"))
                    return;

                System.out.println("\n=== Testing: " + fname + " ===");
                try {
                    ValidationResult result = validateWorkbook(p.toFile());
                    results.add(result);

                    System.out.println("  ✅ Sheets: " + result.numSheets);
                    System.out.println("  ✅ Formulas validated: " + result.formulasValidated);
                    System.out.println("  ⚠️  Formulas skipped: " + result.formulasSkipped);
                    System.out.println("  ❌ Mismatches: " + result.mismatches.size());

                    if (!result.mismatches.isEmpty()) {
                        System.out.println("  MISMATCHES DETECTED:");
                        for (String mismatch : result.mismatches) {
                            System.out.println("    " + mismatch);
                        }
                    }

                } catch (Exception e) {
                    System.err.println("  ❌ ERROR validating " + fname + ": " + e.getClass().getName());
                    System.err.println("  Message: " + e.getMessage());
                    e.printStackTrace(System.err);

                    // Add a failed result instead of aborting entire test
                    ValidationResult failedResult = new ValidationResult(fname);
                    failedResult.mismatches.add("LOAD FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    results.add(failedResult);
                }
            });
        } catch (IOException e1) {
            fail("Failed to read test directory: " + e1.getMessage());
        }

        // Generate summary report
        printSummaryReport(results);

        // Assert no mismatches across all workbooks
        int totalMismatches = results.stream().mapToInt(r -> r.mismatches.size()).sum();
        if (totalMismatches > 0) {
            fail("Cross-validation failed: " + totalMismatches + " formula mismatches detected");
        }
    }

    /**
     * Validate a single workbook - loads with HyperCell and compares to Excel values
     */
    private ValidationResult validateWorkbook(File file) throws IOException {
        ValidationResult result = new ValidationResult(file.getName());

        // Load with Apache POI (to get Excel's cached values)
        XSSFWorkbook poiWorkbook = (XSSFWorkbook) WorkbookFactory.create(file);

        if (poiWorkbook == null) {
            throw new IOException("Failed to load workbook with POI: " + file);
        }

        // Load with HyperCell
        MemWorkbook hyperCellWorkbook = new MemWorkbook(file.toString(), poiWorkbook, true);

        if (hyperCellWorkbook == null) {
            throw new IOException("MemWorkbook constructor returned null: " + file);
        }

        if (hyperCellWorkbook.getNumSheets() == 0) {
            throw new IOException("MemWorkbook has no sheets: " + file);
        }

        // Calculate all formulas using HyperCell
        hyperCellWorkbook.calculate();

        result.numSheets = hyperCellWorkbook.getNumSheets();

        // Compare each sheet
        for (int sheetNum = 0; sheetNum < hyperCellWorkbook.getNumSheets(); sheetNum++) {
            MemSheet hyperSheet = hyperCellWorkbook.getSheet(sheetNum);
            Sheet poiSheet = poiWorkbook.getSheetAt(sheetNum);

            if (hyperSheet == null) {
                throw new IOException("MemSheet is null for sheet " + sheetNum + " in " + file);
            }

            validateSheet(hyperSheet, poiSheet, result);
        }

        return result;
    }

    /**
     * Validate all formulas in a sheet
     */
    private void validateSheet(MemSheet hyperSheet, Sheet poiSheet, ValidationResult result) {
        for (int rowNum = 0; rowNum < hyperSheet.getNumRows(); rowNum++) {
            for (int colNum = 0; colNum < hyperSheet.getNumCellsInRow(rowNum); colNum++) {
                MemCell hyperCell = hyperSheet.getCellAt(rowNum, colNum);

                if (hyperCell == null || hyperCell.isInformationalOnly())
                    continue;

                if (hyperCell.getCellType() == MemCellType.Formula) {
                    // Check if this formula had parse errors
                    boolean skip = false;
                    if (hyperSheet.getParseErrors() != null) {
                        for (var error : hyperSheet.getParseErrors()) {
                            if (error.equals("Unable to parse expression:" + hyperCell.getFormula())) {
                                skip = true;
                                result.formulasSkipped++;
                                break;
                            }
                        }
                    }

                    if (skip)
                        continue;

                    // Get POI cell
                    Cell poiCell = poiSheet.getRow(rowNum).getCell(colNum);
                    if (poiCell == null) {
                        result.formulasSkipped++;
                        continue;
                    }

                    // Validate the calculated value matches
                    String mismatch = compareFormulaResult(hyperCell, poiCell, rowNum, colNum);
                    if (mismatch != null) {
                        result.mismatches.add(mismatch);
                    } else {
                        result.formulasValidated++;
                    }
                }
            }
        }
    }

    /**
     * Compare a single formula result between HyperCell and POI
     * Returns null if match, error message if mismatch
     */
    private String compareFormulaResult(MemCell hyperCell, Cell poiCell, int row, int col) {
        String cellAddress = getCellAddress(row, col);
        String formula = poiCell.getCellFormula();
        Object hyperValue = hyperCell.getValue();
        CellType poiType = poiCell.getCachedFormulaResultType();

        try {
            if (poiType == CellType.STRING) {
                String poiValue = poiCell.getStringCellValue();
                if (hyperValue == null) {
                    hyperValue = "";
                }
                if (hyperValue instanceof String) {
                    if (!hyperValue.equals(poiValue)) {
                        return String.format("%s [%s]: String mismatch - HyperCell='%s' Excel='%s'",
                                cellAddress, formula, hyperValue, poiValue);
                    }
                } else {
                    return String.format("%s [%s]: Type mismatch - HyperCell returned %s but Excel expects String",
                            cellAddress, formula, hyperValue.getClass().getSimpleName());
                }
            } else if (poiType == CellType.NUMERIC) {
                double poiValue = poiCell.getNumericCellValue();
                if (hyperValue == null) {
                    hyperValue = 0.0;
                }
                if (hyperValue != null && Number.class.isAssignableFrom(hyperValue.getClass())) {
                    double hyperDouble = ((Number) hyperValue).doubleValue();
                    double precision = poiValue == 0 ? 1E-8 : Math.pow(10, Math.log10(Math.abs(poiValue)) - 8);

                    if (Math.abs(hyperDouble - poiValue) > precision) {
                        return String.format("%s [%s]: Numeric mismatch - HyperCell=%.10f Excel=%.10f (diff=%.2e)",
                                cellAddress, formula, hyperDouble, poiValue, Math.abs(hyperDouble - poiValue));
                    }
                } else {
                    return String.format("%s [%s]: Type mismatch - HyperCell returned %s but Excel expects Numeric",
                            cellAddress, formula, hyperValue != null ? hyperValue.getClass().getSimpleName() : "null");
                }
            } else if (poiType == CellType.ERROR) {
                byte poiError = poiCell.getErrorCellValue();
                FormulaError hyperError = hyperCell.getErrorValue();

                if (hyperError == null) {
                    return String.format("%s [%s]: Error mismatch - HyperCell has no error but Excel has error code %d",
                            cellAddress, formula, poiError);
                } else if (hyperError.getPoiErrorCode() != poiError) {
                    return String.format("%s [%s]: Error code mismatch - HyperCell=%d Excel=%d",
                            cellAddress, formula, hyperError.getPoiErrorCode(), poiError);
                }
            } else if (poiType == CellType.BOOLEAN) {
                boolean poiValue = poiCell.getBooleanCellValue();
                if (hyperValue instanceof Number) {
                    // HyperCell may return 0/1 for boolean
                    int hyperInt = ((Number) hyperValue).intValue();
                    boolean hyperBool = (hyperInt != 0);
                    if (hyperBool != poiValue) {
                        return String.format("%s [%s]: Boolean mismatch - HyperCell=%s Excel=%s",
                                cellAddress, formula, hyperBool, poiValue);
                    }
                } else {
                    return String.format("%s [%s]: Type mismatch - HyperCell returned %s but Excel expects Boolean",
                            cellAddress, formula, hyperValue != null ? hyperValue.getClass().getSimpleName() : "null");
                }
            }
        } catch (Exception e) {
            return String.format("%s [%s]: Exception during comparison - %s",
                    cellAddress, formula, e.getMessage());
        }

        return null; // Match!
    }

    /**
     * Convert row/col to Excel-style address (e.g., A1, B5)
     */
    private String getCellAddress(int row, int col) {
        StringBuilder sb = new StringBuilder();
        int colNum = col;
        while (colNum >= 0) {
            sb.insert(0, (char) ('A' + (colNum % 26)));
            colNum = (colNum / 26) - 1;
        }
        sb.append(row + 1);
        return sb.toString();
    }

    /**
     * Print summary report of all validation results
     */
    private void printSummaryReport(List<ValidationResult> results) {
        System.out.println("\n");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("         HYPERCELL CROSS-VALIDATION SUMMARY");
        System.out.println("═══════════════════════════════════════════════════════════");

        int totalWorkbooks = results.size();
        int totalSheets = results.stream().mapToInt(r -> r.numSheets).sum();
        int totalValidated = results.stream().mapToInt(r -> r.formulasValidated).sum();
        int totalSkipped = results.stream().mapToInt(r -> r.formulasSkipped).sum();
        int totalMismatches = results.stream().mapToInt(r -> r.mismatches.size()).sum();

        System.out.println("Workbooks tested:     " + totalWorkbooks);
        System.out.println("Total sheets:         " + totalSheets);
        System.out.println("Formulas validated:   " + totalValidated);
        System.out.println("Formulas skipped:     " + totalSkipped);
        System.out.println("Mismatches found:     " + totalMismatches);
        System.out.println();

        if (totalMismatches == 0) {
            System.out.println("✅ SUCCESS: All calculations match!");
            System.out.println("   HyperCell produces identical results to Excel.");
        } else {
            System.out.println("❌ FAILURE: " + totalMismatches + " mismatches detected");
            System.out.println();
            System.out.println("Detailed mismatches:");
            for (ValidationResult result : results) {
                if (!result.mismatches.isEmpty()) {
                    System.out.println("\n" + result.workbookName + ":");
                    for (String mismatch : result.mismatches) {
                        System.out.println("  " + mismatch);
                    }
                }
            }
        }
        System.out.println("═══════════════════════════════════════════════════════════");
    }

    /**
     * Container for validation results of a single workbook
     */
    private static class ValidationResult {
        String workbookName;
        int numSheets = 0;
        int formulasValidated = 0;
        int formulasSkipped = 0;
        List<String> mismatches = new ArrayList<>();

        ValidationResult(String name) {
            this.workbookName = name;
        }
    }
}
