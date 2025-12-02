/**
 *
 */
package io.hypercell.core.expression;
import io.hypercell.formula.HyperCellExpressionParser;
import io.hypercell.formula.HyperCellExpressionLexer;

import com.google.common.util.concurrent.AtomicDouble;
import org.antlr.v4.runtime.tree.ParseTree;
import scoop.datatable.AggregationRule;
import scoop.worksheet.CalculatedSourceWorkbook;
import io.hypercell.api.CellAddress;
import io.hypercell.core.grid.FormulaError;
import io.hypercell.core.grid.MemCell;
import io.hypercell.core.grid.MemSheet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * @author bradpeters
 */
public class MathFunction extends Function
{
    private Map<String, Set<Integer>> ifMap = null;
    private Map<ScoopExpression, Map<String, Set<Integer>>> ifsMap = null;
    private Map<ScoopExpression, Integer> ifsSizes = null;
    private Map<ScoopExpression, Criteria> criteriaMap = null;
    private final AtomicInteger nonDateCount = new AtomicInteger(0);

    public MathFunction(ParseTree tree, CompileContext cc)
    {
        super(tree, cc);
        if (expressions != null && expressions.size() == 1 && expressions.getFirst() instanceof Identifier identifier)
        {
            if (type == HyperCellExpressionLexer.SUMTOKEN)
            {
                identifier.setAggregationRule(AggregationRule.Sum);
            } else if (type == HyperCellExpressionLexer.COUNTTOKEN)
            {
                identifier.setAggregationRule(AggregationRule.Count);
            } else if (type == HyperCellExpressionLexer.AVERAGETOKEN)
            {
                identifier.setAggregationRule(AggregationRule.Avg);
            } else if (type == HyperCellExpressionLexer.MINTOKEN)
            {
                identifier.setAggregationRule(AggregationRule.Min);
            } else if (type == HyperCellExpressionLexer.MAXTOKEN)
            {
                identifier.setAggregationRule(AggregationRule.Max);
            } else if (type == HyperCellExpressionLexer.STDEVTOKEN)
            {
                identifier.setAggregationRule(AggregationRule.StdDev);
            }
        }
    }

    @Override
    public MemCell calculateCellValue()
    {
        if (memCellCalculationCache != null)
        {
            var cacheValue = memCellCalculationCache.getValue();
            if (cacheValue != null)
            {
                return cacheValue;
            }
        }
        double sum = 0;
        double min = Double.NaN;
        double max = Double.NaN;
        int count = 0;
        List<Double> values = new ArrayList<>();
        MemCell memCellResult = null;
        if (type == HyperCellExpressionLexer.LOGTOKEN || type == HyperCellExpressionLexer.LOG10TOKEN || type == HyperCellExpressionLexer.LNTOKEN || type == HyperCellExpressionLexer.EXPTOKEN)
        {
            MemCell mc = expressions.getFirst().calculateCellValue();
            if (mc == null)
            {
                memCellResult = new MemCell(FormulaError.NA);
            } else
            {
                Number n = mc.getNumberValue();
                if (n == null)
                {
                    return new MemCell(FormulaError.VALUE);
                }
                if (type == HyperCellExpressionLexer.LOGTOKEN || type == HyperCellExpressionLexer.LOG10TOKEN)
                {
                    memCellResult = new MemCell(Math.log10(n.doubleValue()));
                } else if (type == HyperCellExpressionLexer.LNTOKEN)
                {
                    memCellResult = new MemCell(Math.log(n.doubleValue()));
                } else if (type == HyperCellExpressionLexer.EXPTOKEN)
                {
                    memCellResult = new MemCell(Math.exp(n.doubleValue()));
                }
            }
            return getReturn(memCellResult);
        } else if (type == HyperCellExpressionLexer.COUNTIFTOKEN || type == HyperCellExpressionLexer.SUMIFTOKEN || type == HyperCellExpressionLexer.COUNTIFSTOKEN || type == HyperCellExpressionLexer.SUMIFSTOKEN || type == HyperCellExpressionLexer.AVERAGEIFTOKEN || type == HyperCellExpressionLexer.AVERAGEIFSTOKEN || type == HyperCellExpressionLexer.MAXIFSTOKEN || type == HyperCellExpressionLexer.MINIFSTOKEN)
        {
            cc.setContainsAggregation(true);
            if (canPushDown())
            {
                return pushDown();
            }
            int start = 0;
            List<CellAddress> addresses;
            MemSheet valueSheet;
            boolean[] results = null;
            boolean[] touched = null;
            if (type == HyperCellExpressionLexer.AVERAGEIFTOKEN || type == HyperCellExpressionLexer.SUMIFTOKEN || type == HyperCellExpressionLexer.COUNTIFTOKEN)
            {
                Range criteriaValueRange = (Range) (expressions.size() == 2 ? expressions.get(0) : expressions.get(2));
                if (criteriaValueRange == null) return new MemCell(FormulaError.VALUE);
                addresses = criteriaValueRange.getAddresses();
                ScoopExpression criteria = expressions.get(1);
                MemCell criteriaMc = criteria.calculateCellValue();
                populateIfNecessary(criteriaMc);
                Criteria c = null;
                if (criteriaMc != null)
                {
                    c = new Criteria(criteriaMc, nonDateCount);
                    if (c.operator == null && cc.getSheet().getWorkbook().isUseIndices())
                    {
                        // Can index
                        if (ifMap == null)
                        {
                            ifMap = new ConcurrentHashMap<>();
                            IntStream.range(0, addresses.size()).parallel().forEach(i -> {
                                MemCell key = cc.getSheet().getCellAt(addresses.get(i));
                                if (key != null)
                                {
                                    String value = key.getStringValue();
                                    if (value == null)
                                    {
                                        Number n = key.getNumberValue();
                                        if (n != null)
                                        {
                                            value = Double.toString(n.doubleValue());
                                        }
                                    }
                                    Set<Integer> resultsList = ifMap.computeIfAbsent(value,
                                            k -> Collections.synchronizedSet(new HashSet<>()));
                                    resultsList.add(i);
                                }
                            });
                        }
                    }
                }
                Range criteriaRange = (Range) (expressions.size() == 2 ? criteriaValueRange : expressions.getFirst());
                if (ifMap != null)
                {
                    var criteriaValue = criteria.calculateCellValue();
                    Set<Integer> rowSet = null;
                    if (criteriaValue != null)
                    {
                        String value = criteriaValue.getStringValue();
                        if (value == null)
                        {
                            Number n = criteriaValue.getNumberValue();
                            if (n != null)
                            {
                                value = Double.toString(n.doubleValue());
                            }
                        }
                        rowSet = ifMap.get(value);
                    }
                    results = new boolean[addresses.size()];
                    touched = new boolean[addresses.size()];
                    for (int i = 0; i < addresses.size(); i++)
                    {
                        if (rowSet != null)
                        {
                            results[i] = rowSet.contains(i);
                        } else
                        {
                            results[i] = false;
                        }
                        touched[i] = true;
                    }
                } else if (c != null)
                {
                    List<CellAddress> criteriaAddresses = criteriaRange.getAddresses();
                    results = new boolean[criteriaAddresses.size()];
                    touched = new boolean[criteriaAddresses.size()];
                    c.evaluateCriteriaAddresses(cc.getSheet(), criteriaAddresses, results, touched);
                }
                valueSheet = criteriaValueRange.getSheet();
            } else
            {
                if (type == HyperCellExpressionLexer.SUMIFSTOKEN || type == HyperCellExpressionLexer.AVERAGEIFSTOKEN || type == HyperCellExpressionLexer.MAXIFSTOKEN || type == HyperCellExpressionLexer.MINIFSTOKEN)
                {
                    start = 1;
                    Range sumRange = (Range) expressions.getFirst();
                    valueSheet = sumRange.getSheet();
                    addresses = sumRange.getAddresses();
                    var firstMc = valueSheet.getCellAt(addresses.getFirst());
                    populateIfNecessary(firstMc);
                } else
                {
                    valueSheet = null;
                    addresses = null;
                }
                int maxLength = 0;
                List<List<CellAddress>> criteriaAddressList = new ArrayList<>();
                for (int pos = start; pos < expressions.size(); pos += 2)
                {
                    if (ifsMap != null && ifsMap.containsKey(expressions.get(pos)))
                    {
                        if (maxLength == 0)
                        {
                            maxLength = ifsSizes.get(expressions.get(pos));
                        }
                        continue;
                    }
                    Range r = (Range) expressions.get(pos);
                    List<CellAddress> criteriaAddresses = r.getAddresses();
                    if (criteriaAddresses.size() > maxLength)
                    {
                        maxLength = criteriaAddresses.size();
                    }
                    criteriaAddressList.add(criteriaAddresses);
                }
                results = new boolean[maxLength];
                touched = new boolean[maxLength];
                int listCount = 0;
                for (int pos = start; pos < expressions.size(); pos += 2)
                {
                    Range r = (Range) expressions.get(pos);
                    ScoopExpression criteria = expressions.get(pos + 1);
                    MemCell criteriaMc = criteria.calculateCellValue();
                    if (criteriaMc == null)
                    {
                        Arrays.fill(results, false);
                        break;
                    }
                    Criteria c = null;
                    if (criteriaMc != null && (criteria instanceof SheetString || criteria instanceof SheetNumber))
                    {
                        if (criteriaMap == null)
                        {
                            criteriaMap = new ConcurrentHashMap<>();
                        }
                        c = criteriaMap.get(expressions.get(pos));
                        if (c == null)
                        {
                            c = new Criteria(criteriaMc, nonDateCount);
                            criteriaMap.put(expressions.get(pos), c);
                        }
                    } else
                    {
                        c = new Criteria(criteriaMc, nonDateCount);
                    }
                    // Indexable if doing exact matching
                    if (c.indexAble())
                    {
                        if (ifsMap == null)
                        {
                            ifsMap = new ConcurrentHashMap<>();
                            ifsSizes = new ConcurrentHashMap<>();
                        }
                        if (!ifsMap.containsKey(expressions.get(pos)))
                        {
                            // Cache the value at each address and just do a lookup like if map
                            List<CellAddress> criteriaAddresses = criteriaAddressList.get(listCount++);
                            Map<String, Set<Integer>> ifsExpressionMap = new ConcurrentHashMap<>();
                            ifsMap.put(criteria, ifsExpressionMap);
                            IntStream.range(0, criteriaAddresses.size()).parallel().forEach(i -> {
                                MemCell key = cc.getSheet().getCellAt(criteriaAddresses.get(i));
                                if (key != null)
                                {
                                    String value = key.getStringValue();
                                    if (value == null)
                                    {
                                        Number n = key.getNumberValue();
                                        if (n != null)
                                        {
                                            value = Double.toString(n.doubleValue());
                                        }
                                    } else
                                    {
                                        value = value.toLowerCase();
                                    }
                                    if (value != null)
                                    {
                                        Set<Integer> resultsList = ifsExpressionMap.computeIfAbsent(value,
                                                k -> Collections.synchronizedSet(new HashSet<>()));
                                        resultsList.add(i);
                                    }
                                }
                            });
                            ifsMap.put(expressions.get(pos), ifsExpressionMap);
                            ifsSizes.put(expressions.get(pos), criteriaAddresses.size());
                        }
                        var ifsExpressionMap = ifsMap.get(expressions.get(pos));
                        String stringValue = criteriaMc.getStringValue();
                        if (stringValue != null && stringValue.charAt(0) == '=')
                        {
                            stringValue = stringValue.substring(1);
                        }
                        var rowSet = stringValue == null ? null : ifsExpressionMap.get(stringValue.toLowerCase());
                        for (int i = 0; i < results.length; i++)
                        {
                            if (rowSet != null && rowSet.contains(i))
                            {
                                if (!touched[i])
                                {
                                    results[i] = true;
                                }
                            } else
                            {
                                results[i] = false;
                            }
                            touched[i] = true;
                        }
                    } else
                    {
                        if (listCount < criteriaAddressList.size())
                        {
                            List<CellAddress> criteriaAddresses = criteriaAddressList.get(listCount++);
                            c.evaluateCriteriaAddresses(cc.getSheet(), criteriaAddresses, results, touched);
                        }
                    }
                }
            }

            AtomicDouble atomicSum = new AtomicDouble(0);
            AtomicInteger atomicCount = new AtomicInteger(0);
            AtomicDouble atomicMax = new AtomicDouble(Double.MIN_VALUE);
            AtomicDouble atomicMin = new AtomicDouble(Double.MAX_VALUE);
            int maxValue = results == null ? 0 : results.length;
            if (addresses != null && (results == null || addresses.size() < results.length))
            {
                maxValue = addresses.size();
            }
            if (touched != null && results != null)
            {
                boolean[] finalResults = results;
                boolean[] finalTouched = touched;
                IntStream.range(0, maxValue).parallel().forEach(i -> {
                    if (finalTouched[i] && finalResults[i])
                    {
                        if (type == HyperCellExpressionLexer.SUMIFSTOKEN || type == HyperCellExpressionLexer.SUMIFTOKEN || type == HyperCellExpressionLexer.AVERAGEIFTOKEN || type == HyperCellExpressionLexer.AVERAGEIFSTOKEN || type == HyperCellExpressionLexer.MAXIFSTOKEN || type == HyperCellExpressionLexer.MINIFSTOKEN)
                        {
                            if (addresses != null)
                            {
                                MemCell mc = valueSheet.getCellAt(addresses.get(i));
                                if (mc != null)
                                {
                                    mc.calculate();
                                    if (mc.getNumberValue() != null)
                                    {
                                        double val = mc.getNumberValue().doubleValue();
                                        atomicSum.addAndGet(val);
                                        atomicMax.updateAndGet(currentValue -> Math.max(currentValue, val));
                                        atomicMin.updateAndGet(currentValue -> Math.min(currentValue, val));
                                    }
                                }
                            }
                        }
                        atomicCount.incrementAndGet();
                    }
                });

            }
            sum = atomicSum.get();
            count = atomicCount.get();
            max = count > 0 ? atomicMax.get() : 0;
            min = count > 0 ? atomicMin.get() : 0;
        } else if (type == HyperCellExpressionLexer.SUBTOTALTOKEN)
        {
            MemCell sttype = expressions.getFirst().calculateCellValue();
            if (sttype == null || sttype.getNumberValue() == null)
            {
                return new MemCell(FormulaError.NA);
            }
            int type = (int) sttype.getNumberValue().doubleValue();
            List<CellAddress> addresses = new ArrayList<>();
            for (int i = 1; i < expressions.size(); i++)
            {
                ScoopExpression exp = expressions.get(i);
                if (exp instanceof Range)
                {
                    addresses.addAll(((Range) exp).getAddresses());
                } else if (exp instanceof Identifier)
                {
                    addresses.add(((Identifier) exp).getAddress());
                }
            }
            int counta = 0;
            double product = 1;
            max = Double.MIN_VALUE;
            min = Double.MAX_VALUE;
            List<Double> data = new ArrayList<>();
            for (CellAddress ca : addresses)
            {
                MemCell mc = cc.getSheet().getCellAt(ca);
                if (mc == null)
                {
                    continue;
                }
                counta++;
                if (mc.getNumberValue() != null)
                {
                    count++;
                    double d = mc.getNumberValue().doubleValue();
                    if (d < min)
                    {
                        min = d;
                    }
                    if (d > max)
                    {
                        max = d;
                    }
                    sum += d;
                    product *= d;
                    data.add(d);
                }
            }
            double mean = 0;
            double s = 0;
            switch (type)
            {
                case 1:
                case 101:
                    // Average
                    return new MemCell(sum / count);
                case 2:
                case 102:
                    // Count
                    return new MemCell(count);
                case 3:
                case 103:
                    // CountA
                    return new MemCell(counta);
                case 4:
                case 104:
                    // Max
                    return new MemCell(max);
                case 5:
                case 105:
                    // Min
                    return new MemCell(min);
                case 6:
                case 106:
                    // Product
                    return new MemCell(product);
                case 7:
                case 107:
                    // StdDev
                    mean = sum / count;
                    s = 0;
                    for (double d : data)
                    {
                        s += (d - mean) * (d - mean);
                    }
                    return new MemCell(Math.sqrt(s / (count - 1)));
                case 8:
                case 108:
                    // StdDev Population
                    mean = sum / count;
                    s = 0;
                    for (double d : data)
                    {
                        s += (d - mean) * (d - mean);
                    }
                    return new MemCell(Math.sqrt(s / count));
                case 9:
                case 109:
                    // Sum
                    return new MemCell(sum);
                case 10:
                case 110:
                    // Var
                    mean = sum / count;
                    s = 0;
                    for (double d : data)
                    {
                        s += (d - mean) * (d - mean);
                    }
                    return new MemCell(s / (count - 1));
                case 11:
                case 111:
                    // Var Population
                    mean = sum / count;
                    s = 0;
                    for (double d : data)
                    {
                        s += (d - mean) * (d - mean);
                    }
                    return new MemCell(s / count);
            }
        } else if (type == HyperCellExpressionLexer.SUMPRODUCTTOKEN)
        {
            cc.setContainsAggregation(true);
            // Normal version with multiple ranges
            List<List<CellAddress>> ranges = new ArrayList<>();
            List<MemCell> filters = new ArrayList<>();
            int lastSize = -1;
            for (ScoopExpression expression : expressions)
            {
                Range r = (Range) expression;
                List<CellAddress> addresses = r.getAddresses();
                if (lastSize < 0)
                {
                    lastSize = addresses.size();
                } else if (lastSize != addresses.size())
                {
                    return new MemCell(FormulaError.VALUE);
                }
                ranges.add(addresses);
                if (r.getFilter() != null)
                {
                    filters.add(r.getFilter().calculateCellValue());
                } else
                {
                    filters.add(null);
                }
            }
            for (int index = 0; index < lastSize; index++)
            {
                double val = 1;
                for (int range = 0; range < ranges.size(); range++)
                {
                    MemCell mc = cc.getSheet().getCellAt(ranges.get(range).get(index));
                    boolean isFilter = false;
                    if (filters.get(range) != null)
                    {
                        if (mc == null || !mc.equals(filters.get(range)))
                        {
                            val = 0;
                            continue;
                        }
                        isFilter = true;
                    }
                    if (!isFilter)
                    {
                        if (mc != null && mc.getNumberValue() != null)
                        {
                            val *= mc.getNumberValue().doubleValue();
                        } else
                        {
                            val = 0;
                        }
                    }
                }
                sum += val;
            }
        } else
        {
            for (ScoopExpression exp : expressions)
            {
                if (exp instanceof Range)
                {
                    cc.setContainsAggregation(true);
                    var addresses = ((Range) exp).getAddresses();
                    MemCell mc = cc.getSheet().getCellAt(addresses.getFirst());
                    if (mc != null)
                    {
                        populateIfNecessary(mc);
                    }
                    for (CellAddress address : ((Range) exp).getAddresses())
                    {
                        mc = cc.getSheet().getCellAt(address);
                        if (mc == null) continue;
                        mc.calculate();
                        Number n = mc.getNumberValue();
                        if (n != null)
                        {
                            double d = n.doubleValue();
                            values.add(d);
                            sum += d;
                            if (Double.isNaN(max) || d > max) max = d;
                            if (Double.isNaN(min) || d < min) min = d;
                            count++;
                        }
                    }
                } else
                {
/*                  Basic aggregations now allowed
                    if (exp instanceof Identifier identifier)
                    {
                        if (identifier.getAddress() == null)
                        {
                            cc.setContainsAggregation(true);
                        }
                    }*/
                    if (exp == null) return new MemCell(FormulaError.NA);
                    MemCell mc = exp.calculateCellValue();
                    if (mc == null) continue;
                    Number n = mc.getNumberValue();
                    if (n != null)
                    {
                        double d = n.doubleValue();
                        values.add(d);
                        sum += d;
                        if (Double.isNaN(max) || d > max) max = d;
                        if (Double.isNaN(min) || d < min) min = d;
                        count++;
                    }
                }
            }
        }
        if (type == HyperCellExpressionLexer.SUMTOKEN)
        {
            return getReturn(new MemCell(sum));
        } else if (type == HyperCellExpressionLexer.COUNTTOKEN || type == HyperCellExpressionLexer.COUNTATOKEN)
        {
            return getReturn(new MemCell(count));
        } else if (type == HyperCellExpressionLexer.AVERAGETOKEN || type == HyperCellExpressionLexer.AVERAGEIFTOKEN || type == HyperCellExpressionLexer.AVERAGEIFSTOKEN)
        {
            if (count == 0) return new MemCell(FormulaError.DIV0);
            return getReturn(new MemCell(sum / ((double) count)));
        } else if (type == HyperCellExpressionLexer.MAXTOKEN)
        {
            return getReturn(new MemCell(max));
        } else if (type == HyperCellExpressionLexer.MINTOKEN)
        {
            return getReturn(new MemCell(min));
        } else if (type == HyperCellExpressionLexer.STDEVTOKEN)
        {
            double avg = sum / ((double) count);
            double sumsq = 0;
            for (Double d : values)
            {
                double diff = d - avg;
                sumsq += diff * diff;
            }
            return getReturn(new MemCell(Math.sqrt((sumsq / ((double) count - 1)))));
        } else if (type == HyperCellExpressionLexer.MEDIANTOKEN)
        {
            Collections.sort(values);
            if (values.isEmpty())
            {
                return getReturn(new MemCell(FormulaError.VALUE));
            }
            if (values.size() % 2 == 1)
            {
                return getReturn(new MemCell(values.get(values.size() / 2)));
            } else
            {
                double val1 = values.get(values.size() / 2 - 1);
                double val2 = values.get(values.size() / 2);
                return getReturn(new MemCell((val1 + val2) / 2));
            }
        } else if (type == HyperCellExpressionLexer.COUNTIFSTOKEN || type == HyperCellExpressionLexer.COUNTIFTOKEN)
        {
            return getReturn(new MemCell(count));
        } else if (type == HyperCellExpressionLexer.SUMIFSTOKEN || type == HyperCellExpressionLexer.SUMIFTOKEN)
        {
            return getReturn(new MemCell(sum));
        } else if (type == HyperCellExpressionLexer.MAXIFSTOKEN)
        {
            return getReturn(max > Double.MIN_VALUE ? new MemCell(max) : new MemCell(FormulaError.NA));
        } else if (type == HyperCellExpressionLexer.MINIFSTOKEN)
        {
            return getReturn(min < Double.MAX_VALUE ? new MemCell(min) : new MemCell(FormulaError.NA));
        } else if (type == HyperCellExpressionLexer.SUMPRODUCTTOKEN)
        {
            return getReturn(new MemCell(sum));
        } else if (type == HyperCellExpressionLexer.ABSTOKEN)
        {
            MemCell mc = expressions.getFirst().calculateCellValue();
            if (mc == null || mc.getNumberValue() == null) return getReturn(new MemCell(FormulaError.NA));
            else return getReturn(new MemCell(Math.abs(mc.getNumberValue().doubleValue())));
        } else if (type == HyperCellExpressionLexer.SQRTTOKEN)
        {
            MemCell mc = expressions.getFirst().calculateCellValue();
            if (mc == null || mc.getNumberValue() == null) return getReturn(new MemCell(FormulaError.NA));
            else
            {
                double num = mc.getNumberValue().doubleValue();
                if (num < 0) return getReturn(new MemCell(FormulaError.NUM));
                return getReturn(new MemCell(Math.sqrt(mc.getNumberValue().doubleValue())));
            }
        } else if (type == HyperCellExpressionLexer.CEILINGTOKEN)
        {
            MemCell number = expressions.get(0).calculateCellValue();
            if (number == null || number.getNumberValue() == null) return getReturn(new MemCell(FormulaError.NA));
            MemCell significance = expressions.get(1).calculateCellValue();
            if (significance == null || significance.getNumberValue() == null)
                return getReturn(new MemCell(FormulaError.NA));
            double num = number.getNumberValue().doubleValue();
            double sig = significance.getNumberValue().doubleValue();
            return getReturn(new MemCell(Math.ceil(num / sig) * sig));
        } else if (type == HyperCellExpressionLexer.FLOORTOKEN)
        {
            MemCell number = expressions.get(0).calculateCellValue();
            if (number == null || number.getNumberValue() == null) return new MemCell(FormulaError.NA);
            MemCell significance = expressions.size() > 1 ? expressions.get(1).calculateCellValue() : new MemCell(1);
            if (significance == null || significance.getNumberValue() == null)
                return getReturn(new MemCell(FormulaError.NA));
            double num = number.getNumberValue().doubleValue();
            double sig = significance.getNumberValue().doubleValue();
            if (sig * num < 0) return new MemCell(FormulaError.NUM);
            return getReturn(new MemCell(Math.floor(num / sig) * sig));
        } else if (type == HyperCellExpressionLexer.INTTOKEN)
        {
            MemCell number = expressions.getFirst().calculateCellValue();
            if (number == null || number.getNumberValue() == null) return getReturn(new MemCell(FormulaError.NA));
            else return getReturn(new MemCell(Math.floor(number.getNumberValue().doubleValue())));
        } else if (type == HyperCellExpressionLexer.MODTOKEN)
        {
            MemCell number = expressions.get(0).calculateCellValue();
            if (number == null || number.getNumberValue() == null) return new MemCell(FormulaError.NA);
            MemCell divisor = expressions.get(1).calculateCellValue();
            if (divisor == null || divisor.getNumberValue() == null) return new MemCell(FormulaError.NA);
            double num = number.getNumberValue().doubleValue();
            double div = divisor.getNumberValue().doubleValue();
            double result = num % div;
            if (result * div < 0) result = -result;
            return getReturn(new MemCell(result));
        } else if (type == HyperCellExpressionLexer.POWERTOKEN)
        {
            MemCell number = expressions.get(0).calculateCellValue();
            if (number == null || number.getNumberValue() == null) return new MemCell(FormulaError.NA);
            MemCell power = expressions.get(1).calculateCellValue();
            if (power == null || power.getNumberValue() == null) return new MemCell(FormulaError.NA);
            double num = number.getNumberValue().doubleValue();
            double pow = power.getNumberValue().doubleValue();
            double result = Math.pow(num, pow);
            return getReturn(new MemCell(result));
        } else if (type == HyperCellExpressionLexer.ROUNDTOKEN)
        {
            MemCell number = expressions.get(0).calculateCellValue();
            if (number == null || number.getNumberValue() == null) return new MemCell(FormulaError.NA);
            MemCell digits = expressions.get(1).calculateCellValue();
            if (digits == null || digits.getNumberValue() == null) return new MemCell(FormulaError.NA);
            double num = number.getNumberValue().doubleValue();
            double signum = Math.signum(num);
            num = Math.abs(num);
            double dig = digits.getNumberValue().doubleValue();
            double result = Math.round(num * Math.pow(10, dig)) / Math.pow(10, dig);
            result *= signum;
            return getReturn(new MemCell(result));
        } else if (type == HyperCellExpressionLexer.ROUNDUPTOKEN)
        {
            MemCell number = expressions.get(0).calculateCellValue();
            if (number == null || number.getNumberValue() == null) return new MemCell(FormulaError.NA);
            MemCell digits = expressions.get(1).calculateCellValue();
            if (digits == null || digits.getNumberValue() == null) return new MemCell(FormulaError.NA);
            double num = number.getNumberValue().doubleValue();
            double signum = Math.signum(num);
            num = Math.abs(num);
            double dig = digits.getNumberValue().doubleValue();
            double result = Math.ceil(num * Math.pow(10, dig)) / Math.pow(10, dig);
            result *= signum;
            return getReturn(new MemCell(result));
        } else if (type == HyperCellExpressionLexer.ROUNDDOWNTOKEN)
        {
            MemCell number = expressions.get(0).calculateCellValue();
            if (number == null || number.getNumberValue() == null) return new MemCell(FormulaError.NA);
            MemCell digits = expressions.get(1).calculateCellValue();
            if (digits == null || digits.getNumberValue() == null) return new MemCell(FormulaError.NA);
            double num = number.getNumberValue().doubleValue();
            double signum = Math.signum(num);
            num = Math.abs(num);
            double dig = digits.getNumberValue().doubleValue();
            double result = Math.floor(num * Math.pow(10, dig)) / Math.pow(10, dig);
            result *= signum;
            return getReturn(new MemCell(result));
        } else if (type == HyperCellExpressionLexer.RANDBETWEEN)
        {
            MemCell bottom = expressions.get(0).calculateCellValue();
            if (bottom == null || bottom.getNumberValue() == null) return new MemCell(FormulaError.NA);
            MemCell top = expressions.get(1).calculateCellValue();
            if (top == null || top.getNumberValue() == null) return new MemCell(FormulaError.NA);
            int bottomInt = bottom.getNumberValue().intValue();
            int topInt = top.getNumberValue().intValue();
            int value = (int) (Math.random() * (topInt + 1 - bottomInt) + bottomInt);
            return getReturn(new MemCell(value));
        } else if (type == HyperCellExpressionLexer.TRUNCTOKEN)
        {
            MemCell number = expressions.getFirst().calculateCellValue();
            if (number == null || number.getNumberValue() == null) return new MemCell(FormulaError.NA);
            double dig = 0;
            if (expressions.size() > 1)
            {
                MemCell digits = expressions.get(1).calculateCellValue();
                if (digits == null || digits.getNumberValue() == null) return new MemCell(FormulaError.NA);
                dig = digits.getNumberValue().doubleValue();
            }
            double num = number.getNumberValue().doubleValue();
            double signum = Math.signum(num);
            num = Math.abs(num);
            num = Math.floor(num * Math.pow(10, dig)) / Math.pow(10, dig);
            return getReturn(new MemCell(num * signum));
        }
        return getReturn(new MemCell(FormulaError.NA));
    }

    private void populateIfNecessary(MemCell mc)
    {
        if (mc == null) return;
        var mcSheet = mc.getSheet();
        if (mcSheet != null && mcSheet.isQuerySheet() && !mcSheet.isQuerySheetUpdated() && mcSheet.getWorkbook()
                                                                                                  .isRefreshQueryDataOnUse())
        {
            // Populate data if necessary
            CalculatedSourceWorkbook csw = new CalculatedSourceWorkbook(null, null, mcSheet.getWorkbook());
            var queries = csw.getQueries(false, false);
            for (var iq : queries)
            {
                if (iq.sheetName.startsWith(mcSheet.getName()))
                {
                    csw.refreshInputQuery(iq);
                    mcSheet.setQuerySheetUpdated(true);
                }
            }
        }
    }

    private MemCell getReturn(MemCell result)
    {
        if (memCellCalculationCache != null)
        {
            memCellCalculationCache.cacheValue(result);
        }
        return result;

    }

    private boolean canPushDown()
    {
        if (type == HyperCellExpressionLexer.COUNTIFSTOKEN || type == HyperCellExpressionLexer.SUMIFSTOKEN || type == HyperCellExpressionLexer.AVERAGEIFTOKEN || type == HyperCellExpressionLexer.AVERAGEIFSTOKEN)
        {

        }
        return false;
    }

    private MemCell pushDown()
    {
        return null;
    }
}
