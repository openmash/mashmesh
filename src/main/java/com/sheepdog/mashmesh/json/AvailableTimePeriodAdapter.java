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
package com.sheepdog.mashmesh.json;

import com.google.gson.*;
import com.sheepdog.mashmesh.models.AvailableTimePeriod;
import org.joda.time.LocalTime;

import java.lang.reflect.Type;

public class AvailableTimePeriodAdapter implements JsonSerializer<AvailableTimePeriod>,
        JsonDeserializer<AvailableTimePeriod> {
    @Override
    public AvailableTimePeriod deserialize(JsonElement element, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject object = element.getAsJsonObject();
        AvailableTimePeriod period = new AvailableTimePeriod();
        period.setDay(object.get("dayId").getAsInt());
        period.setStartTime(new LocalTime(object.get("startHour").getAsInt(), 0));
        period.setEndTime(new LocalTime(object.get("endHour").getAsInt(), 0));
        return period;
    }

    @Override
    public JsonElement serialize(AvailableTimePeriod period, Type type, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.addProperty("dayId", period.getDay());
        object.addProperty("startHour", period.getStartTime().getHourOfDay());
        object.addProperty("endHour", period.getEndTime().getHourOfDay());
        return object;
    }
}
