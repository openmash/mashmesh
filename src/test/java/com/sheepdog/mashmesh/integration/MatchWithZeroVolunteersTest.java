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
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the case that an appointment notification is sent for a patient
 * when there are no volunteers in the system.
 */
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

    /**
     * Integration test which checks that match failure notifications are sent to
     * patients when there are no volunteers in the system to match them with.
     */
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
        MailServicePb.MailMessage sentMessage = integrationTestHelper.popNextEmailMessage();
        assertTrue(sentMessage.getSubject().startsWith("No Pickup Available"));
        assertEquals(Collections.singletonList(IntegrationTestConstants.PATIENT_1.getEmail()), sentMessage.tos());

        // Make sure that no ride requests remain
        assertEquals(0, OfyService.ofy().query(RideRequest.class).count());
    }
}
