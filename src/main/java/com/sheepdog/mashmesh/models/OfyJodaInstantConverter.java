package com.sheepdog.mashmesh.models;


import com.googlecode.objectify.impl.conv.Converter;
import com.googlecode.objectify.impl.conv.ConverterLoadContext;
import com.googlecode.objectify.impl.conv.ConverterSaveContext;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

class OfyJodaInstantConverter implements Converter {
    private static final DateTimeFormatter iso8601Formatter = ISODateTimeFormat.dateTimeNoMillis().withOffsetParsed();

    @Override
    public Object forDatastore(Object value, ConverterSaveContext ctx) {
        if (!(value instanceof DateTime)) {
            return null;
        }

        DateTime dateTime = (DateTime) value;
        return iso8601Formatter.print(dateTime);
    }

    @Override
    public Object forPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx, Object onPojo) {
        if (!DateTime.class.isAssignableFrom(fieldType)) {
            return null;
        }

        return iso8601Formatter.parseDateTime((String) value);
    }
}
