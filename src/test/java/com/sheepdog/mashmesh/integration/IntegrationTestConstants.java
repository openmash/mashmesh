package com.sheepdog.mashmesh.integration;

import com.sheepdog.mashmesh.TestLocationConstants;
import com.sheepdog.mashmesh.models.AvailableTimePeriod;
import org.joda.time.DateTimeConstants;

import java.util.Arrays;

public class IntegrationTestConstants {
    public static final PatientConfig PATIENT_1 = new PatientConfig()
            .setName("John Smith")
            .setEmail("john.smith@example.com")
            .setAddress(TestLocationConstants.EAST_BAYSHORE_EPA_ADDRESS);

    public static final PatientConfig PATIENT_2 = new PatientConfig()
            .setName("Chris Travis")
            .setEmail("chris.travis@example.com")
            .setAddress(TestLocationConstants.MENLO_PARK_ADDRESS);

    public static final PatientConfig DISTANT_PATIENT = new PatientConfig()
            .setName("James Gravel")
            .setEmail("james.gravel@example.com")
            .setAddress(TestLocationConstants.OAKLAND_ADDRESS);

    public static final VolunteerConfig VOLUNTEER_1 = new VolunteerConfig()
            .setName("Diane Fraser")
            .setEmail("diane.fraser@gmail.com")
            .setComments("I will try to be five minutes early")
            .setMaximumDistance(20)
            .setAddress(TestLocationConstants.UNIVERSITY_AVENUE_PA_ADDRESS)
            .setAvailableTimePeriods(Arrays.asList(
                    new AvailableTimePeriod().setDay(DateTimeConstants.SUNDAY).setStartTime(6).setEndTime(20),
                    new AvailableTimePeriod().setDay(DateTimeConstants.TUESDAY).setStartTime(6).setEndTime(10),
                    new AvailableTimePeriod().setDay(DateTimeConstants.TUESDAY).setStartTime(13).setEndTime(17),
                    new AvailableTimePeriod().setDay(DateTimeConstants.WEDNESDAY).setStartTime(6).setEndTime(9),
                    new AvailableTimePeriod().setDay(DateTimeConstants.WEDNESDAY).setStartTime(11).setEndTime(12),
                    new AvailableTimePeriod().setDay(DateTimeConstants.WEDNESDAY).setStartTime(13).setEndTime(18),
                    new AvailableTimePeriod().setDay(DateTimeConstants.THURSDAY).setStartTime(6).setEndTime(8),
                    new AvailableTimePeriod().setDay(DateTimeConstants.SATURDAY).setStartTime(13).setEndTime(20)
            ));

    public static final String VOLUNTEER_1_UNAVAILABLE_TIME = "2013-06-06T10:20:00-0700";
    public static final String VOLUNTEER_1_AVAILABLE_TIME = "2013-06-05T16:30:00-0700";
}
