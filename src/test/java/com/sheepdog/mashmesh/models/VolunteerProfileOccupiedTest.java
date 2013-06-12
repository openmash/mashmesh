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

import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.Objectify;
import com.sheepdog.mashmesh.util.CollectionUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class VolunteerProfileOccupiedTest {
    private final LocalServiceTestHelper localServiceTestHelper =
            new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
    private final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis().withOffsetParsed();

    private RideRequest getRideRequest() {
        RideRequest rideRequest = new RideRequest();
        rideRequest.setId(1L);
        return rideRequest;
    }

    private DateTime dateTime(String iso8601DateTimeString) {
        return dateTimeFormatter.parseDateTime(iso8601DateTimeString);
    }

    @Test
    public void testFindingVolunteerWithOldAppointment() {
        localServiceTestHelper.setUp();

        VolunteerProfile volunteerProfile = new VolunteerProfile();
        volunteerProfile.setUserId("1");

        DateTime startTime = dateTime("2013-06-01T20:00:00-03:00");
        DateTime endTime = dateTime("2013-06-01T20:30:00-03:00");
        DateTime cutoffTime = dateTime("2013-06-02T00:00:00-03:00");

        volunteerProfile.addAppointmentTime(getRideRequest(), startTime, endTime);
        Objectify ofy = OfyService.ofy();
        ofy.put(volunteerProfile);

        QueryResultIterator<VolunteerProfile> volunteerProfileIterator = VolunteerProfile
                .withExpiredAppointments(cutoffTime.getMillis(), 100);
        List<VolunteerProfile> volunteerProfiles = CollectionUtils.listOfIterator(volunteerProfileIterator);

        assertEquals(Arrays.asList(volunteerProfile), volunteerProfiles);

        localServiceTestHelper.tearDown();
    }

    @Test
    public void testFindingVolunteerWithoutOldAppointments() {
        localServiceTestHelper.setUp();

        VolunteerProfile volunteerProfile = new VolunteerProfile();
        volunteerProfile.setUserId("1");

        DateTime startTime = dateTime("2013-06-01T20:00:00-03:00");
        DateTime endTime = dateTime("2013-06-01T20:30:00-03:00");
        DateTime cutoffTime = dateTime("2013-05-30T00:00:00-03:00");

        volunteerProfile.addAppointmentTime(getRideRequest(), startTime, endTime);
        Objectify ofy = OfyService.ofy();
        ofy.put(volunteerProfile);

        QueryResultIterator<VolunteerProfile> volunteerProfileIterator = VolunteerProfile
                .withExpiredAppointments(cutoffTime.getMillis(), 100);
        List<VolunteerProfile> volunteerProfiles = CollectionUtils.listOfIterator(volunteerProfileIterator);

        assertTrue(volunteerProfiles.isEmpty());

        localServiceTestHelper.tearDown();
    }

    @Test
    public void testMatchingAppointmentIsOccupied() {
        VolunteerProfile volunteerProfile = new VolunteerProfile();
        DateTime startTime = dateTime("2013-06-01T20:00:00-03:00");
        DateTime endTime = dateTime("2013-06-01T20:30:00-03:00");

        volunteerProfile.addAppointmentTime(getRideRequest(), startTime, endTime);
        assertTrue(volunteerProfile.isTimeslotOccupied(startTime, endTime));
    }

    @Test
    public void testSmallerAppointmentIsOccupied() {
        VolunteerProfile volunteerProfile = new VolunteerProfile();
        DateTime apptStartTime = dateTime("2013-06-01T20:00:00-03:00");
        DateTime apptEndTime = dateTime("2013-06-01T20:30:00-03:00");
        DateTime requestedStartTime = dateTime("2013-06-01T19:30:00-03:00");
        DateTime requestedEndTime = dateTime("2013-06-01T21:00:00-03:00");

        volunteerProfile.addAppointmentTime(getRideRequest(), apptStartTime, apptEndTime);
        assertTrue(volunteerProfile.isTimeslotOccupied(requestedStartTime, requestedEndTime));
    }

    @Test
    public void testLargerAppointmentIsOccupied() {
        VolunteerProfile volunteerProfile = new VolunteerProfile();
        DateTime apptStartTime = dateTime("2013-06-01T20:00:00-03:00");
        DateTime apptEndTime = dateTime("2013-06-01T20:30:00-03:00");
        DateTime requestedStartTime = dateTime("2013-06-01T20:10:00-03:00");
        DateTime requestedEndTime = dateTime("2013-06-01T20:29:00-03:00");

        volunteerProfile.addAppointmentTime(getRideRequest(), apptStartTime, apptEndTime);
        assertTrue(volunteerProfile.isTimeslotOccupied(requestedStartTime, requestedEndTime));
    }

    @Test
    public void testOverlappingAppointmentIsOccupied() {
        VolunteerProfile volunteerProfile = new VolunteerProfile();
        DateTime apptStartTime = dateTime("2013-06-01T20:10:00-03:00");
        DateTime apptEndTime = dateTime("2013-06-01T20:30:00-03:00");
        DateTime requestedStartTime = dateTime("2013-06-01T20:00:00-03:00");
        DateTime requestedEndTime = dateTime("2013-06-01T20:20:00-03:00");

        volunteerProfile.addAppointmentTime(getRideRequest(), apptStartTime, apptEndTime);
        assertTrue(volunteerProfile.isTimeslotOccupied(requestedStartTime, requestedEndTime));
    }

    @Test
    public void testDisjointAppointmentIsNotOccupied() {
        VolunteerProfile volunteerProfile = new VolunteerProfile();
        DateTime apptStartTime = dateTime("2013-06-01T20:10:00-03:00");
        DateTime apptEndTime = dateTime("2013-06-01T20:30:00-03:00");
        DateTime requestedStartTime = dateTime("2013-06-01T20:30:00-03:00");
        DateTime requestedEndTime = dateTime("2013-06-01T21:20:02-03:00");

        volunteerProfile.addAppointmentTime(getRideRequest(), apptStartTime, apptEndTime);
        assertTrue(volunteerProfile.isTimeslotOccupied(requestedStartTime, requestedEndTime));
    }

    @Test
    public void testScrubbingOneOldAppointment() {
        VolunteerProfile volunteerProfile = new VolunteerProfile();
        DateTime startTime = dateTime("2013-06-01T20:00:00-03:00");
        DateTime endTime = dateTime("2013-06-01T20:30:00-03:00");
        DateTime cutoffTime = dateTime("2013-06-02T00:00:00-03:00");

        volunteerProfile.addAppointmentTime(getRideRequest(), startTime, endTime);
        volunteerProfile.removeExpiredAppointments(cutoffTime.getMillis());

        assertFalse(volunteerProfile.isTimeslotOccupied(startTime, endTime));
    }

    @Test
    public void testScrubbingOneNewAppointment() {
        VolunteerProfile volunteerProfile = new VolunteerProfile();
        DateTime startTime = dateTime("2013-06-01T20:00:00-03:00");
        DateTime endTime = dateTime("2013-06-01T20:30:00-03:00");
        DateTime cutoffTime = dateTime("2013-05-30T00:00:00-03:00");

        volunteerProfile.addAppointmentTime(getRideRequest(), startTime, endTime);
        volunteerProfile.removeExpiredAppointments(cutoffTime.getMillis());

        assertTrue(volunteerProfile.isTimeslotOccupied(startTime, endTime));
    }

    @Test
    public void testScrubbingMultipleAppointments() {
        VolunteerProfile volunteerProfile = new VolunteerProfile();
        DateTime cutoffTime = dateTime("2013-06-02T00:00:00-03:00");

        DateTime startTime1 = dateTime("2013-06-01T20:00:00-03:00");
        DateTime endTime1 = dateTime("2013-06-01T20:30:00-03:00");
        volunteerProfile.addAppointmentTime(getRideRequest(), startTime1, endTime1);

        DateTime startTime2 = dateTime("2013-06-02T20:00:00-03:00");
        DateTime endTime2 = dateTime("2013-06-02T20:30:00-03:00");
        volunteerProfile.addAppointmentTime(getRideRequest(), startTime2, endTime2);

        volunteerProfile.removeExpiredAppointments(cutoffTime.getMillis());

        assertFalse(volunteerProfile.isTimeslotOccupied(startTime1, endTime1));
        assertTrue(volunteerProfile.isTimeslotOccupied(startTime2, endTime2));
    }
}
