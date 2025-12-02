package scoop.timeseries;

import io.hypercell.core.grid.MemWorkbook;
import scoop.metric.CalendarType;
import io.hypercell.core.datatable.TimeSeriesPeriod;
import java.util.Date;

public class TimeSeries extends MemWorkbook {
    public Date getDate(int row) { return new Date(); }
    public TimeSeriesPeriod getPeriod() { return TimeSeriesPeriod.Daily; }
    public CalendarType getCalendar() { return CalendarType.Calendar; }
    public io.hypercell.core.datatable.ColumnMetadata[] getColumnMetadata() { return null; } // Stub return type
}
