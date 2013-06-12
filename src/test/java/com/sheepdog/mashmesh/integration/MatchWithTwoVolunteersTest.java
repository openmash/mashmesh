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
package com.sheepdog.mashmesh.integration;

import com.google.appengine.api.mail.MailServicePb;
import com.google.appengine.api.users.User;
import com.googlecode.objectify.Key;
import com.meterware.httpunit.WebResponse;
import com.sheepdog.mashmesh.TestLocationConstants;
import com.sheepdog.mashmesh.models.*;
import com.sheepdog.mashmesh.util.CollectionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for the case that an appointment notification is sent for a patient while
 * there are two volunteers in the system. These tests are representative of the
 * behaviour of the system when there are three or more volunteers present as well.
 */
public class MatchWithTwoVolunteersTest {
    private final IntegrationTestHelper integrationTestHelper = new IntegrationTestHelper();

    @Before
    public void setUp() throws Exception {
        integrationTestHelper.setUp();
    }

    @After
    public void tearDown() throws Exception {
        integrationTestHelper.tearDown();
    }

    /**
     * Integration test for the case that an appointment pickup request is accepted
     * by the first volunteer to receive it. The patient should receive a pickup notification,
     * and a record of the pickup should be recorded. No further requests should be sent
     * to volunteers.
     */
    @Test
    public void testFirstVolunteerAcceptsRequest() throws IOException, SAXException {
        // 1. Sign up a patient and two volunteers
        integrationTestHelper.signUpPatient(IntegrationTestConstants.PATIENT_1);
        integrationTestHelper.signUpVolunteer(IntegrationTestConstants.VOLUNTEER_1);
        User volunteer2 = integrationTestHelper.signUpVolunteer(IntegrationTestConstants.VOLUNTEER_2);

        // 2. Send an appointment notification when both volunteers are available
        integrationTestHelper.runNotification(IntegrationTestConstants.PATIENT_1,
                TestLocationConstants.PALO_ALTO_MEDICAL_FOUNDATION_ADDRESS,
                IntegrationTestConstants.VOLUNTEER_1_AND_2_AVAILABLE_TIME);

        // 3. Make sure that volunteer 2 (who is closer) got the request
        MailServicePb.MailMessage sentMessage = integrationTestHelper.popNextEmailMessage();
        assertTrue(sentMessage.getSubject().startsWith("Appointment Pickup"));
        assertEquals(Collections.singletonList(IntegrationTestConstants.VOLUNTEER_2.getEmail()), sentMessage.tos());

        // XXX: The AppEngine mail API stubs don't give addressing info, so check the ride request itself.
        RideRequest rideRequest = OfyService.ofy().query(RideRequest.class).get();
        Key<UserProfile> volunteer2UserProfileKey = Key.create(UserProfile.class, volunteer2.getUserId());
        Key<VolunteerProfile> volunteer2ProfileKey = Key.create(VolunteerProfile.class, volunteer2.getUserId());
        assertEquals(volunteer2ProfileKey, rideRequest.getPendingVolunteerProfileKey());

        // 4. Accept the request to volunteer 2.
        integrationTestHelper.clickEmailLink(IntegrationTestConstants.VOLUNTEER_2.getEmail(), sentMessage, "Accept");

        // 5. Make sure that the confirmation was sent to the patient
        MailServicePb.MailMessage patientMessage = integrationTestHelper.popNextEmailMessage();
        assertTrue(patientMessage.getSubject().startsWith("Appointment Pickup"));
        assertEquals(Collections.singletonList(IntegrationTestConstants.PATIENT_1.getEmail()), patientMessage.tos());

        // 6. Make sure that an exportable ride record was logged for the volunteer
        List<RideRecord> rideRecords = CollectionUtils.listOfIterator(RideRecord.getExportableRecords().iterator());
        assertEquals(1, rideRecords.size());
        assertEquals(volunteer2UserProfileKey, rideRecords.get(0).getVolunteerUserProfileKey());
    }

    /**
     * Integration test for the case that the appointment pickup request is for a time period
     * in which only one of the volunteers is available, and that volunteer declines the request.
     * The other volunteer should not be notified, and the patient should receive a notification
     * that no pickup is available.
     */
    @Test
    public void testOnlyAvailableVolunteerDeclinesRequest() throws IOException, SAXException {
        // 1. Sign up a patient and two volunteers
        integrationTestHelper.signUpPatient(IntegrationTestConstants.PATIENT_1);
        User volunteer1 = integrationTestHelper.signUpVolunteer(IntegrationTestConstants.VOLUNTEER_1);
        integrationTestHelper.signUpVolunteer(IntegrationTestConstants.VOLUNTEER_2);

        // 2. Send an appointment notification when only volunteer 1 is available
        integrationTestHelper.runNotification(IntegrationTestConstants.PATIENT_1,
                TestLocationConstants.PALO_ALTO_MEDICAL_FOUNDATION_ADDRESS,
                IntegrationTestConstants.ONLY_VOLUNTEER_2_UNAVAILABLE_TIME);

        // 3. Make sure that volunteer 1 got the request.
        MailServicePb.MailMessage volunteerMessage = integrationTestHelper.popNextEmailMessage();
        assertTrue(volunteerMessage.getSubject().startsWith("Appointment Pickup"));
        assertEquals(Collections.singletonList(IntegrationTestConstants.VOLUNTEER_1.getEmail()),
                volunteerMessage.tos());

        // XXX: Work around the lack of addressing information in AppEngine's mail test stub
        RideRequest rideRequest = OfyService.ofy().query(RideRequest.class).get();
        Key<VolunteerProfile> volunteer1ProfileKey = Key.create(VolunteerProfile.class, volunteer1.getUserId());
        assertEquals(volunteer1ProfileKey, rideRequest.getPendingVolunteerProfileKey());

        // 4. Accept the request to volunteer 1.
        integrationTestHelper.clickEmailLink(IntegrationTestConstants.VOLUNTEER_1.getEmail(), volunteerMessage, "Decline");
        assertTrue(integrationTestHelper.waitForTask());

        // 5. Make sure that the patient was notified that a pickup is not available.
        MailServicePb.MailMessage patientMessage = integrationTestHelper.popNextEmailMessage();
        assertTrue(patientMessage.getSubject().startsWith("No Pickup Available"));
        assertEquals(Collections.singletonList(IntegrationTestConstants.PATIENT_1.getEmail()), patientMessage.tos());
    }

    /**
     * Integration test for the case that both volunteers are available for the time period
     * indicated by an appointment pickup request, and the first declines, while the second
     * accepts it. The patient should be notified of the pickup, and a record of the pickup
     * should be captured for later export. No other notifications should occur.
     */
    @Test
    public void testSecondVolunteerAcceptsRequest() throws IOException, SAXException {
        // 1. Sign up a patient and two volunteers
        integrationTestHelper.signUpPatient(IntegrationTestConstants.PATIENT_1);
        User volunteer1 = integrationTestHelper.signUpVolunteer(IntegrationTestConstants.VOLUNTEER_1);
         integrationTestHelper.signUpVolunteer(IntegrationTestConstants.VOLUNTEER_2);

        // 2. Send an appointment notification when both volunteers are available.
        integrationTestHelper.runNotification(IntegrationTestConstants.PATIENT_1,
                TestLocationConstants.PALO_ALTO_MEDICAL_FOUNDATION_ADDRESS,
                IntegrationTestConstants.VOLUNTEER_1_AND_2_AVAILABLE_TIME);

        // 3. Decline the request sent to volunteer 2 (who is closer)
        integrationTestHelper.clickNextEmailLink(IntegrationTestConstants.VOLUNTEER_2, "Decline");
        assertTrue(integrationTestHelper.waitForTask());

        // 4. Make sure that the request was sent to volunteer 1 after volunteer 2 declined it.
        MailServicePb.MailMessage volunteerMessage = integrationTestHelper.popNextEmailMessage();
        assertTrue(volunteerMessage.getSubject().startsWith("Appointment Pickup"));
        assertEquals(Collections.singletonList(IntegrationTestConstants.VOLUNTEER_1.getEmail()),
                volunteerMessage.tos());

        RideRequest rideRequest = OfyService.ofy().query(RideRequest.class).get();
        Key<UserProfile> volunteer1UserProfileKey = Key.create(UserProfile.class, volunteer1.getUserId());
        Key<VolunteerProfile> volunteer1ProfileKey = Key.create(VolunteerProfile.class, volunteer1.getUserId());
        assertEquals(volunteer1ProfileKey, rideRequest.getPendingVolunteerProfileKey());

        // 5. Accept the request to volunteer 1.
        WebResponse acceptPage = integrationTestHelper.clickEmailLink(
                IntegrationTestConstants.VOLUNTEER_1.getEmail(), volunteerMessage, "Accept");
        assertEquals(200, acceptPage.getResponseCode());
        assertEquals("Pickup Accepted", acceptPage.getElementsByTagName("h1")[0].getText());

        // 6. Make sure that the patient was notified.
        MailServicePb.MailMessage patientMessage = integrationTestHelper.popNextEmailMessage();
        assertTrue(patientMessage.getSubject().startsWith("Appointment Pickup"));
        assertEquals(Collections.singletonList(IntegrationTestConstants.PATIENT_1.getEmail()), patientMessage.tos());

        // 7. Make sure that the ride request was cleaned up.
        assertEquals(0, OfyService.ofy().query(RideRequest.class).count());

        // 8. Make sure that a ride record was generated for volunteer 1.
        List<RideRecord> rideRecords = CollectionUtils.listOfIterator(RideRecord.getExportableRecords().iterator());
        assertEquals(1, rideRecords.size());
        assertEquals(volunteer1UserProfileKey, rideRecords.get(0).getVolunteerUserProfileKey());
    }

    /**
     * Integration test for the case that both available volunteers decline the appointment
     * request. The patient should be notified that a pickup could not be arranged, and no
     * further requests or notifications should be sent.
     */
    @Test
    public void testBothVolunteersDeclineRequest() throws IOException, SAXException {
        // 1. Sign up a patient and two volunteers
        integrationTestHelper.signUpPatient(IntegrationTestConstants.PATIENT_1);
        integrationTestHelper.signUpVolunteer(IntegrationTestConstants.VOLUNTEER_1);
        integrationTestHelper.signUpVolunteer(IntegrationTestConstants.VOLUNTEER_2);

        // 2. Send an appointment notification when both volunteers are available.
        integrationTestHelper.runNotification(IntegrationTestConstants.PATIENT_1,
                TestLocationConstants.PALO_ALTO_MEDICAL_FOUNDATION_ADDRESS,
                IntegrationTestConstants.VOLUNTEER_1_AND_2_AVAILABLE_TIME);

        // 3. Decline the request sent to volunteer 2 (who is closer)
        integrationTestHelper.clickNextEmailLink(IntegrationTestConstants.VOLUNTEER_2, "Decline");
        assertTrue(integrationTestHelper.waitForTask());

        // 4. Decline the request sent to volunter 1.
        WebResponse declinePage = integrationTestHelper.clickNextEmailLink(
                IntegrationTestConstants.VOLUNTEER_1, "Decline");
        assertEquals(200, declinePage.getResponseCode());
        assertEquals("Pickup Declined", declinePage.getElementsByTagName("h1")[0].getText());

        assertTrue(integrationTestHelper.waitForTask());

        // 5. Make sure that the patient was notified.
        MailServicePb.MailMessage mailMessage = integrationTestHelper.popNextEmailMessage();
        assertTrue(mailMessage.getSubject().startsWith("No Pickup Available"));
        assertEquals(Collections.singletonList(IntegrationTestConstants.PATIENT_1.getEmail()), mailMessage.tos());

        // 6. Make sure that the ride request was cleaned up.
        assertEquals(0, OfyService.ofy().query(RideRequest.class).count());

        // 7. Make sure that no ride record was emitted.
        assertFalse(RideRecord.getExportableRecords().iterator().hasNext());
    }
}
