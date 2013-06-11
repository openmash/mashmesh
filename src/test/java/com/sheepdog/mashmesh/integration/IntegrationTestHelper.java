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
import com.meterware.httpunit.WebForm;
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

public class IntegrationTestHelper {
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
    }

    public void tearDown() throws Exception {
        servletRunner.shutDown();

        localServiceTestHelper.tearDown();
        observeRateLimit();
    }

    public ServletRunner getServletRunner() {
        return servletRunner;
    }

    public ServletUnitClient getClient() {
        return client;
    }

    public LocalServiceTestHelper getServiceTestHelper() {
        return localServiceTestHelper;
    }

    public HtmlClientWrapper getHtmlClientForString(String urlString, String html) throws IOException {
        URL url = new URL(urlString);
        StringWebResponse response = new StringWebResponse(html, url);
        WebClient webClient = new WebClient();
        HtmlPage page = HTMLParser.parseHtml(response, webClient.getCurrentWindow());
        return new HtmlClientWrapper(webClient, page);
    }

    public String getLinkHrefWith(String urlString, String html, String text) throws IOException {
        HtmlClientWrapper wrapper = getHtmlClientForString(urlString, html);
        return wrapper.getPage().getAnchorByText(text).getHrefAttribute();
    }

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

    private String getUserId(String emailAddress) {
        return DigestUtils.md5Hex(emailAddress);
    }

    private Map<String, Object> getAppEngineAttributes() {
        return ApiProxy.getCurrentEnvironment().getAttributes();
    }

    public void setLoggedInUser(String emailAddress, boolean isAdmin) {
        localServiceTestHelper
                .setEnvIsLoggedIn(true)
                .setEnvIsAdmin(isAdmin)
                .setEnvEmail(emailAddress)
                .setEnvAuthDomain(EmailUtils.extractDomain(emailAddress));

        // setEnvAttributes is currently broken, so we need to set the user ID directly.
        getAppEngineAttributes().put(USER_ID_ATTRIBUTE_NAME, getUserId(emailAddress));
    }

    public void setLoggedInUser(PatientConfig patientConfig) {
        setLoggedInUser(patientConfig.getEmail(), false);
    }

    public void setLoggedInUser(VolunteerConfig volunteerConfig) {
        setLoggedInUser(volunteerConfig.getEmail(), false);
    }

    public User getUser() {
        return UserServiceFactory.getUserService().getCurrentUser();
    }

    public void setNotLoggedIn() {
        localServiceTestHelper.setEnvIsLoggedIn(false);
        getAppEngineAttributes().remove(USER_ID_ATTRIBUTE_NAME);
    }

    public String serializeAvailability(List<AvailableTimePeriod> availableTimePeriods) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(AvailableTimePeriod.class, new AvailableTimePeriodAdapter());
        Gson gson = gsonBuilder.create();
        return gson.toJson(availableTimePeriods);
    }

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

    public int getTaskCount(String queueName) {
        LocalTaskQueue localTaskQueue = LocalTaskQueueTestConfig.getLocalTaskQueue();
        QueueStateInfo queueState = localTaskQueue.getQueueStateInfo().get(queueName);
        return queueState.getCountTasks();
    }

    public boolean waitForTask() {
        try {
            return latch.awaitAndReset(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false; // This won't happen
        }
    }

    public List<MailServicePb.MailMessage> getSentEmailMessages() {
        return LocalMailServiceTestConfig.getLocalMailService().getSentMessages();
    }
}
