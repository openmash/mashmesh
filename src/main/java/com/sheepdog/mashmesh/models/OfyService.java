package com.sheepdog.mashmesh.models;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.conv.Converter;
import com.googlecode.objectify.impl.conv.ConverterLoadContext;
import com.googlecode.objectify.impl.conv.ConverterSaveContext;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.ReadableInstant;
import org.joda.time.base.AbstractInstant;

import java.lang.reflect.Constructor;

public class OfyService {
    static {
        factory().register(UserProfile.class);
        factory().register(VolunteerProfile.class);
        factory().register(RideRecord.class);

        // TODO: Pull this out
        factory().getConversions().add(new Converter() {
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
        });
    }

    public static Objectify ofy() {
        return ObjectifyService.begin();
    }

    public static ObjectifyFactory factory() {
        return ObjectifyService.factory();
    }
}