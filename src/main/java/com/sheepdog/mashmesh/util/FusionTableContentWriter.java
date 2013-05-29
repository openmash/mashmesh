package com.sheepdog.mashmesh.util;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.fusiontables.model.Column;
import com.google.api.services.fusiontables.model.Table;
import com.google.appengine.api.datastore.GeoPt;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class FusionTableContentWriter {
    private StringWriter stringWriter = new StringWriter();
    private CSVWriter csvWriter = new CSVWriter(stringWriter);

    public FusionTableContentWriter(Table table) {
        writeHeader(table);
    }

    private static String serializeObjectForFusionTables(Object object) {
        if (object instanceof GeoPt) {
            GeoPt geoPt = (GeoPt) object;
            return String.format("%f %f", geoPt.getLatitude(), geoPt.getLongitude());
        } else if (object instanceof DateTime) {
            DateTime dateTime = (DateTime) object;
            DateTimeFormatter iso8601Formatter = ISODateTimeFormat.dateTimeNoMillis();
            return iso8601Formatter.print(dateTime);
        } else {
            return object.toString();
        }
    }

    private void writeHeader(Table table) {
        List<String> columnNames = new ArrayList<String>();

        for (Column column : table.getColumns()) {
            columnNames.add(column.getName());
        }

        String[] columnNameArray = columnNames.toArray(new String[columnNames.size()]);
        csvWriter.writeNext(columnNameArray);
    }

    public void writeRecord(Object... objects) {
        String[] fields = new String[objects.length];

        for (int i = 0; i < objects.length; i++) {
            fields[i] = serializeObjectForFusionTables(objects[i]);
        }

        csvWriter.writeNext(fields);
    }

    public AbstractInputStreamContent getInputStreamContent() {
        InputStream inputStream = new ByteArrayInputStream(stringWriter.toString().getBytes());
        return new InputStreamContent("application/octet-stream", inputStream);
    }
}