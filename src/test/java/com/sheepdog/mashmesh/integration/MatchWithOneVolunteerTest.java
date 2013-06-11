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
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        integrationTestHelper.signUpPatient(IntegrationTestConstants.patient1Config);
        integrationTestHelper.signUpVolunteer(IntegrationTestConstants.volunteer1Config);

        integrationTestHelper.setNotLoggedIn();

        ServletUnitClient client = integrationTestHelper.getClient();

        // 2. Trigger the appointment notification endpoint
        WebRequest request = new PostMethodWebRequest("http://localhost/resources/notification");
        request.setParameter("patientEmail", IntegrationTestConstants.patient1Config.getEmail());
        request.setParameter("appointmentAddress", TestLocationConstants.PALO_ALTO_MEDICAL_FOUNDATION_ADDRESS);

        // Volunteer 1 is not available at 10:20 AM on Thursday
        request.setParameter("appointmentTime", "2013-06-06T10:20:00-0700");

        WebResponse response = client.getResponse(request);
        assertEquals(200, response.getResponseCode());

        assertEquals(1, integrationTestHelper.getTaskCount(SendNotificationTask.QUEUE_NAME));
        assertTrue(integrationTestHelper.waitForTask());

        // 3. Make sure that we sent out a "no pickup available" notification
        List<MailServicePb.MailMessage> sentMessages = integrationTestHelper.getSentEmailMessages();

        assertEquals(1, sentMessages.size());

        MailServicePb.MailMessage sentPatientMessage = sentMessages.get(0);
        assertTrue(sentPatientMessage.getSubject().startsWith("No Pickup Available"));

        // 4. Make sure that no exportable ride record was generated
        assertTrue(!RideRecord.getExportableRecords().iterator().hasNext());

        // 5. Make sure the ride request was deleted
        assertEquals(0, OfyService.ofy().query(RideRequest.class).count());
    }

    @Test
    public void testMatchWithOneVolunteerAccepted() throws IOException, SAXException, URISyntaxException {
        // 1. Sign up a patient and a volunteer
        User patientUser = integrationTestHelper.signUpPatient(IntegrationTestConstants.patient1Config);
        User volunteerUser = integrationTestHelper.signUpVolunteer(IntegrationTestConstants.volunteer1Config);

        integrationTestHelper.setNotLoggedIn();

        ServletUnitClient client = integrationTestHelper.getClient();

        // 2. Trigger the appointment notification endpoint
        WebRequest request = new PostMethodWebRequest("http://localhost/resources/notification");
        request.setParameter("patientEmail", IntegrationTestConstants.patient1Config.getEmail());
        request.setParameter("appointmentAddress", TestLocationConstants.PALO_ALTO_MEDICAL_FOUNDATION_ADDRESS);

        // Volunteer 1 is available at 4:30 PM on Wednesday
        request.setParameter("appointmentTime", "2013-06-05T16:30:00-0700");

        WebResponse response = client.getResponse(request);
        assertEquals(200, response.getResponseCode());

        assertEquals(1, integrationTestHelper.getTaskCount(SendNotificationTask.QUEUE_NAME));
        assertTrue(integrationTestHelper.waitForTask());

        // 3. Make sure that we sent out a pickup request
        List<MailServicePb.MailMessage> sentMessages = integrationTestHelper.getSentEmailMessages();

        assertEquals(1, sentMessages.size());
        MailServicePb.MailMessage sentVolunteerMessage = sentMessages.get(0);

        assertTrue(sentVolunteerMessage.getSubject().startsWith("Appointment Pickup"));

        IntegrationTestHelper.HtmlClientWrapper wrapper =
                integrationTestHelper.getHtmlClientForString("http://localhost/mail", sentVolunteerMessage.getHtmlBody());
        String acceptLink = wrapper.getPage().getAnchorByText("Accept").getHrefAttribute();

        // 4. Make sure that the map link is valid
        HtmlElement mapImageElement = wrapper.getPage().getElementsByTagName("img").get(0);
        String mapImageSrc = mapImageElement.getAttribute("src");
        List<String> mapPathParams = integrationTestHelper.getQueryStringParameter(mapImageSrc, "path");

        assertEquals(1, mapPathParams.size());

        String[] mapPathAttributes = mapPathParams.get(0).split(":");
        String polyline = mapPathAttributes[mapPathAttributes.length - 1];
        List<Point> points =  new PolylineDecoder(polyline).getPoints();
        assertTrue(points.size() > 0);

        // 5. Accept the pickup request sent in the email
        integrationTestHelper.setLoggedInUser(IntegrationTestConstants.volunteer1Config);
        WebResponse acceptPage = client.getResponse(acceptLink);
        assertEquals("Pickup Accepted", acceptPage.getElementsByTagName("h1")[0].getText());

        // 6. Make sure that we sent a notification to the patient.
        sentMessages = integrationTestHelper.getSentEmailMessages();
        MailServicePb.MailMessage sentPatientMessage = sentMessages.get(1);

        assertTrue(sentPatientMessage.getSubject().startsWith("Appointment Pickup"));

        // 7. Make sure that an exportable ride record was generated
        List<RideRecord> rideRecords = CollectionUtils.listOfIterator(RideRecord.getExportableRecords().iterator());
        assertTrue(rideRecords.size() == 1);

        RideRecord rideRecord = rideRecords.get(0);
        assertEquals(UserProfile.get(patientUser).getKey(), rideRecord.getPatientProfileKey());
        assertEquals(UserProfile.get(volunteerUser).getKey(), rideRecord.getVolunteerUserProfileKey());

        // 8. Make sure the ride request was deleted
        assertEquals(0, OfyService.ofy().query(RideRequest.class).count());
    }

    @Test
    public void testMatchWithOneVolunteerDeclined() throws IOException, SAXException {
        // 1. Sign up a patient and a volunteer
        integrationTestHelper.signUpPatient(IntegrationTestConstants.patient1Config);
        integrationTestHelper.signUpVolunteer(IntegrationTestConstants.volunteer1Config);

        integrationTestHelper.setNotLoggedIn();

        ServletUnitClient client = integrationTestHelper.getClient();

        // 2. Trigger the notification endpoint
        WebRequest request = new PostMethodWebRequest("http://localhost/resources/notification");
        request.setParameter("patientEmail", IntegrationTestConstants.patient1Config.getEmail());
        request.setParameter("appointmentAddress", TestLocationConstants.PALO_ALTO_MEDICAL_FOUNDATION_ADDRESS);

        // Volunteer 1 is available at 4:20 PM on Wednesday.
        request.setParameter("appointmentTime", "2013-06-05T16:30:00-0700");

        WebResponse response = client.getResponse(request);
        assertEquals(200, response.getResponseCode());

        assertTrue(integrationTestHelper.waitForTask());

        // 3. Make sure that we sent out a pickup request
        List<MailServicePb.MailMessage> sentMessages = integrationTestHelper.getSentEmailMessages();

        assertEquals(1, sentMessages.size());
        MailServicePb.MailMessage sentVolunteerMessage = sentMessages.get(0);

        assertTrue(sentVolunteerMessage.getSubject().startsWith("Appointment Pickup"));

        String declineLink = integrationTestHelper.getLinkHrefWith("http://localhost/mail",
                sentVolunteerMessage.getHtmlBody(), "Decline");

        // 4. Decline the appointment pickup request
        integrationTestHelper.setLoggedInUser(IntegrationTestConstants.volunteer1Config);
        WebResponse declinePage = client.getResponse(declineLink);
        assertEquals("Pickup Declined", declinePage.getElementsByTagName("h1")[0].getText());

        assertTrue(integrationTestHelper.waitForTask());

        // 5. Make sure that we send out a pickup failed notification
        sentMessages = integrationTestHelper.getSentEmailMessages();

        assertEquals(2, sentMessages.size());

        MailServicePb.MailMessage sentPatientMessage = sentMessages.get(1);
        assertTrue(sentPatientMessage.getSubject().startsWith("No Pickup Available"));

        // 6. Make sure that no exportable ride record was generated
        assertTrue(!RideRecord.getExportableRecords().iterator().hasNext());

        // 7. Make sure the ride request was deleted
        assertEquals(0, OfyService.ofy().query(RideRequest.class).count());
    }

    @Test
    public void testMatchWithOneVolunteerBusy() throws IOException, SAXException {
        // 1. Sign up two patients and a volunteer
        integrationTestHelper.signUpPatient(IntegrationTestConstants.patient1Config);
        integrationTestHelper.signUpPatient(IntegrationTestConstants.patient2Config);
        integrationTestHelper.signUpVolunteer(IntegrationTestConstants.volunteer1Config);

        integrationTestHelper.setNotLoggedIn();

        ServletUnitClient client = integrationTestHelper.getClient();

        // 2. Trigger the notification endpoint for the second patient
        WebRequest request = new PostMethodWebRequest("http://localhost/resources/notification");
        request.setParameter("patientEmail", IntegrationTestConstants.patient2Config.getEmail());
        request.setParameter("appointmentAddress", TestLocationConstants.PALO_ALTO_MEDICAL_FOUNDATION_ADDRESS);

        // Volunteer 1 is available at 4:20 PM on Wednesday.
        request.setParameter("appointmentTime", "2013-06-05T16:30:00-0700");

        WebResponse response = client.getResponse(request);
        assertEquals(200, response.getResponseCode());

        assertTrue(integrationTestHelper.waitForTask());

        // 3. Make sure that we sent out a pickup request
        List<MailServicePb.MailMessage> sentMessages = integrationTestHelper.getSentEmailMessages();

        assertEquals(1, sentMessages.size());
        MailServicePb.MailMessage sentVolunteerMessage = sentMessages.get(0);

        assertTrue(sentVolunteerMessage.getSubject().startsWith("Appointment Pickup"));

        String acceptLink = integrationTestHelper.getLinkHrefWith("http://localhost/mail",
                sentVolunteerMessage.getHtmlBody(), "Accept");

        // 4. Accept the appointment pickup request
        integrationTestHelper.setLoggedInUser(IntegrationTestConstants.volunteer1Config);
        WebResponse acceptPage = client.getResponse(acceptLink);
        assertEquals(200, acceptPage.getResponseCode());

        // 5. Trigger the notification endpoint for the first patient
        integrationTestHelper.setNotLoggedIn();
        request = new PostMethodWebRequest("http://localhost/resources/notification");
        request.setParameter("patientEmail", IntegrationTestConstants.patient1Config.getEmail());
        request.setParameter("appointmentAddress", TestLocationConstants.PALO_ALTO_MEDICAL_FOUNDATION_ADDRESS);

        // Volunteer 1 is available at 4:20 PM on Wednesday, but the timeslot is now busy
        request.setParameter("appointmentTime", "2013-06-05T16:30:00-0700");

        response = client.getResponse(request);
        assertEquals(200, response.getResponseCode());

        assertTrue(integrationTestHelper.waitForTask());

        // 6. Make sure that the a "no pickup available" message was sent.
        sentMessages = integrationTestHelper.getSentEmailMessages();

        // There should be a pickup request, a success message, and a failed message.
        assertEquals(3, sentMessages.size());

        MailServicePb.MailMessage sentPatient1Message = sentMessages.get(2);
        assertTrue(sentPatient1Message.getSubject().startsWith("No Pickup Available"));
    }
}
