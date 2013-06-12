package com.sheepdog.mashmesh.integration;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.google.appengine.api.mail.MailServicePb;
import com.google.appengine.api.users.User;
import com.meterware.httpunit.*;
import com.meterware.servletunit.ServletUnitClient;
import com.sheepdog.mashmesh.TestLocationConstants;
import com.sheepdog.mashmesh.models.OfyService;
import com.sheepdog.mashmesh.models.RideRecord;
import com.sheepdog.mashmesh.models.RideRequest;
import com.sheepdog.mashmesh.models.UserProfile;
import com.sheepdog.mashmesh.polyline.Point;
import com.sheepdog.mashmesh.polyline.PolylineDecoder;
import com.sheepdog.mashmesh.tasks.SendNotificationTask;
import com.sheepdog.mashmesh.util.CollectionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class MatchWithOneVolunteerTest {
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
    public void testMatchWithOneVolunteerUnavailable() throws IOException, SAXException {
        // 1. Sign up a patient and a volunteer
        integrationTestHelper.signUpPatient(IntegrationTestConstants.PATIENT_1);
        integrationTestHelper.signUpVolunteer(IntegrationTestConstants.VOLUNTEER_1);

        integrationTestHelper.setNotLoggedIn();

        ServletUnitClient client = integrationTestHelper.getClient();

        // 2. Trigger the appointment notification endpoint
        WebRequest request = new PostMethodWebRequest("http://localhost/resources/notification");
        request.setParameter("patientEmail", IntegrationTestConstants.PATIENT_1.getEmail());
        request.setParameter("appointmentAddress", TestLocationConstants.PALO_ALTO_MEDICAL_FOUNDATION_ADDRESS);
        request.setParameter("appointmentTime", IntegrationTestConstants.VOLUNTEER_1_UNAVAILABLE_TIME);

        WebResponse response = client.getResponse(request);
        assertEquals(200, response.getResponseCode());

        // 3. Make sure that a task was dispatched to the task queue
        assertEquals(1, integrationTestHelper.getTaskCount(SendNotificationTask.QUEUE_NAME));
        assertTrue(integrationTestHelper.waitForTask());

        // 4. Make sure that we sent out a "no pickup available" notification
        MailServicePb.MailMessage sentPatientMessage = integrationTestHelper.popNextEmailMessage();
        assertTrue(sentPatientMessage.getSubject().startsWith("No Pickup Available"));
        assertEquals(Collections.singletonList(IntegrationTestConstants.PATIENT_1.getEmail()),
                sentPatientMessage.tos());

        // 5. Make sure that no exportable ride record was generated
        assertFalse(RideRecord.getExportableRecords().iterator().hasNext());

        // 6. Make sure the ride request was deleted
        assertEquals(0, OfyService.ofy().query(RideRequest.class).count());
    }

    @Test
    public void testMatchWithPatientTooFarAway() throws IOException, SAXException {
        // 1. Sign up a patient and a volunteer, with the patient further away than the volunteer's
        //    maximum distance
        integrationTestHelper.signUpPatient(IntegrationTestConstants.DISTANT_PATIENT);
        integrationTestHelper.signUpVolunteer(IntegrationTestConstants.VOLUNTEER_1);

        // 2. Trigger the notification endpoint for the patient
        integrationTestHelper.runNotification(IntegrationTestConstants.DISTANT_PATIENT,
                TestLocationConstants.PALO_ALTO_MEDICAL_FOUNDATION_ADDRESS,
                IntegrationTestConstants.VOLUNTEER_1_AVAILABLE_TIME);

        // 3. Make sure that a "no pickup available" message was sent
        MailServicePb.MailMessage patientMessage = integrationTestHelper.popNextEmailMessage();
        assertTrue(patientMessage.getSubject().startsWith("No Pickup Available"));
        assertEquals(Collections.singletonList(IntegrationTestConstants.DISTANT_PATIENT.getEmail()),
                patientMessage.tos());
    }

    @Test
    public void testMatchWithOneVolunteerAccepted() throws IOException, SAXException, URISyntaxException {
        // 1. Sign up a patient and a volunteer
        User patientUser = integrationTestHelper.signUpPatient(IntegrationTestConstants.PATIENT_1);
        User volunteerUser = integrationTestHelper.signUpVolunteer(IntegrationTestConstants.VOLUNTEER_1);

        integrationTestHelper.setNotLoggedIn();

        ServletUnitClient client = integrationTestHelper.getClient();

        // 2. Trigger the appointment notification endpoint
        WebRequest request = new PostMethodWebRequest("http://localhost/resources/notification");
        request.setParameter("patientEmail", IntegrationTestConstants.PATIENT_1.getEmail());
        request.setParameter("appointmentAddress", TestLocationConstants.PALO_ALTO_MEDICAL_FOUNDATION_ADDRESS);
        request.setParameter("appointmentTime", IntegrationTestConstants.VOLUNTEER_1_AVAILABLE_TIME);

        WebResponse response = client.getResponse(request);
        assertEquals(200, response.getResponseCode());

        // 3. Make sure that a task was dispatched to the task queue.
        assertEquals(1, integrationTestHelper.getTaskCount(SendNotificationTask.QUEUE_NAME));
        assertTrue(integrationTestHelper.waitForTask());

        // 4. Make sure that we sent out a pickup request
        MailServicePb.MailMessage sentVolunteerMessage = integrationTestHelper.popNextEmailMessage();

        assertTrue(sentVolunteerMessage.getSubject().startsWith("Appointment Pickup"));
        assertEquals(Collections.singletonList(IntegrationTestConstants.VOLUNTEER_1.getEmail()),
                sentVolunteerMessage.tos());

        IntegrationTestHelper.HtmlClientWrapper wrapper =
                integrationTestHelper.getHtmlClientForString("http://localhost/mail", sentVolunteerMessage.getHtmlBody());
        String acceptLink = wrapper.getPage().getAnchorByText("Accept").getHrefAttribute();

        // 5. Make sure that the map link is valid
        HtmlElement mapImageElement = wrapper.getPage().getElementsByTagName("img").get(0);
        String mapImageSrc = mapImageElement.getAttribute("src");
        List<String> mapPathParams = integrationTestHelper.getQueryStringParameter(mapImageSrc, "path");

        assertEquals(1, mapPathParams.size());

        String[] mapPathAttributes = mapPathParams.get(0).split(":");
        String polyline = mapPathAttributes[mapPathAttributes.length - 1];
        List<Point> points =  new PolylineDecoder(polyline).getPoints();
        assertTrue(points.size() > 0);

        // 6. Accept the pickup request sent in the email
        integrationTestHelper.setLoggedInUser(IntegrationTestConstants.VOLUNTEER_1);
        WebResponse acceptPage = client.getResponse(acceptLink);
        assertEquals("Pickup Accepted", acceptPage.getElementsByTagName("h1")[0].getText());

        // 7. Make sure that we sent a notification to the patient.
        MailServicePb.MailMessage sentPatientMessage = integrationTestHelper.popNextEmailMessage();
        assertTrue(sentPatientMessage.getSubject().startsWith("Appointment Pickup"));
        assertEquals(Collections.singletonList(IntegrationTestConstants.PATIENT_1.getEmail()),
                sentPatientMessage.tos());

        // 8. Make sure that an exportable ride record was generated
        List<RideRecord> rideRecords = CollectionUtils.listOfIterator(RideRecord.getExportableRecords().iterator());
        assertEquals(1, rideRecords.size());

        RideRecord rideRecord = rideRecords.get(0);
        assertEquals(UserProfile.get(patientUser).getKey(), rideRecord.getPatientProfileKey());
        assertEquals(UserProfile.get(volunteerUser).getKey(), rideRecord.getVolunteerUserProfileKey());

        // 9. Make sure the ride request was deleted
        assertEquals(0, OfyService.ofy().query(RideRequest.class).count());
    }

    @Test
    public void testMatchWithOneVolunteerDeclined() throws IOException, SAXException {
        // 1. Sign up a patient and a volunteer
        integrationTestHelper.signUpPatient(IntegrationTestConstants.PATIENT_1);
        integrationTestHelper.signUpVolunteer(IntegrationTestConstants.VOLUNTEER_1);

        integrationTestHelper.setNotLoggedIn();

        // 2. Trigger the notification endpoint
        integrationTestHelper.runNotification(IntegrationTestConstants.PATIENT_1,
                TestLocationConstants.PALO_ALTO_MEDICAL_FOUNDATION_ADDRESS,
                IntegrationTestConstants.VOLUNTEER_1_AVAILABLE_TIME);

        // 3. Decline the appointment pickup request
        WebResponse declinePage = integrationTestHelper.clickNextEmailLink(
                IntegrationTestConstants.VOLUNTEER_1, "Decline");
        assertEquals("Pickup Declined", declinePage.getElementsByTagName("h1")[0].getText());
        assertTrue(integrationTestHelper.waitForTask());

        // 4. Make sure that we send out a pickup failed notification
        MailServicePb.MailMessage sentPatientMessage = integrationTestHelper.popNextEmailMessage();
        assertTrue(sentPatientMessage.getSubject().startsWith("No Pickup Available"));
        assertEquals(Collections.singletonList(IntegrationTestConstants.PATIENT_1.getEmail()),
                sentPatientMessage.tos());

        // 5. Make sure that no exportable ride record was generated
        assertFalse(RideRecord.getExportableRecords().iterator().hasNext());

        // 6. Make sure the ride request was deleted
        assertEquals(0, OfyService.ofy().query(RideRequest.class).count());
    }

    @Test
    public void testMatchWithOneVolunteerBusy() throws IOException, SAXException {
        // 1. Sign up two patients and a volunteer
        integrationTestHelper.signUpPatient(IntegrationTestConstants.PATIENT_1);
        integrationTestHelper.signUpPatient(IntegrationTestConstants.PATIENT_2);
        integrationTestHelper.signUpVolunteer(IntegrationTestConstants.VOLUNTEER_1);

        // 2. Trigger the notification endpoint for the second patient
        integrationTestHelper.runNotification(IntegrationTestConstants.PATIENT_2,
                TestLocationConstants.PALO_ALTO_MEDICAL_FOUNDATION_ADDRESS,
                IntegrationTestConstants.VOLUNTEER_1_AVAILABLE_TIME);

        // 3. Accept the appointment pickup request
        WebResponse acceptPage = integrationTestHelper.clickNextEmailLink(
                IntegrationTestConstants.VOLUNTEER_1, "Accept");
        assertEquals(200, acceptPage.getResponseCode());

        // 4. Skip the notification email to the patient
        integrationTestHelper.popNextEmailMessage();

        // 5. Trigger the notification endpoint for the first patient
        integrationTestHelper.runNotification(IntegrationTestConstants.PATIENT_1,
                TestLocationConstants.PALO_ALTO_MEDICAL_FOUNDATION_ADDRESS,
                IntegrationTestConstants.VOLUNTEER_1_AVAILABLE_TIME);

        // 6. Make sure that the a "no pickup available" message was sent.
        MailServicePb.MailMessage sentPatient1Message = integrationTestHelper.popNextEmailMessage();
        assertTrue(sentPatient1Message.getSubject().startsWith("No Pickup Available"));
        assertEquals(Collections.singletonList(IntegrationTestConstants.PATIENT_1.getEmail()),
                sentPatient1Message.tos());
    }
}
