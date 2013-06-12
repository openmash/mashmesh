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
