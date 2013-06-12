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
