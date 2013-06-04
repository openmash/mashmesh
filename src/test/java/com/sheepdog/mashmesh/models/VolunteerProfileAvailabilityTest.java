package com.sheepdog.mashmesh.models;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalTime;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VolunteerProfileAvailabilityTest {
    private DateTime saturdayAtNoon = DateTime.parse("2013-06-01T12:00:00-04:00");
    private DateTime saturdayAtOneThirtyPm = DateTime.parse("2013-06-01T13:30:00-04:00");

    private DateTime sundayAtElevenPm = DateTime.parse("2013-06-02T23:00:00-04:00");
    private DateTime mondayAtOneAm = DateTime.parse("2013-06-03T01:00:00-04:00");

    @Test
    public void testNoAvailability() {
        VolunteerProfile volunteerProfile = new VolunteerProfile();
        assertFalse(volunteerProfile.isTimeslotAvailable(saturdayAtNoon, saturdayAtOneThirtyPm));
    }

    @Test
    public void testAlwaysAvailable() {
        VolunteerProfile volunteerProfile = new VolunteerProfile();
        volunteerProfile.setAlwaysAvailable();
        assertTrue(volunteerProfile.isTimeslotAvailable(saturdayAtNoon, saturdayAtOneThirtyPm));
    }

    @Test
    public void testDisjointNonAvailability() {
        VolunteerProfile volunteerProfile = new VolunteerProfile();

        AvailableTimePeriod availableTimePeriod = new AvailableTimePeriod();
        availableTimePeriod.setDay(DateTimeConstants.SATURDAY);
        availableTimePeriod.setStartTime(new LocalTime(15, 0));
        availableTimePeriod.setEndTime(new LocalTime(20, 0));
        volunteerProfile.setAvailableTimePeriods(Collections.singletonList(availableTimePeriod));

        assertFalse(volunteerProfile.isTimeslotAvailable(saturdayAtNoon, saturdayAtOneThirtyPm));
    }

    @Test
    public void testOverlappingNonAvailability() {
        VolunteerProfile volunteerProfile = new VolunteerProfile();

        AvailableTimePeriod availableTimePeriod = new AvailableTimePeriod();
        availableTimePeriod.setDay(DateTimeConstants.SATURDAY);
        availableTimePeriod.setStartTime(new LocalTime(13, 0));
        availableTimePeriod.setEndTime(new LocalTime(17, 0));
        volunteerProfile.setAvailableTimePeriods(Collections.singletonList(availableTimePeriod));

        assertFalse(volunteerProfile.isTimeslotAvailable(saturdayAtNoon, saturdayAtOneThirtyPm));
    }

    @Test
    public void testSingleIntervalAvailability() {
        VolunteerProfile volunteerProfile = new VolunteerProfile();

        AvailableTimePeriod availableTimePeriod = new AvailableTimePeriod();
        availableTimePeriod.setDay(DateTimeConstants.SATURDAY);
        availableTimePeriod.setStartTime(new LocalTime(8, 0));
        availableTimePeriod.setEndTime(new LocalTime(18, 0));
        volunteerProfile.setAvailableTimePeriods(Collections.singletonList(availableTimePeriod));

        assertTrue(volunteerProfile.isTimeslotAvailable(saturdayAtNoon, saturdayAtOneThirtyPm));
    }

    @Test
    public void testExactAvailability() {
        VolunteerProfile volunteerProfile = new VolunteerProfile();

        AvailableTimePeriod availableTimePeriod = new AvailableTimePeriod();
        availableTimePeriod.setDay(DateTimeConstants.SATURDAY);
        availableTimePeriod.setStartTime(new LocalTime(12, 0));
        availableTimePeriod.setEndTime(new LocalTime(13, 30));
        volunteerProfile.setAvailableTimePeriods(Collections.singletonList(availableTimePeriod));

        assertTrue(volunteerProfile.isTimeslotAvailable(saturdayAtNoon, saturdayAtOneThirtyPm));
    }

    @Test
    public void testAvailabilityOnAnotherDay() {
        VolunteerProfile volunteerProfile = new VolunteerProfile();

        AvailableTimePeriod availableTimePeriod1 = new AvailableTimePeriod();
        availableTimePeriod1.setDay(DateTimeConstants.FRIDAY);
        availableTimePeriod1.setStartTime(new LocalTime(12, 0));
        availableTimePeriod1.setEndTime(new LocalTime(13, 30));

        AvailableTimePeriod availableTimePeriod2 = new AvailableTimePeriod();
        availableTimePeriod2.setDay(DateTimeConstants.SATURDAY);
        availableTimePeriod2.setStartTime(new LocalTime(12, 30));
        availableTimePeriod2.setStartTime(new LocalTime(16, 11));

        volunteerProfile.setAvailableTimePeriods(Arrays.asList(availableTimePeriod1, availableTimePeriod2));

        assertFalse(volunteerProfile.isTimeslotAvailable(saturdayAtNoon, saturdayAtOneThirtyPm));
    }

    @Test
    public void testAbuttingPeriodAvailability() {
        VolunteerProfile volunteerProfile = new VolunteerProfile();

        AvailableTimePeriod availableTimePeriod1 = new AvailableTimePeriod();
        availableTimePeriod1.setDay(DateTimeConstants.SATURDAY);
        availableTimePeriod1.setStartTime(new LocalTime(9, 0));
        availableTimePeriod1.setEndTime(new LocalTime(13, 0));

        AvailableTimePeriod availableTimePeriod2 = new AvailableTimePeriod();
        availableTimePeriod2.setDay(DateTimeConstants.SATURDAY);
        availableTimePeriod2.setStartTime(new LocalTime(13, 0));
        availableTimePeriod2.setEndTime(new LocalTime(14, 0));

        volunteerProfile.setAvailableTimePeriods(Arrays.asList(availableTimePeriod1, availableTimePeriod2));

        assertTrue(volunteerProfile.isTimeslotAvailable(saturdayAtNoon, saturdayAtOneThirtyPm));
    }

    @Test
    public void testNonAbuttingAvailability() {
        VolunteerProfile volunteerProfile = new VolunteerProfile();

        AvailableTimePeriod availableTimePeriod1 = new AvailableTimePeriod();
        availableTimePeriod1.setDay(DateTimeConstants.SATURDAY);
        availableTimePeriod1.setStartTime(new LocalTime(9, 0));
        availableTimePeriod1.setEndTime(new LocalTime(10, 0));

        AvailableTimePeriod availableTimePeriod2 = new AvailableTimePeriod();
        availableTimePeriod2.setDay(DateTimeConstants.SATURDAY);
        availableTimePeriod2.setStartTime(new LocalTime(11, 0));
        availableTimePeriod2.setEndTime(new LocalTime(14, 0));

        volunteerProfile.setAvailableTimePeriods(Arrays.asList(availableTimePeriod1, availableTimePeriod2));

        assertTrue(volunteerProfile.isTimeslotAvailable(saturdayAtNoon, saturdayAtOneThirtyPm));
    }

    @Test
    public void testNonAbuttingNonAvailability() {
        VolunteerProfile volunteerProfile = new VolunteerProfile();

        AvailableTimePeriod availableTimePeriod1 = new AvailableTimePeriod();
        availableTimePeriod1.setDay(DateTimeConstants.SATURDAY);
        availableTimePeriod1.setStartTime(new LocalTime(9, 0));
        availableTimePeriod1.setEndTime(new LocalTime(12, 20));

        AvailableTimePeriod availableTimePeriod2 = new AvailableTimePeriod();
        availableTimePeriod2.setDay(DateTimeConstants.SATURDAY);
        availableTimePeriod2.setStartTime(new LocalTime(12, 30));
        availableTimePeriod2.setEndTime(new LocalTime(14, 0));

        volunteerProfile.setAvailableTimePeriods(Arrays.asList(availableTimePeriod1, availableTimePeriod2));

        assertFalse(volunteerProfile.isTimeslotAvailable(saturdayAtNoon, saturdayAtOneThirtyPm));
    }

    @Test
    public void testOvernightAvailability() {
        VolunteerProfile volunteerProfile = new VolunteerProfile();

        AvailableTimePeriod availableTimePeriod = new AvailableTimePeriod();
        availableTimePeriod.setDay(DateTimeConstants.SUNDAY);
        availableTimePeriod.setStartTime(new LocalTime(22, 0));
        availableTimePeriod.setEndTime(new LocalTime(3, 0));

        volunteerProfile.setAvailableTimePeriods(Collections.singletonList(availableTimePeriod));

        assertTrue(volunteerProfile.isTimeslotAvailable(sundayAtElevenPm, mondayAtOneAm));
    }

    @Test
    public void testAbuttingOvernightAvailability() {
        VolunteerProfile volunteerProfile = new VolunteerProfile();

        AvailableTimePeriod availableTimePeriod1 = new AvailableTimePeriod();
        availableTimePeriod1.setDay(DateTimeConstants.SUNDAY);
        availableTimePeriod1.setStartTime(new LocalTime(22, 0));
        availableTimePeriod1.setEndTime(new LocalTime(0, 0));

        AvailableTimePeriod availableTimePeriod2 = new AvailableTimePeriod();
        availableTimePeriod2.setDay(DateTimeConstants.MONDAY);
        availableTimePeriod2.setStartTime(new LocalTime(0, 0));
        availableTimePeriod2.setEndTime(new LocalTime(2, 0));

        volunteerProfile.setAvailableTimePeriods(Arrays.asList(availableTimePeriod1, availableTimePeriod2));

        assertTrue(volunteerProfile.isTimeslotAvailable(sundayAtElevenPm, mondayAtOneAm));
    }
}
