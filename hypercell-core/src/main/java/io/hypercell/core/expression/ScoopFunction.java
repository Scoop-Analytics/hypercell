package io.hypercell.core.expression;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.burt.jmespath.Expression;
import io.burt.jmespath.JmesPath;
import io.burt.jmespath.jackson.JacksonRuntime;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.hypercell.api.HyperCellException;
import scoop.ai.ml.SavedModel;
import scoop.ai.ml.models.ModelContext;
import scoop.datagrid.ExcelDataGrid;
import scoop.datatable.ColumnMetadata;
import scoop.datatable.TimeSeriesPeriod;
import scoop.ingest.ReportInbox;
import scoop.metadata.ScoopMetadataObject;
import scoop.metric.CalendarType;
import scoop.processanalysis.ChangeCell;
import scoop.processanalysis.ConversionAnalysis;
import scoop.processanalysis.ConversionAnalysisCache;
import scoop.processanalysis.ProcessAnalysisDefinition;
import scoop.reportseriestable.ReportSeriesTable;
import scoop.timeseries.TimeSeries;
import scoop.workspace.Workspace;
import io.hypercell.api.CellAddress;
import scoop.worksheet.CustomFieldWorkbook;
import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;
import io.hypercell.core.grid.MemCellContext;
import io.hypercell.core.grid.MemCellStyle;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class ScoopFunction extends Function
{
    private static final Logger logger = LoggerFactory.getLogger(ScoopFunction.class);
    private Identifier id;
    private String reportSeriesTableID;
    private String selectedColumn;
    private String selectedSuccess;
    private ConversionAnalysis conversionAnalysis;
    private ModelContext modelContext;
    private Map<String, CellAddress> colMap;
    private Instances instances;
    private boolean badContext = false;

    public ScoopFunction(ParseTree tree, CompileContext cc)
    {
        super(tree, cc);
        if (type == ScoopExpressionLexer.SCOOPNEXTCONVERSION || type == ScoopExpressionLexer.SCOOPFINALCONVERSION)
        {
            if (tree.getChild(2) instanceof ScoopExpressionParser.STRINGContext)
            {
                var processDiagramName = tree.getChild(2).getChild(0).getText();
                processDiagramName = processDiagramName.substring(1, processDiagramName.length() - 1);
                var result = cc.getSc().sm().getProcessDiagrams(cc.getSc().workspaceID());
                ProcessAnalysisDefinition selectedPad = null;
                for (var pdp : result)
                {
                    if (pdp.getDiagramName().equals(processDiagramName))
                    {
                        selectedPad = pdp;
                        break;
                    }
                }
                if (selectedPad != null)
                {
                    ObjectMapper mapper = new ObjectMapper();
                    try
                    {
                        var preferences = mapper.readTree(selectedPad.getSavedObject());
                        reportSeriesTableID = preferences.get("selectedReportSeriesTable").textValue();
                        selectedColumn = preferences.get("selectedColumn").textValue();
                        selectedSuccess = preferences.get("selectedSuccess").textValue();
                        var rst = cc.getSc().getReportSeriesTable(reportSeriesTableID);
                        if (rst == null)
                        {
                            int index = reportSeriesTableID.indexOf("_");
                            var inboxID = reportSeriesTableID.substring(0, index);
                            var inboxList = cc.getSc().getInboxesForWorkspace();
                            Set<String> allColSet = new HashSet<>();
                            allColSet.add(selectedColumn);
                            List<ReportSeriesTable> rstList = cc.getSc().getReportSeriesTablesForWorkspace();
                            var pickedRst = ReportSeriesTable.pickBestTable(inboxID, rstList, inboxList, allColSet);
                            if (pickedRst != null)
                            {
                                reportSeriesTableID = pickedRst.getTableName();
                            }
                        }
                        id = new Identifier(reportSeriesTableID);
                        cc.addIdentifier(id);
                    } catch (JsonProcessingException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            }
        } else if (type == ScoopExpressionLexer.SCOOPPROMPT)
        {
        } else if (type == ScoopExpressionLexer.SCOOPJSON)
        {
        } else if (type == ScoopExpressionLexer.SCOOPLOOKUP)
        {
        } else if (type == ScoopExpressionLexer.SCOOPAPPLYMODEL)
        {
            if (cc.getSheet() != null)
            {
                colMap = CustomFieldWorkbook.getFieldAddresses(cc.getSheet());
            }
        } else if (type == ScoopExpressionLexer.SCOOP)
        {
        } else if (type == ScoopExpressionLexer.NULLTOKEN)
        {
        }
    }

    @Override
    public MemCell calculateCellValue()
    {
        if (type == ScoopExpressionLexer.SCOOPNEXTCONVERSION || type == ScoopExpressionLexer.SCOOPFINALCONVERSION)
        {
            if (expressions.size() < 4 || expressions.size() % 2 == 1 || reportSeriesTableID == null || selectedColumn == null || selectedSuccess == null)
            {
                return null;
            }
            MemCell mc = expressions.get(1).calculateCellValue();
            /*
             * 0 - Conversion rate
             * 1 - Total snapshot count of from state
             * 2 - Total number of conversions of from to to
             * 3 - Average duration
             */
            int resultType = mc.getNumberValue().intValue();
            // Calculate conversion rate
            var dsEn = id.getValue();
            var row = dsEn.row;
            var ds = dsEn.dataSet;
            Date date = new Date();
            TimeSeriesPeriod period = TimeSeriesPeriod.Annually;
            CalendarType calendar = CalendarType.Rolling;
            if (ds instanceof TimeSeries)
            {
                date = ((TimeSeries) ds).getDate(row);
                period = ((TimeSeries) ds).getPeriod();
                calendar = ((TimeSeries) ds).getCalendar();
            }
            List<String> categories = new ArrayList<>();
            List<ColumnMetadata> categoryColumns = new ArrayList<>();
            ColumnMetadata[] columnMetadata = ds.getColumnMetadata();
            if (columnMetadata.length > 1)
            {
                for (int i = 0; i < columnMetadata.length - 1; i++)
                {
                    categories.add((String) ds.getValue(row, i));
                    categoryColumns.add(columnMetadata[i]);
                }
            }
            if (conversionAnalysis == null)
            {
                conversionAnalysis = new ConversionAnalysis(cc.getSc(), reportSeriesTableID, selectedColumn,
                        selectedSuccess, categoryColumns);
                conversionAnalysis = ConversionAnalysisCache.getProcessedConversionAnalysis(conversionAnalysis,
                        period,
                        calendar);
            }
            mc = expressions.get(2).calculateCellValue();
            String from = mc.getStringValue();
            mc = expressions.get(3).calculateCellValue();
            String to = mc.getStringValue();
            ChangeCell changeCell = new ChangeCell();
            changeCell.date = LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault());
            changeCell.categories = categories;
            if (conversionAnalysis.getSnapshotKeys() != null)
            {
                var totalMap = conversionAnalysis.getSnapshotKeys().get(changeCell);
                if (resultType == 1)
                {
                    if (totalMap != null && totalMap.containsKey(from))
                    {
                        Set<String> totalSet = totalMap.get(from);
                        return new MemCell(totalSet.size());
                    }
                    return null;
                }
                var summary = (type == ScoopExpressionLexer.SCOOPFINALCONVERSION) ?
                        conversionAnalysis.getFinalSummaries().get(changeCell) : conversionAnalysis.getNextSummaries()
                                                                                                   .get(changeCell);
                if (summary != null)
                {
                    int fromIndex = -1;
                    int toIndex = -1;
                    for (int i = 0; i < summary.keys.length; i++)
                    {
                        if (summary.keys[i].equals(from))
                        {
                            fromIndex = i;
                        }
                        if (summary.keys[i].equals(to))
                        {
                            toIndex = i;
                        }
                    }
                    if (fromIndex >= 0 && toIndex >= 0)
                    {
                        int count = summary.counts[fromIndex][toIndex];
                        Double avgDuration = summary.avgDurations[fromIndex][toIndex];
                        if (resultType == 2)
                        {
                            return new MemCell(count);
                        }
                        if (resultType == 3)
                        {
                            if (avgDuration != null)
                            {
                                // Average duration in days
                                return new MemCell(avgDuration / 1000 / 60 / 60 / 24);
                            } else
                            {
                                return null;
                            }
                        }
                        if (totalMap != null)
                        {
                            Set<String> totalSet = totalMap.get(from);
                            if (totalSet != null)
                            {
                                /*
                                 * Rate: Number that transitioned in that period, divided by the original starting number
                                 */
                                return new MemCell(((double) count) / ((double) totalSet.size()));
                            } else
                            {
                            }
                        }
                    }
                } else
                {
                }
            }
        } else if (type == ScoopExpressionLexer.SCOOPPROMPT)
        {
            MemCell mc = expressions.getFirst().calculateCellValue();
            if (cc.getSheet().getWorkbook().getPromptMap() != null)
            {
                var promptName = expressions.getFirst().calculateCellValue();
                if (promptName.getStringValue() != null)
                {
                    var result = cc.getSheet().getWorkbook().getPromptMap().get(promptName.getStringValue());
                    if (result != null)
                    {
                        return new MemCell(result);
                    }
                }
            }
            return expressions.get(1).calculateCellValue();
        } else if (type == ScoopExpressionLexer.SCOOPJSON)
        {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = null;
            try
            {
                String jsonValue = expressions.getFirst().calculateCellValue().getStringValue();
                if (jsonValue != null)
                {
                    jsonValue = jsonValue.replace("\\\"", "\"");
                    rootNode = mapper.readTree(jsonValue);
                    // Initialize JmesPath
                    JmesPath<JsonNode> jmespath = new JacksonRuntime();

                    // Compile JMESPath expression
                    Expression<JsonNode> expression = jmespath.compile(
                            expressions.get(1).calculateCellValue().getStringValue());

                    // Evaluate expression
                    JsonNode result = expression.search(rootNode);
                    if (result.textValue() != null)
                    {
                        return new MemCell(result.textValue());
                    } else if (result instanceof ArrayNode arrayNode && arrayNode.get(0).isTextual())
                    {
                        return new MemCell(arrayNode.get(0).textValue());
                    }
                }
            } catch (JsonProcessingException e)
            {
                return null;
            }
        } else if (type == ScoopExpressionLexer.SCOOPLOOKUP)
        {
            MemCell lookupValue = expressions.getFirst().calculateCellValue();
            MemCell datasetName = expressions.get(1).calculateCellValue();
            MemCell lookupColumn = expressions.get(2).calculateCellValue();
            MemCell resultColumn = expressions.get(3).calculateCellValue();
            if (lookupValue == null || datasetName == null || lookupColumn == null || resultColumn == null || badContext)
                return new MemCell(FormulaError.NA);
            var lookupMap = cc.getSheet().getWorkbook().getLookupMap();
            String key = datasetName.getStringValue() + "|" + lookupColumn.getStringValue() + "|" + resultColumn.getStringValue();
            Map<String, Object> dataMap = null;
            synchronized (lookupMap)
            {
                dataMap = lookupMap.get(key);
                if (dataMap == null)
                {
                    List<ScoopMetadataObject> inboxes = cc.getSc().sm().queryForObjects(ReportInbox.class,
                            "label='" + datasetName + "' and workspaceID='" + cc.getSc().workspaceID() + "'");
                    if (inboxes != null && !inboxes.isEmpty())
                    {
                        ReportInbox reportInbox = (ReportInbox) inboxes.getFirst();
                        var rstList = cc.getSc().getReportSeriesTablesForInboxID(reportInbox.inboxID);
                        if (rstList != null && !rstList.isEmpty())
                        {
                            dataMap = new HashMap<>();
                            // Support tour workspaces: check for override schema first
                            ReportSeriesTable rst = rstList.getFirst();
                            Workspace workspace = cc.getSc().getWorkspace();
                            String clusterId = workspace != null ? workspace.getDataClusterId() : null;
                            String schemaToUse = rst.getTableMetadata().overrideSchema != null
                                ? rst.getTableMetadata().overrideSchema
                                : reportInbox.workspaceID;
                            String query = "SELECT DISTINCT `" + lookupColumn + "`, `" + resultColumn + "` FROM " + schemaToUse + "." + rst.getTableName();
                            try
                            {
                                Statement stmt = cc.getSc().sm().getDataDBConnection(clusterId, schemaToUse).createStatement();
                                ResultSet rs = stmt.executeQuery(query);
                                while (rs.next())
                                {
                                    dataMap.put(rs.getString(1), rs.getObject(2));
                                }
                                lookupMap.put(key, dataMap);
                            } catch (SQLException e)
                            {
                                logger.error("Unable to query for lookup table", e);
                                badContext = true;
                            }
                        }
                    }
                }
            }
            if (dataMap == null)
            {
                return new MemCell(FormulaError.NA);
            }
            Object result = dataMap.get(lookupValue.getStringValue());
            switch (result)
            {
                case String string ->
                {
                    return new MemCell(string);
                }
                case null ->
                {
                    return null;
                }
                case Timestamp timestamp ->
                {
                    MemCell memCell = new MemCell(DateTimeFunction.getSheetDateNumber(timestamp.getTime()));
                    var cellContext = new MemCellContext();
                    var cellStyle = new MemCellStyle();
                    cellContext.setStyle(cellStyle);
                    cellStyle.setFormatString("MM/dd/yyyy");
                    memCell.setCellContext(cellContext);
                    return memCell;
                }
                default ->
                {
                    return new MemCell(result.toString());
                }
            }
        } else if (type == ScoopExpressionLexer.SCOOPAPPLYMODEL)
        {
            if (colMap == null || badContext || cc.getSheet() == null)
                return new MemCell(FormulaError.NA);
            if (modelContext == null)
            {
                String modelName = expressions.getFirst().calculateCellValue().getStringValue();
                try
                {
                    List<ScoopMetadataObject> resultList = cc.getSc().sm().queryForObjects(SavedModel.class,
                            "workspaceID='" + cc.getSc().workspaceID() + "' AND modelName='" + modelName + "'");
                    if (resultList == null || resultList.isEmpty())
                        return new MemCell(FormulaError.NA);
                    SavedModel savedModel = (SavedModel) resultList.getFirst();
                    byte[] modelObject = null;
                    modelObject = cc.getSc().sm()
                                    .getS3ObjectBytes(cc.getSc().sm().getCustomers3Bucket(), savedModel.modelS3Key);
                    modelContext = ModelContext.deserialize(modelObject);
                    ArrayList<Attribute> attributeList = new ArrayList<>(List.of(modelContext.attributes));
                    instances = new Instances("Cluster", attributeList, 0);
                } catch (IOException | ClassNotFoundException | HyperCellException e)
                {
                    logger.error("Unable to get model context for model: {}", modelName, e);
                    badContext = true;
                    return new MemCell(FormulaError.NA);
                }
            }
            if (instances == null || modelContext == null)
                return new MemCell(FormulaError.NA);
            double[] values = new double[modelContext.attributes.length];
            for (int i = 0; i < modelContext.attributes.length; i++)
            {
                CellAddress cellAddress = colMap.get(modelContext.attributes[i].name());
                if (cellAddress == null)
                    continue;
                MemCell mc = cc.getSheet().getCellAt(cellAddress);
                double val = Double.NaN;
                if (mc != null)
                {
                    if (modelContext.attributes[i].isNominal())
                    {
                        String value = mc.getStringValue();
                        if (value != null)
                        {
                            String stringValue = mc.getStringValue();
                            val = modelContext.attributes[i].indexOfValue(stringValue);
                            if (val < 0)
                            {
                                val = Double.NaN;
                            }
                        }
                    } else
                    {
                        val = mc.getDoubleValue();
                    }
                }
                values[i] = val;
            }
            /* Potentially add filters later
            if (modelContext.filters != null)
            {
                QueryFilter filter = modelContext.filters[0];
                if (modelContext.filters.length > 1)
                {
                    CompoundFilter cf = new CompoundFilter();
                    cf.boperator = FilterOperator.And;
                    cf.filters = new ArrayList<>(List.of(modelContext.filters));
                    filter = cf;
                }
                if (!filter.filterRow(modelContext.datasetColumns, )
            }*/
            DenseInstance denseInstance = new DenseInstance(1, values);
            denseInstance.setDataset(instances);
            try
            {
                int cluster = modelContext.clusterer.clusterInstance(denseInstance);
                return new MemCell(modelContext.clusterIDMap.get("cluster_" + cluster));
            } catch (Exception e)
            {
                return new MemCell((String) null);
            }
        } else if (type == ScoopExpressionLexer.SCOOP)
        {
            return new MemCell(expressions.getFirst().calculateCellValue());
        } else if (type == ScoopExpressionLexer.NULLTOKEN)
        {
            return null;
        }
        return null;
    }
}
