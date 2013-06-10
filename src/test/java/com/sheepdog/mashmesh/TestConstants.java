package com.sheepdog.mashmesh;

import com.google.appengine.api.datastore.GeoPt;
import com.sheepdog.mashmesh.models.AvailableTimePeriod;
import org.joda.time.DateTimeConstants;

import java.util.Arrays;
import java.util.List;

public class TestConstants {
    public static final String PATIENT_NAME = "John Smith";
    public static final String PATIENT_EMAIL = "patient-1@example.com";

    public static final String VOLUNTEER_NAME = "Diane Fraser";
    public static final String VOLUNTEER_EMAIL = "volunteer-1@example.com";
    public static final String VOLUNTEER_COMMENTS = "I will try to be five minutes early";

    public static final List<AvailableTimePeriod> VOLUNTEER_AVAILABILITY = Arrays.asList(
            new AvailableTimePeriod().setDay(DateTimeConstants.SUNDAY).setStartTime(6).setEndTime(20),
            new AvailableTimePeriod().setDay(DateTimeConstants.TUESDAY).setStartTime(6).setEndTime(10),
            new AvailableTimePeriod().setDay(DateTimeConstants.TUESDAY).setStartTime(13).setEndTime(17),
            new AvailableTimePeriod().setDay(DateTimeConstants.WEDNESDAY).setStartTime(6).setEndTime(9),
            new AvailableTimePeriod().setDay(DateTimeConstants.WEDNESDAY).setStartTime(11).setEndTime(12),
            new AvailableTimePeriod().setDay(DateTimeConstants.WEDNESDAY).setStartTime(13).setEndTime(18),
            new AvailableTimePeriod().setDay(DateTimeConstants.THURSDAY).setStartTime(6).setEndTime(8),
            new AvailableTimePeriod().setDay(DateTimeConstants.SATURDAY).setStartTime(13).setEndTime(20)
    );

    public static final String EAST_BAYSHORE_EPA_ADDRESS = "1760 East Bayshore Road, East Palo Alto, CA";
    public static final GeoPt EAST_BAYSHORE_EPA_GEOPT = new GeoPt(37.4597f, -122.137973f);

    public static final String UNIVERSITY_AVENUE_PA_ADDRESS = "1000 University Avenue, Palo Alto, CA";
    public static final GeoPt UNIVERSITY_AVENUE_PA_GEOPT = new GeoPt(37.444969f, -122.162521f);

    public static final String PALO_ALTO_MEDICAL_FOUNDATION_ADDRESS = "Palo Alto Medical Foundation, Palo Alto, CA";
    public static final GeoPt PALO_ALTO_MEDICAL_FOUNDATION_GEOPT = new GeoPt(37.44012f, -122.161173f);
    public static final String VOLUNTEER_MAXIMUM_DISTANCE = "20";
}
