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
import com.sheepdog.mashmesh.models.AvailableTimePeriod;
import com.sheepdog.mashmesh.models.UserProfile;
import com.sheepdog.mashmesh.models.VolunteerProfile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests related to volunteers creating accounts in the system
 */
public class VolunteerSignupTest {
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
     * Integration test which walks through the process of a new volunteer landing
     * on the home page and signing up.
     */
    @Test
    public void testVolunteerSignsUp() throws IOException, SAXException {
        ServletUnitClient client = integrationTestHelper.getClient();
        integrationTestHelper.setLoggedInUser(IntegrationTestConstants.VOLUNTEER_1);

        // 1. Client lands on the root page
        WebResponse landingPage = client.getResponse("http://localhost/");

        // 2. Volunteer selects the "I am a patient" button
        WebResponse signupPage = landingPage.getLinkWith("I am a volunteer").click();

        // 3. Volunteer fills out the signup form
        WebForm signupForm = signupPage.getForms()[0];
        assertEquals(IntegrationTestConstants.VOLUNTEER_1.getEmail(), signupForm.getParameterValue("email"));

        String serializedTimePeriods = integrationTestHelper.serializeAvailability(
                IntegrationTestConstants.VOLUNTEER_1.getAvailableTimePeriods());

        signupForm.setParameter("name", IntegrationTestConstants.VOLUNTEER_1.getName());
        signupForm.setParameter("maximumDistance", "" + IntegrationTestConstants.VOLUNTEER_1.getMaximumDistance());
        // Use the scriptable object to set the hidden availability field - HttpUnit can't
        // simulate enough javascript to be able to drive the UI correctly.
        signupForm.getScriptableObject()
                .setParameterValue("availability", serializedTimePeriods);
        signupForm.setParameter("location", IntegrationTestConstants.VOLUNTEER_1.getAddress());
        signupForm.setParameter("comments", IntegrationTestConstants.VOLUNTEER_1.getComments());
        WebResponse signupPagePostSubmit = signupForm.submit();

        // 4. Volunteer is informed that the page was submitted successfully
        signupPagePostSubmit.getElementsWithAttribute("class", "alert alert-success");
        WebForm signupFormPostSubmit = signupPagePostSubmit.getForms()[0];
        assertEquals(IntegrationTestConstants.VOLUNTEER_1.getName(),
                signupFormPostSubmit.getParameterValue("name"));
        assertEquals(IntegrationTestConstants.VOLUNTEER_1.getEmail(),
                signupFormPostSubmit.getParameterValue("email"));
        assertEquals(IntegrationTestConstants.VOLUNTEER_1.getMaximumDistance(),
                Float.parseFloat(signupFormPostSubmit.getParameterValue("maximumDistance")),
                0.001);
        assertEquals(serializedTimePeriods, signupFormPostSubmit.getParameterValue("availability"));
        assertEquals(TestLocationConstants.UNIVERSITY_AVENUE_PA_ADDRESS, signupFormPostSubmit.getParameterValue("location"));

        // 5. Make sure that we saved the new user as a volunteer
        User user = integrationTestHelper.getUser();
        UserProfile userProfile = UserProfile.get(user);
        assertEquals(UserProfile.UserType.VOLUNTEER, userProfile.getType());

        // 6. Make sure that we assigned them a location somewhere around University Avenue
        assertTrue(GeoUtils.distanceMiles(userProfile.getLocation(), TestLocationConstants.UNIVERSITY_AVENUE_PA_GEOPT) < 4);

        // 7. Make sure that we saved the volunteer's availability properly.
        VolunteerProfile volunteerProfile = VolunteerProfile.get(userProfile);
        assertEquals(new HashSet<AvailableTimePeriod>(IntegrationTestConstants.VOLUNTEER_1.getAvailableTimePeriods()),
                new HashSet<AvailableTimePeriod>(volunteerProfile.getAvailableTimePeriods()));

        integrationTestHelper.observeRateLimit();
    }
}
