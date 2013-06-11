package com.sheepdog.mashmesh.integration;

import com.google.appengine.api.mail.MailServicePb;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletUnitClient;
import com.sheepdog.mashmesh.TestLocationConstants;
import com.sheepdog.mashmesh.models.OfyService;
import com.sheepdog.mashmesh.models.RideRequest;
import com.sheepdog.mashmesh.tasks.SendNotificationTask;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MatchWithZeroVolunteersTest {
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
    public void testMatchWithoutVolunteer() throws IOException, SAXException {
        integrationTestHelper.signUpPatient(IntegrationTestConstants.PATIENT_1);
        integrationTestHelper.setNotLoggedIn();

        ServletUnitClient client = integrationTestHelper.getClient();

        WebRequest request = new PostMethodWebRequest("http://localhost/resources/notification");
        request.setParameter("patientEmail", IntegrationTestConstants.PATIENT_1.getEmail());
        request.setParameter("appointmentAddress", TestLocationConstants.PALO_ALTO_MEDICAL_FOUNDATION_ADDRESS);
        request.setParameter("appointmentTime", "2013-06-05T16:30:00-0700");

        WebResponse response = client.getResponse(request);
        assertEquals(200, response.getResponseCode());

        assertEquals(1, integrationTestHelper.getTaskCount(SendNotificationTask.QUEUE_NAME));
        assertTrue(integrationTestHelper.waitForTask());

        // Make sure that we sent out a match failure notification
        List<MailServicePb.MailMessage> sentMessages = integrationTestHelper.getSentEmailMessages();

        assertEquals(1, sentMessages.size());

        // XXX: Due to bugs in the local mail service, we can't get the To, CC, or BCC lists.
        MailServicePb.MailMessage sentMessage = sentMessages.get(0);

        assertTrue(sentMessage.getSubject().startsWith("No Pickup Available"));

        // Make sure that no ride requests remain
        assertEquals(0, OfyService.ofy().query(RideRequest.class).count());
    }
}
