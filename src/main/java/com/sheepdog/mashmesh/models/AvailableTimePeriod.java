package com.sheepdog.mashmesh.models;

import com.googlecode.objectify.annotation.Unindexed;
import org.joda.time.LocalTime;

@Unindexed
public class AvailableTimePeriod {
    private int day; // Monday = 1, Sunday = 7
    private LocalTime startTime;
    private LocalTime endTime;

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
}
