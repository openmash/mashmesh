package com.sheepdog.mashmesh;

import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalSearchServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.Objectify;
import com.sheepdog.mashmesh.models.*;
import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertNull;

public class VolunteerLocatorTest {
    private final LocalServiceTestHelper localServiceTestHelper =
            new LocalServiceTestHelper(
                    new LocalDatastoreServiceTestConfig(),
                    new LocalMemcacheServiceTestConfig(),
                    new LocalSearchServiceTestConfig());

    private static final DateTimeFormatter iso8601Formatter = ISODateTimeFormat.dateTimeNoMillis().withOffsetParsed();

    @Before
    public void setUp() {
        localServiceTestHelper.setUp();
    }

    @After
    public void tearDown() {
        localServiceTestHelper.tearDown();
    }

    private UserProfile createProfile(String userId, GeoPt location) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(userId);
        userProfile.setLocation(location);
        OfyService.ofy().put(userProfile);
        return userProfile;
    }

    @Test
    public void testAvailabilityWithHole() {
        Objectify ofy = OfyService.ofy();

        UserProfile patientProfile = createProfile("patient-1", TestConstants.EAST_BAYSHORE_EPA_GEOPT);
        UserProfile volunteerUserProfile = createProfile("volunteer-1", TestConstants.UNIVERSITY_AVENUE_PA_GEOPT);

        VolunteerProfile volunteerProfile = new VolunteerProfile();
        volunteerProfile.setUserId("volunteer-1");

        // Available 10 AM - 1 PM, 2 PM - 6 PM on Wednesdays
        AvailableTimePeriod period1 = new AvailableTimePeriod();
        period1.setDay(3);
        period1.setStartTime(new LocalTime(10, 0));
        period1.setEndTime(new LocalTime(13, 0));

        AvailableTimePeriod period2 = new AvailableTimePeriod();
        period2.setDay(3);
        period2.setStartTime(new LocalTime(14, 0));
        period2.setEndTime(new LocalTime(18, 0));

        volunteerProfile.setAvailableTimePeriods(Arrays.asList(period1, period2));
        volunteerProfile.setLocation(TestConstants.UNIVERSITY_AVENUE_PA_GEOPT);

        ofy.put(volunteerProfile);

        DateTime appointmentTime = iso8601Formatter.parseDateTime("2013-06-05T13:35:00-07:00");
        RideRequest rideRequest = new RideRequest();
        rideRequest.setPatientUserProfileKey(patientProfile.getKey());
        rideRequest.setAppointmentLocation(TestConstants.PALO_ALTO_MEDICAL_FOUNDATION_GEOPT);
        rideRequest.setAppointmentTime(appointmentTime);
        ofy.put(rideRequest);

        VolunteerLocator volunteerLocator = new VolunteerLocator(rideRequest);
        assertNull(volunteerLocator.getEligibleVolunteer());
    }
}
