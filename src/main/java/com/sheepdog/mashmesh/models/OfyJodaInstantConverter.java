/**
 *    Copyright 2013 Talend Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
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
