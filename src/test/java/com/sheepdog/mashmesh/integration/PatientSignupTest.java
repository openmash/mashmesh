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

import com.google.appengine.api.users.User;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletUnitClient;
import com.sheepdog.mashmesh.TestLocationConstants;
import com.sheepdog.mashmesh.geo.GeoUtils;
import com.sheepdog.mashmesh.models.UserProfile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Tests related to patients creating accounts in the system
 */
public class PatientSignupTest {
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
     * Integration test which walks through the process of a new patient landing
     * on the home page, and signing up.
     */
    @Test
    public void testPatientSignsUp() throws IOException, SAXException {
        ServletUnitClient client = integrationTestHelper.getClient();
        integrationTestHelper.setLoggedInUser(IntegrationTestConstants.PATIENT_1);

        // 1. Client lands on the root page
        WebResponse landingPage = client.getResponse("http://localhost/");

        // 2. Patient selects the "I am a patient" button
        WebResponse signupPage = landingPage.getLinkWith("I am a patient").click();

        // 3. Patient fills out the signup form
        WebForm signupForm = signupPage.getForms()[0];
        assertEquals(IntegrationTestConstants.PATIENT_1.getEmail(), signupForm.getParameterValue("email"));

        signupForm.setParameter("name", IntegrationTestConstants.PATIENT_1.getName());
        signupForm.setParameter("location", IntegrationTestConstants.PATIENT_1.getAddress());
        WebResponse signupPagePostSubmit = signupForm.submit();

        // 4. Patient is informed that the page was submitted successfully
        signupPagePostSubmit.getElementsWithAttribute("class", "alert alert-success");
        WebForm signupFormPostSubmit = signupPagePostSubmit.getForms()[0];
        assertEquals(IntegrationTestConstants.PATIENT_1.getName(), signupFormPostSubmit.getParameterValue("name"));
        assertEquals(IntegrationTestConstants.PATIENT_1.getEmail(),
                signupFormPostSubmit.getParameterValue("email"));
        assertEquals(IntegrationTestConstants.PATIENT_1.getAddress(),
                signupFormPostSubmit.getParameterValue("location"));

        // 5. Make sure that we saved the new user as a patient
        User user = integrationTestHelper.getUser();
        UserProfile userProfile = UserProfile.get(user);
        assertEquals(UserProfile.UserType.PATIENT, userProfile.getType());

        // 6. Make sure that we assigned them a location somewhere around East Palo Alto
        assertTrue(GeoUtils.distanceMiles(userProfile.getLocation(), TestLocationConstants.EAST_BAYSHORE_EPA_GEOPT) < 4);

        integrationTestHelper.observeRateLimit();
    }
}
