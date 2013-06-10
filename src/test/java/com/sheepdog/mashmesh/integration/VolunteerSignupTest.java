package com.sheepdog.mashmesh.integration;

import com.google.appengine.api.users.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletUnitClient;
import com.sheepdog.mashmesh.TestConstants;
import com.sheepdog.mashmesh.geo.GeoUtils;
import com.sheepdog.mashmesh.json.AvailableTimePeriodAdapter;
import com.sheepdog.mashmesh.models.AvailableTimePeriod;
import com.sheepdog.mashmesh.models.UserProfile;
import com.sheepdog.mashmesh.models.VolunteerProfile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    public String serializeAvailability(List<AvailableTimePeriod> availableTimePeriods) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(AvailableTimePeriod.class, new AvailableTimePeriodAdapter());
        Gson gson = gsonBuilder.create();
        return gson.toJson(availableTimePeriods);
    }

    @Test
    public void testVolunteerSignsUp() throws IOException, SAXException {
        ServletUnitClient client = integrationTestHelper.getServletUnitClient();
        integrationTestHelper.setLoggedInUser(TestConstants.VOLUNTEER_EMAIL, false);

        // 1. Client lands on the root page
        WebResponse landingPage = client.getResponse("http://localhost/");

        // 2. Volunteer selects the "I am a patient" button
        WebResponse signupPage = landingPage.getLinkWith("I am a volunteer").click();

        // 3. Volunteer fills out the signup form
        WebForm signupForm = signupPage.getForms()[0];
        assertEquals(TestConstants.VOLUNTEER_EMAIL, signupForm.getParameterValue("email"));

        String serializedTimePeriods = serializeAvailability(TestConstants.VOLUNTEER_AVAILABILITY);

        signupForm.setParameter("name", TestConstants.VOLUNTEER_NAME);
        signupForm.setParameter("maximumDistance", TestConstants.VOLUNTEER_MAXIMUM_DISTANCE);
        // Use the scriptable object to set the hidden availability field - HttpUnit can't
        // simulate enough javascript to be able to drive the UI correctly.
        signupForm.getScriptableObject()
                .setParameterValue("availability", serializedTimePeriods);
        signupForm.setParameter("location", TestConstants.UNIVERSITY_AVENUE_PA_ADDRESS);
        signupForm.setParameter("comments", TestConstants.VOLUNTEER_COMMENTS);
        WebResponse signupPagePostSubmit = signupForm.submit();

        // 4. Volunteer is informed that the page was submitted successfully
        signupPagePostSubmit.getElementsWithAttribute("class", "alert alert-success");
        WebForm signupFormPostSubmit = signupPagePostSubmit.getForms()[0];
        assertEquals(TestConstants.VOLUNTEER_NAME, signupFormPostSubmit.getParameterValue("name"));
        assertEquals(TestConstants.VOLUNTEER_EMAIL, signupFormPostSubmit.getParameterValue("email"));
        assertEquals(Float.parseFloat(TestConstants.VOLUNTEER_MAXIMUM_DISTANCE),
                Float.parseFloat(signupFormPostSubmit.getParameterValue("maximumDistance")),
                0.001);
        assertEquals(serializedTimePeriods, signupFormPostSubmit.getParameterValue("availability"));
        assertEquals(TestConstants.UNIVERSITY_AVENUE_PA_ADDRESS, signupFormPostSubmit.getParameterValue("location"));

        // 5. Make sure that we saved the new user as a volunteer
        User user = integrationTestHelper.getUser();
        UserProfile userProfile = UserProfile.get(user);
        assertEquals(UserProfile.UserType.VOLUNTEER, userProfile.getType());

        // 6. Make sure that we assigned them a location somewhere around University Avenue
        assertTrue(GeoUtils.distanceMiles(userProfile.getLocation(), TestConstants.UNIVERSITY_AVENUE_PA_GEOPT) < 4);

        // 7. Make sure that we saved the volunteer's availability properly.
        VolunteerProfile volunteerProfile = VolunteerProfile.get(userProfile);
        assertEquals(new HashSet<AvailableTimePeriod>(TestConstants.VOLUNTEER_AVAILABILITY),
                new HashSet<AvailableTimePeriod>(volunteerProfile.getAvailableTimePeriods()));
    }
}