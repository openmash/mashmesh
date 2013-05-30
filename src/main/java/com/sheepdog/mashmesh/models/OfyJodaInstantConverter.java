package com.sheepdog.mashmesh.models;

import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.conv.Converter;
import com.googlecode.objectify.impl.conv.ConverterLoadContext;
import com.googlecode.objectify.impl.conv.ConverterSaveContext;
import org.joda.time.ReadableInstant;
import org.joda.time.base.AbstractInstant;

import java.lang.reflect.Constructor;

class OfyJodaInstantConverter implements Converter {
    @Override
    public Object forDatastore(Object value, ConverterSaveContext ctx) {
        if (!(value instanceof ReadableInstant)) {
            return null;
        }

        ReadableInstant readableInstant = (ReadableInstant) value;
        return readableInstant.toInstant().toDate();
    }

    @Override
    public Object forPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx, Object onPojo) {
        if (!AbstractInstant.class.isAssignableFrom(fieldType)) {
            return null;
        }

        Constructor<?> ctor = TypeUtils.getConstructor(fieldType, Object.class);
        return TypeUtils.newInstance(ctor, value);
    }
}
