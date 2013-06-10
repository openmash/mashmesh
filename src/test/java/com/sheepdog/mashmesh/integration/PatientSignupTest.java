package com.sheepdog.mashmesh.integration;

import com.google.appengine.api.users.User;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletUnitClient;
import com.sheepdog.mashmesh.TestConstants;
import com.sheepdog.mashmesh.geo.GeoUtils;
import com.sheepdog.mashmesh.models.UserProfile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.junit.Assert.*;

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

    @Test
    public void testPatientSignsUp() throws IOException, SAXException {
        ServletUnitClient client = integrationTestHelper.getServletUnitClient();
        integrationTestHelper.setLoggedInUser("patient-1@example.com", false);

        // 1. Client lands on the root page
        WebResponse landingPage = client.getResponse("http://localhost/");

        // 2. Patient selects the "I am a patient" button
        WebResponse signupPage = landingPage.getLinkWith("I am a patient").click();

        // 3. Patient fills out the signup form
        WebForm signupForm = signupPage.getForms()[0];
        assertEquals("patient-1@example.com", signupForm.getParameterValue("email"));

        signupForm.setParameter("name", "John Smith");
        signupForm.setParameter("location", TestConstants.EAST_BAYSHORE_EPA_ADDRESS);
        WebResponse signupPagePostSubmit = signupForm.submit();

        // 4. Patient is informed that the page was submitted successfully
        signupPagePostSubmit.getElementsWithAttribute("class", "alert alert-success");
        WebForm signupFormPostSubmit = signupPagePostSubmit.getForms()[0];
        assertEquals("John Smith", signupFormPostSubmit.getParameterValue("name"));
        assertEquals("patient-1@example.com", signupFormPostSubmit.getParameterValue("email"));
        assertEquals(TestConstants.EAST_BAYSHORE_EPA_ADDRESS, signupFormPostSubmit.getParameterValue("location"));

        // 5. Make sure that we saved the new user as a patient
        User user = integrationTestHelper.getUser();
        UserProfile userProfile = UserProfile.get(user);
        assertEquals(UserProfile.UserType.PATIENT, userProfile.getType());

        // 6. Make sure that we assigned them a location somewhere around East Palo Alto
        assertTrue(GeoUtils.distanceMiles(userProfile.getLocation(), TestConstants.EAST_BAYSHORE_EPA_GEOPT) < 4);
    }
}
