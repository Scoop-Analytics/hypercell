/**
 * Callback interfaces for Scoop integration.
 */
package com.scoopanalytics.hypercell.bridge.scoop;

import com.scoopanalytics.hypercell.api.DataSource;
import com.scoopanalytics.hypercell.core.grid.MemSheet;
import com.scoopanalytics.hypercell.core.grid.MemWorkbook;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Callback interfaces that Scoop provides to the bridge.
 *
 * <p>This allows Scoop to integrate with HyperCell without subclassing.
 * Scoop provides lambda functions for its specific logic.
 *
 * <p><b>Usage in Scoop:</b>
 * <pre>{@code
 * ScoopCallbacks callbacks = ScoopCallbacks.builder()
 *     .userId(() -> sc.getUserId())
 *     .tenantId(() -> sc.getOrganizationId())
 *     .dataSources(() -> convertQueries(sc.getInputQueries()))
 *     .queryRefresher((workbook, sheet) -> {
 *         CalculatedSourceWorkbook csw = new CalculatedSourceWorkbook(sc, null, workbook);
 *         var queries = csw.getQueries(false, false);
 *         for (var iq : queries) {
 *             if (iq.sheetName.startsWith(sheet.getName())) {
 *                 csw.refreshInputQuery(iq);
 *             }
 *         }
 *         return true;
 *     })
 *     .auditLogger((event, details) -> sc.logAudit(event, details))
 *     .permissionChecker(perm -> sc.hasPermission(perm))
 *     .build();
 *
 * ScoopIntegration integration = new ScoopIntegration(callbacks);
 * ExtendedWorkbook workbook = integration.createWorkbook(memWorkbook);
 * workbook.calculate();
 * }</pre>
 *
 * @since 0.1.0
 */
public class ScoopCallbacks
{
    private final Supplier<String> userIdSupplier;
    private final Supplier<String> tenantIdSupplier;
    private final Supplier<List<DataSource>> dataSourcesSupplier;
    private final Consumer<DataSource> dataSourceRefresher;
    private final BiFunction<MemWorkbook, MemSheet, Boolean> queryRefresher;
    private final Function<MemSheet, Boolean> querySheetChecker;
    private final Function<MemSheet, Boolean> needsRefreshChecker;
    private final java.util.function.BiConsumer<String, String> auditLogger;
    private final Function<String, Boolean> permissionChecker;
    private final TriFunction<String, Integer, Integer, Object> referenceResolver;

    private ScoopCallbacks(Builder builder)
    {
        this.userIdSupplier = builder.userIdSupplier;
        this.tenantIdSupplier = builder.tenantIdSupplier;
        this.dataSourcesSupplier = builder.dataSourcesSupplier;
        this.dataSourceRefresher = builder.dataSourceRefresher;
        this.queryRefresher = builder.queryRefresher;
        this.querySheetChecker = builder.querySheetChecker;
        this.needsRefreshChecker = builder.needsRefreshChecker;
        this.auditLogger = builder.auditLogger;
        this.permissionChecker = builder.permissionChecker;
        this.referenceResolver = builder.referenceResolver;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public String getUserId()
    {
        return userIdSupplier != null ? userIdSupplier.get() : null;
    }

    public String getTenantId()
    {
        return tenantIdSupplier != null ? tenantIdSupplier.get() : null;
    }

    public List<DataSource> getDataSources()
    {
        return dataSourcesSupplier != null ? dataSourcesSupplier.get() : List.of();
    }

    public void refreshDataSource(DataSource dataSource)
    {
        if (dataSourceRefresher != null)
        {
            dataSourceRefresher.accept(dataSource);
        }
    }

    public boolean refreshQuerySheet(MemWorkbook workbook, MemSheet sheet)
    {
        if (queryRefresher != null)
        {
            return queryRefresher.apply(workbook, sheet);
        }
        return false;
    }

    public boolean isQuerySheet(MemSheet sheet)
    {
        if (querySheetChecker != null)
        {
            return querySheetChecker.apply(sheet);
        }
        return sheet != null && sheet.isQuerySheet();
    }

    public boolean needsRefresh(MemSheet sheet)
    {
        if (needsRefreshChecker != null)
        {
            return needsRefreshChecker.apply(sheet);
        }
        if (sheet == null)
        {
            return false;
        }
        MemWorkbook workbook = sheet.getWorkbook();
        return sheet.isQuerySheet()
                && !sheet.isQuerySheetUpdated()
                && workbook != null
                && workbook.isRefreshQueryDataOnUse();
    }

    public void logAuditEvent(String eventType, String details)
    {
        if (auditLogger != null)
        {
            auditLogger.accept(eventType, details);
        }
    }

    public boolean hasPermission(String permission)
    {
        if (permissionChecker != null)
        {
            return permissionChecker.apply(permission);
        }
        return true;
    }

    public Object resolveReference(String sheetName, int row, int col)
    {
        if (referenceResolver != null)
        {
            return referenceResolver.apply(sheetName, row, col);
        }
        return null;
    }

    /**
     * Functional interface for 3-argument functions.
     */
    @FunctionalInterface
    public interface TriFunction<T, U, V, R>
    {
        R apply(T t, U u, V v);
    }

    /**
     * Builder for ScoopCallbacks.
     */
    public static class Builder
    {
        private Supplier<String> userIdSupplier;
        private Supplier<String> tenantIdSupplier;
        private Supplier<List<DataSource>> dataSourcesSupplier;
        private Consumer<DataSource> dataSourceRefresher;
        private BiFunction<MemWorkbook, MemSheet, Boolean> queryRefresher;
        private Function<MemSheet, Boolean> querySheetChecker;
        private Function<MemSheet, Boolean> needsRefreshChecker;
        private java.util.function.BiConsumer<String, String> auditLogger;
        private Function<String, Boolean> permissionChecker;
        private TriFunction<String, Integer, Integer, Object> referenceResolver;

        public Builder userId(Supplier<String> supplier)
        {
            this.userIdSupplier = supplier;
            return this;
        }

        public Builder tenantId(Supplier<String> supplier)
        {
            this.tenantIdSupplier = supplier;
            return this;
        }

        public Builder dataSources(Supplier<List<DataSource>> supplier)
        {
            this.dataSourcesSupplier = supplier;
            return this;
        }

        public Builder dataSourceRefresher(Consumer<DataSource> refresher)
        {
            this.dataSourceRefresher = refresher;
            return this;
        }

        public Builder queryRefresher(BiFunction<MemWorkbook, MemSheet, Boolean> refresher)
        {
            this.queryRefresher = refresher;
            return this;
        }

        public Builder querySheetChecker(Function<MemSheet, Boolean> checker)
        {
            this.querySheetChecker = checker;
            return this;
        }

        public Builder needsRefreshChecker(Function<MemSheet, Boolean> checker)
        {
            this.needsRefreshChecker = checker;
            return this;
        }

        public Builder auditLogger(java.util.function.BiConsumer<String, String> logger)
        {
            this.auditLogger = logger;
            return this;
        }

        public Builder permissionChecker(Function<String, Boolean> checker)
        {
            this.permissionChecker = checker;
            return this;
        }

        public Builder referenceResolver(TriFunction<String, Integer, Integer, Object> resolver)
        {
            this.referenceResolver = resolver;
            return this;
        }

        public ScoopCallbacks build()
        {
            return new ScoopCallbacks(this);
        }
    }
}
