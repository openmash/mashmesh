package com.sheepdog.mashmesh.models;

import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.conv.Converter;
import com.googlecode.objectify.impl.conv.ConverterLoadContext;
import com.googlecode.objectify.impl.conv.ConverterSaveContext;
import org.joda.time.LocalTime;
import org.joda.time.ReadableInstant;
import org.joda.time.base.AbstractInstant;

import java.lang.reflect.Constructor;

class OfyJodaLocalTimeConverter implements Converter {
    @Override
    public Object forDatastore(Object value, ConverterSaveContext ctx) {
        if (!(value instanceof LocalTime)) {
            return null;
        }

        LocalTime localTime = (LocalTime) value;
        return localTime.toString();
    }

    @Override
    public Object forPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx, Object onPojo) {
        if (!LocalTime.class.isAssignableFrom(fieldType)) {
            return null;
        }

        return LocalTime.parse((String) value);
    }
}
