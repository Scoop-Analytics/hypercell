/**
 * Integration tests for Scoop bridge.
 */
package com.scoopanalytics.hypercell.bridge.scoop;

import com.scoopanalytics.hypercell.api.DataSource;
import com.scoopanalytics.hypercell.bridge.ExtendedWorkbook;
import com.scoopanalytics.hypercell.core.grid.MemCell;
import com.scoopanalytics.hypercell.core.grid.MemSheet;
import com.scoopanalytics.hypercell.core.grid.MemWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the Scoop integration layer.
 */
class ScoopIntegrationTest
{
    private MemWorkbook workbook;
    private MemSheet sheet;
    private List<String> auditLog;
    private int queryRefreshCount;

    @BeforeEach
    void setUp()
    {
        workbook = new MemWorkbook();
        workbook.setName("TestWorkbook");
        sheet = workbook.createSheet("Data");
        auditLog = new ArrayList<>();
        queryRefreshCount = 0;
    }

    @Test
    void testBasicIntegration()
    {
        // Set up test data using setCellAt pattern
        sheet.setCellAt(0, 0, new MemCell(10.0));  // A1 = 10
        sheet.setCellAt(0, 1, new MemCell(20.0));  // B1 = 20

        // Create integration with callbacks
        ScoopCallbacks callbacks = ScoopCallbacks.builder()
                .userId(() -> "user123")
                .tenantId(() -> "tenant456")
                .auditLogger((event, details) -> auditLog.add(event + ": " + details))
                .build();

        ScoopIntegration integration = new ScoopIntegration(callbacks);
        ExtendedWorkbook extWorkbook = integration.createWorkbook(workbook);

        // Verify we can get cell values
        Object a1 = extWorkbook.getCellValue("Data", 0, 0);
        Object b1 = extWorkbook.getCellValue("Data", 0, 1);
        assertNotNull(a1);
        assertNotNull(b1);
        assertEquals(10.0, ((Number) a1).doubleValue(), 0.001);
        assertEquals(20.0, ((Number) b1).doubleValue(), 0.001);

        // Calculate (even without formulas, should work)
        extWorkbook.calculate();

        // Verify context
        assertEquals("user123", integration.getContext().getUserId());
        assertEquals("tenant456", integration.getContext().getTenantId());

        // Verify audit logging was called
        assertTrue(auditLog.stream().anyMatch(log -> log.contains("CALCULATE")));
    }

    @Test
    void testQueryRefresh()
    {
        // Mark sheet as a query sheet
        sheet.setQuerySheet(true);
        workbook.setRefreshQueryDataOnUse(true);

        // Set up test data that will be "refreshed"
        sheet.setCellAt(0, 0, new MemCell(100.0));

        // Create integration with query refresh callback
        ScoopCallbacks callbacks = ScoopCallbacks.builder()
                .queryRefresher((wb, sh) -> {
                    queryRefreshCount++;
                    // Simulate data refresh by updating values
                    sh.setCellAt(0, 0, new MemCell(200.0));
                    return true;
                })
                .build();

        ScoopIntegration integration = new ScoopIntegration(callbacks);
        ExtendedWorkbook extWorkbook = integration.createWorkbook(workbook);
        extWorkbook.setAutoRefreshQueries(true);

        // Calculate (should trigger query refresh)
        extWorkbook.calculate();

        // Verify refresh was called
        assertEquals(1, queryRefreshCount);

        // Verify data was updated by refresh
        Object result = extWorkbook.getCellValue("Data", 0, 0);
        assertEquals(200.0, ((Number) result).doubleValue(), 0.001);
    }

    @Test
    void testPermissionChecking()
    {
        ScoopCallbacks callbacks = ScoopCallbacks.builder()
                .permissionChecker(perm -> perm.equals("read") || perm.equals("write"))
                .build();

        ScoopIntegration integration = new ScoopIntegration(callbacks);

        assertTrue(integration.getContext().hasPermission("read"));
        assertTrue(integration.getContext().hasPermission("write"));
        assertFalse(integration.getContext().hasPermission("admin"));
    }

    @Test
    void testDataSources()
    {
        List<DataSource> testSources = new ArrayList<>();
        testSources.add(new DataSource("SalesData"));
        testSources.add(new DataSource("Inventory"));

        ScoopCallbacks callbacks = ScoopCallbacks.builder()
                .dataSources(() -> testSources)
                .build();

        ScoopIntegration integration = new ScoopIntegration(callbacks);
        List<DataSource> sources = integration.getContext().getDataSources();

        assertEquals(2, sources.size());
        assertEquals("SalesData", sources.get(0).sheetName());
        assertEquals("Inventory", sources.get(1).sheetName());
    }

    @Test
    void testReferenceResolver()
    {
        ScoopCallbacks callbacks = ScoopCallbacks.builder()
                .referenceResolver((sheetName, row, col) -> {
                    if ("External".equals(sheetName) && row == 0 && col == 0)
                    {
                        return 42.0;
                    }
                    return null;
                })
                .build();

        ScoopIntegration integration = new ScoopIntegration(callbacks);

        Object result = integration.getContext().resolveReference("External", 0, 0);
        assertEquals(42.0, result);

        Object noResult = integration.getContext().resolveReference("Other", 0, 0);
        assertNull(noResult);
    }

    @Test
    void testSetCellValue()
    {
        sheet.setCellAt(0, 0, new MemCell(10.0));
        sheet.setCellAt(0, 1, new MemCell(20.0));

        ScoopCallbacks callbacks = ScoopCallbacks.builder().build();
        ScoopIntegration integration = new ScoopIntegration(callbacks);
        ExtendedWorkbook extWorkbook = integration.createWorkbook(workbook);

        // Verify initial values
        assertEquals(10.0, ((Number) extWorkbook.getCellValue("Data", 0, 0)).doubleValue(), 0.001);
        assertEquals(20.0, ((Number) extWorkbook.getCellValue("Data", 0, 1)).doubleValue(), 0.001);

        // Update value using setCellValue
        extWorkbook.setCellValue("Data", 0, 0, 50, false);

        // Verify value was updated
        assertEquals(50.0, ((Number) extWorkbook.getCellValue("Data", 0, 0)).doubleValue(), 0.001);

        // Test setting a string value
        extWorkbook.setCellValue("Data", 1, 0, "Hello", false);
        assertEquals("Hello", extWorkbook.getCellValue("Data", 1, 0));
    }

    @Test
    void testCallbacksWithDefaults()
    {
        // Test that defaults work when callbacks aren't provided
        ScoopCallbacks callbacks = ScoopCallbacks.builder().build();
        ScoopIntegration integration = new ScoopIntegration(callbacks);

        // Default permission is true
        assertTrue(integration.getContext().hasPermission("anything"));

        // Default data sources is empty
        assertTrue(integration.getContext().getDataSources().isEmpty());

        // Default reference resolver returns null
        assertNull(integration.getContext().resolveReference("Sheet", 0, 0));

        // User/tenant are null by default
        assertNull(integration.getContext().getUserId());
        assertNull(integration.getContext().getTenantId());
    }

    @Test
    void testMultiSheetWorkbook()
    {
        // Create a second sheet
        MemSheet sheet2 = workbook.createSheet("Summary");

        // Set up data in both sheets
        sheet.setCellAt(0, 0, new MemCell(100.0));  // Data!A1 = 100
        sheet.setCellAt(0, 1, new MemCell(50.0));   // Data!B1 = 50
        sheet2.setCellAt(0, 0, new MemCell(200.0)); // Summary!A1 = 200

        ScoopCallbacks callbacks = ScoopCallbacks.builder()
                .userId(() -> "testUser")
                .auditLogger((event, details) -> auditLog.add(event))
                .build();

        ScoopIntegration integration = new ScoopIntegration(callbacks);
        ExtendedWorkbook extWorkbook = integration.createWorkbook(workbook);
        extWorkbook.calculate();

        // Verify values in both sheets
        assertEquals(100.0, ((Number) extWorkbook.getCellValue("Data", 0, 0)).doubleValue(), 0.001);
        assertEquals(50.0, ((Number) extWorkbook.getCellValue("Data", 0, 1)).doubleValue(), 0.001);
        assertEquals(200.0, ((Number) extWorkbook.getCellValue("Summary", 0, 0)).doubleValue(), 0.001);

        // Verify audit logging
        assertTrue(auditLog.stream().anyMatch(log -> log.contains("CALCULATE")));
    }
}
