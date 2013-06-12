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

import com.gargoylesoftware.htmlunit.StringWebResponse;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HTMLParser;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.appengine.api.mail.MailServicePb;
import com.google.appengine.api.taskqueue.dev.LocalTaskQueue;
import com.google.appengine.api.taskqueue.dev.QueueStateInfo;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.tools.development.testing.*;
import com.google.apphosting.api.ApiProxy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;
import com.sheepdog.mashmesh.json.AvailableTimePeriodAdapter;
import com.sheepdog.mashmesh.models.AvailableTimePeriod;
import com.sheepdog.mashmesh.util.EmailUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The IntegrationTestHelper class wraps a LocalServiceTestHelper to help setup
 * and tear down each test. It also provides helper methods for common tasks
 * performed by the integration tests, such as fetching links from HTML documents
 * which are stored in strings, or fetching the next unread transactional email.
 */
public class IntegrationTestHelper {
    // The internal AppEngine environment attribute which stores the userId of the
    //  currently logged-in user.
    private static final String USER_ID_ATTRIBUTE_NAME = "com.google.appengine.api.users.UserService.user_id_key";

    private final LocalTaskQueueTestConfig.TaskCountDownLatch latch =
            new LocalTaskQueueTestConfig.TaskCountDownLatch(1);

    private final LocalServiceTestHelper localServiceTestHelper =
            new LocalServiceTestHelper(
                    new LocalDatastoreServiceTestConfig(),
                    new LocalMemcacheServiceTestConfig(),
                    new LocalSearchServiceTestConfig(),
                    new LocalTaskQueueTestConfig()
                        .setDisableAutoTaskExecution(false)
                        .setCallbackClass(LocalTaskQueueTestConfig.DeferredTaskCallback.class)
                        .setTaskExecutionLatch(latch)
            );

    private ServletRunner servletRunner = null;

    private ServletUnitClient client = null;

    // The zero-based index of the next email message in the sent messages list.
    // This index is reset at the end of each test.
    private int nextEmailIndex = 0;

    /**
     * A HtmlClientWrapper instance wraps a WebClient and a HtmlPage that
     *  were generated from a string of HTML.
     */
    public static class HtmlClientWrapper {
        private final WebClient client;
        private final HtmlPage page;

        private HtmlClientWrapper(WebClient client, HtmlPage page) {
            this.client = client;
            this.page = page;
        }

        public WebClient getClient() {
            return client;
        }

        public HtmlPage getPage() {
            return page;
        }
    }

    /**
     * Sleep long enough to ensure that the Google Geocoding API doesn't throttle
     *  our requests and cause out tests to fail.
     */
    public void observeRateLimit() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // This is not going to happen, so satisfy Java with a runtime exception
            throw new RuntimeException(e);
        }
    }

    public void setUp() throws Exception {
        InputStream webXmlInputStream = getClass().getClassLoader().getResourceAsStream("WEB-INF/web.xml");
        servletRunner = new ServletRunner(webXmlInputStream);
        client = servletRunner.newClient();

        localServiceTestHelper.setUp();

        nextEmailIndex = 0;
    }

    public void tearDown() throws Exception {
        servletRunner.shutDown();

        localServiceTestHelper.tearDown();
    }

    /**
     * @return a ServletUnitClient client tied to the ServletRunner being used to
     * drive the currently-executing integration test.
     */
    public ServletUnitClient getClient() {
        return client;
    }

    /**
     * Constructs a HtmlClientWrapper instance for an arbitrary string of HTML.
     * This is used to extract links and other information from transactional email
     * captured by the AppEngine mail test stub.
     *
     * @param urlString a string containing the URL the page was nominally fetched from. The URL
     *                  does not need to actually exist.
     * @param html the string of HTML to wrap.
     * @return a HtmlClientWrapper instance with a WebPage wrapping the given string.
     */
    public HtmlClientWrapper getHtmlClientForString(String urlString, String html) throws IOException {
        URL url = new URL(urlString);
        StringWebResponse response = new StringWebResponse(html, url);
        WebClient webClient = new WebClient();
        HtmlPage page = HTMLParser.parseHtml(response, webClient.getCurrentWindow());
        return new HtmlClientWrapper(webClient, page);
    }

    /**
     * Fetches the href attribute of a link with the given text content from an
     * arbitrary string of HTML.
     *
     * @param urlString a string containing the URL the page was nominally fetched from. The URL
     *                  does not need to actually exist.
     * @param html the string of HTML to parse.
     * @param text the text body of the link to search for.
     * @return the link's href attribute, as a String
     */
    public String getLinkHrefWith(String urlString, String html, String text) throws IOException {
        HtmlClientWrapper wrapper = getHtmlClientForString(urlString, html);
        return wrapper.getPage().getAnchorByText(text).getHrefAttribute();
    }

    /**
     * Extracts all values of a query string parameter from a string representing a URL.
     * @param url a string containing the URL
     * @param queryParameter the name of the query string parameter to extract
     * @return a list of strings, each one representing a value set for the given parameter
     * @throws URISyntaxException if the URL is malformed.
     */
    public List<String> getQueryStringParameter(String url, String queryParameter) throws URISyntaxException {
        List<NameValuePair> pairs = URLEncodedUtils.parse(new URI(url), "UTF-8");
        List<String> values = new ArrayList<String>();

        for (NameValuePair pair : pairs) {
            if (pair.getName().equals(queryParameter)) {
                values.add(pair.getValue());
            }
        }

        return values;
    }

    /**
     * Generates a deterministic userId from a given email address. This method is
     * necessary because the AppEngine test stubs do not generate userIds from emails
     * automatically.
     * @param emailAddress the email address of the user
     * @return the unique userId as a string.
     */
    private String getUserId(String emailAddress) {
        return DigestUtils.md5Hex(emailAddress);
    }

    private Map<String, Object> getAppEngineAttributes() {
        return ApiProxy.getCurrentEnvironment().getAttributes();
    }

    /**
     * Sets the user currently logged into the AppEngine API stub
     * @param emailAddress the email address of the user
     * @param isAdmin true if the user is an administrator, false otherwise
     */
    public void setLoggedInUser(String emailAddress, boolean isAdmin) {
        localServiceTestHelper
                .setEnvIsLoggedIn(true)
                .setEnvIsAdmin(isAdmin)
                .setEnvEmail(emailAddress)
                .setEnvAuthDomain(EmailUtils.extractDomain(emailAddress));

        // setEnvAttributes is currently broken, so we need to set the user ID directly.
        getAppEngineAttributes().put(USER_ID_ATTRIBUTE_NAME, getUserId(emailAddress));
    }

    /**
     * Sets the user currently logged into the AppEngine API stub
     * @param patientConfig a descriptor for the patient to log in
     */
    public void setLoggedInUser(PatientConfig patientConfig) {
        setLoggedInUser(patientConfig.getEmail(), false);
    }

    /**
     * Sets the user currently logged into the AppEngine API stub
     * @param volunteerConfig a descriptor for the volunteer to log in
     */
    public void setLoggedInUser(VolunteerConfig volunteerConfig) {
        setLoggedInUser(volunteerConfig.getEmail(), false);
    }

    /**
     * Gets the user which is currently logged into the AppEngine API stub
     * @return the currently logged-in user
     */
    public User getUser() {
        return UserServiceFactory.getUserService().getCurrentUser();
    }

    /**
     * Logs out the currently logged-in user from the AppEngine API stub
     */
    public void setNotLoggedIn() {
        localServiceTestHelper.setEnvIsLoggedIn(false);
        getAppEngineAttributes().remove(USER_ID_ATTRIBUTE_NAME);
    }

    /**
     * Serialize a list of AvailableTimePeriod instances to JSON. Used to stub in a replacement
     * for available.js, because HttpUnit has no support for driving drag events.
     * @param availableTimePeriods the list of AvailableTimePeriods to serialize
     * @return a JSON string containing the serialized available time periods.
     */
    public String serializeAvailability(List<AvailableTimePeriod> availableTimePeriods) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(AvailableTimePeriod.class, new AvailableTimePeriodAdapter());
        Gson gson = gsonBuilder.create();
        return gson.toJson(availableTimePeriods);
    }

    /**
     * Convenience method to drive a click through of the process of signing up a new patient.
     * @param patientConfig the specification of the patient to create
     * @return a User instance representing the new patient
     * @throws SAXException if there is an error parsing a page in the click-though sequence.
     * @throws IOException if there is an issue reading a page.
     */
    public User signUpPatient(PatientConfig patientConfig) throws SAXException, IOException {
        setLoggedInUser(patientConfig.getEmail(), false);

        WebResponse landingPage = client.getResponse("http://localhost/");

        WebResponse signupPage = landingPage.getLinkWith("I am a patient").click();
        WebForm signupForm = signupPage.getForms()[0];
        signupForm.setParameter("name", patientConfig.getName());
        signupForm.setParameter("location", patientConfig.getAddress());
        signupForm.submit();

        observeRateLimit();

        return getUser();
    }

    /**
     * Convenience method to drive a click through of the process of signing up a new volunteer.
     * @param volunteerConfig the specification of the patient to create
     * @return a User instance representing the new volunteer.
     * @throws SAXException if there is an error parsing a page in the click-though sequence.
     * @throws IOException if there is an issue reading a page.
     */
    public User signUpVolunteer(VolunteerConfig volunteerConfig) throws IOException, SAXException {
        String serializedTimePeriods = serializeAvailability(volunteerConfig.getAvailableTimePeriods());

        setLoggedInUser(volunteerConfig.getEmail(), false);

        WebResponse landingPage = client.getResponse("http://localhost/");

        WebResponse signupPage = landingPage.getLinkWith("I am a volunteer").click();
        WebForm signupForm = signupPage.getForms()[0];
        signupForm.setParameter("name", volunteerConfig.getName());
        signupForm.setParameter("maximumDistance", "" + volunteerConfig.getMaximumDistance());
        // Use the scriptable object to set the hidden availability field - HttpUnit can't
        // simulate enough javascript to be able to drive the UI correctly.
        signupForm.getScriptableObject()
                .setParameterValue("availability", serializedTimePeriods);
        signupForm.setParameter("location", volunteerConfig.getAddress());
        signupForm.setParameter("comments", volunteerConfig.getComments());
        signupForm.submit();

        observeRateLimit();

        return getUser();
    }

    /**
     * Convenience method to send an appointment notification for a given patient and
     * wait for the task queue to complete processing
     * @param patientConfig the patient to schedule an appointment for
     * @param appointmentAddress the address of the appointment
     * @param appointmentTime the date and time of the appointment in RFC3339 format
     * @throws IOException if there is an error retrieving a response
     * @throws SAXException if there is a problem parsing the response from the notification endpoint
     */
    public void runNotification(PatientConfig patientConfig, String appointmentAddress, String appointmentTime)
            throws IOException, SAXException {
        setNotLoggedIn();

        WebRequest request = new PostMethodWebRequest("http://localhost/resources/notification");
        request.setParameter("patientEmail", patientConfig.getEmail());
        request.setParameter("appointmentAddress", appointmentAddress);
        request.setParameter("appointmentTime", appointmentTime);

        WebResponse response = client.getResponse(request);

        if (response.getResponseCode() != 200) {
            throw new RuntimeException("Failed to trigger appointment notification");
        }

        if (!waitForTask()) {
            throw new RuntimeException("No task executed");
        }
    }

    /**
     * Fetches the number of tasks that have been scheduled in a named queue in the
     * current test, including both tasks that have executed, and those that have not.
     * @param queueName the name of the queue
     * @return the total number of tasks enqueued by the test
     */
    public int getTaskCount(String queueName) {
        LocalTaskQueue localTaskQueue = LocalTaskQueueTestConfig.getLocalTaskQueue();
        QueueStateInfo queueState = localTaskQueue.getQueueStateInfo().get(queueName);
        return queueState.getCountTasks();
    }

    /**
     * Waits up to a minute for the next task in any task queue to finish processing
     * @return true if a task completed in the allotted time, false otherwise
     */
    public boolean waitForTask() {
        try {
            return latch.awaitAndReset(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false; // This won't happen
        }
    }

    /**
     * @return a list of all email messages sent by the current test
     */
    public List<MailServicePb.MailMessage> getSentEmailMessages() {
        return LocalMailServiceTestConfig.getLocalMailService().getSentMessages();
    }

    /**
     * @return the next unprocessed transactional email (ie. the next email which is addressed
     *         to a patient or a volunteer, not an administrator.)
     */
    public MailServicePb.MailMessage popNextEmailMessage() {
        int emailIndex = nextEmailIndex;
        nextEmailIndex++;
        List<MailServicePb.MailMessage> sentMessages = getSentEmailMessages();

        while (sentMessages.get(emailIndex).toSize() == 0) {
            emailIndex++;
            nextEmailIndex++;

            if (emailIndex >= sentMessages.size()) {
                throw new RuntimeException("No new email received");
            }
        }

        return getSentEmailMessages().get(emailIndex);
    }

    /**
     * Fetches the page resulting from clicking on a link with the specified body text in an email
     * @param emailAddress the email address of the user who will click on the link
     * @param emailMessage the email message in which to locate the link
     * @param linkText the body text of the link to be clicked
     * @return a WebResponse for the page fetched using the given link's href attribute.
     * @throws IOException if there was an issue reading the requested page
     * @throws SAXException if there was an issue parsing the requested page
     */
    public WebResponse clickEmailLink(String emailAddress, MailServicePb.MailMessage emailMessage, String linkText)
            throws IOException, SAXException {
        if (!emailMessage.tos().contains(emailAddress)) {
            throw new RuntimeException("Email link clicked by a user other than the recipient");
        }

        String linkHref = getLinkHrefWith("http://localhost/mail", emailMessage.getHtmlBody(), linkText);
        setLoggedInUser(emailAddress, false);
        return client.getResponse(linkHref);
    }

    /**
     * Fetches the page resulting from clicking on a link with the specified body text in the
     * next unprocessed email message.
     * @param emailAddress the email address of the user who will click on the link
     * @param linkText the body text of the link to be clicked
     * @return a WebResponse for the page fetched using the given link's href attribute.
     * @throws IOException if there was an issue reading the requested page
     * @throws SAXException if there was an issue parsing the requested page
     */
    public WebResponse clickNextEmailLink(String emailAddress, String linkText) throws IOException, SAXException {
        return clickEmailLink(emailAddress, popNextEmailMessage(), linkText);
    }

    /**
     * Fetches the page resulting from clicking on a link with the specified body text in the
     * next unprocessed email message.
     * @param volunteerConfig the volunteer who will click on the link
     * @param linkText the body text of the link to be clicked
     * @return a WebResponse for the page fetched using the given link's href attribute.
     * @throws IOException if there was an issue reading the requested page
     * @throws SAXException if there was an issue parsing the requested page
     */
    public WebResponse clickNextEmailLink(VolunteerConfig volunteerConfig, String linkText)
            throws IOException, SAXException {
        return clickNextEmailLink(volunteerConfig.getEmail(), linkText);
    }
}
