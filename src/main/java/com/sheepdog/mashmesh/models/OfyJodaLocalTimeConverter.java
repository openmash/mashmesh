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
