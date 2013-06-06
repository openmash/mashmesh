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
