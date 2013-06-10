package com.sheepdog.mashmesh.models;

import com.googlecode.objectify.annotation.Unindexed;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.joda.time.LocalTime;

@Unindexed
public class AvailableTimePeriod {
    private int day; // Monday = 1, Sunday = 7
    private LocalTime startTime;
    private LocalTime endTime;

    public int getDay() {
        return day;
    }

    public AvailableTimePeriod setDay(int day) {
        this.day = day;
        return this;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public AvailableTimePeriod setStartTime(LocalTime startTime) {
        this.startTime = startTime;
        return this;
    }

    public AvailableTimePeriod setStartTime(int hour) {
        return this.setStartTime(new LocalTime(hour, 0));
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public AvailableTimePeriod setEndTime(LocalTime endTime) {
        this.endTime = endTime;
        return this;
    }

    public AvailableTimePeriod setEndTime(int hour) {
        return this.setEndTime(new LocalTime(hour, 0));
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AvailableTimePeriod)) {
            return false;
        }

        AvailableTimePeriod period = (AvailableTimePeriod) other;

        return getDay() == period.getDay() &&
               getStartTime().equals(period.getStartTime()) &&
               getEndTime().equals(period.getEndTime());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getDay())
                .append(getStartTime())
                .append(getEndTime())
                .toHashCode();
    }

    @Override
    public String toString() {
        return String.format("%s: {day %d, %s - %s}", getClass().getSimpleName(),
                getDay(), getStartTime(), getEndTime());
    }
}
