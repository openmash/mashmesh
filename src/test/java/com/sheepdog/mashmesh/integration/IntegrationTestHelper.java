package com.sheepdog.mashmesh.integration;

import com.gargoylesoftware.htmlunit.StringWebResponse;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HTMLParser;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.tools.development.testing.*;
import com.google.apphosting.api.ApiProxy;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;
import com.sheepdog.mashmesh.util.EmailUtils;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

public class IntegrationTestHelper {
    private static final String USER_ID_ATTRIBUTE_NAME = "com.google.appengine.api.users.UserService.user_id_key";

    private final LocalServiceTestHelper localServiceTestHelper =
            new LocalServiceTestHelper(
                    new LocalDatastoreServiceTestConfig(),
                    new LocalMemcacheServiceTestConfig(),
                    new LocalSearchServiceTestConfig(),
                    new LocalTaskQueueTestConfig()
            );

    private ServletRunner servletRunner = null;

    private ServletUnitClient servletUnitClient = null;

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

    public void setUp() throws Exception {
        InputStream webXmlInputStream = getClass().getClassLoader().getResourceAsStream("WEB-INF/web.xml");
        servletRunner = new ServletRunner(webXmlInputStream);
        servletUnitClient = servletRunner.newClient();

        localServiceTestHelper.setUp();
    }

    public void tearDown() throws Exception {
        servletRunner.shutDown();

        localServiceTestHelper.tearDown();
        Thread.sleep(5000); // Sleep five seconds to avoid rate limits on Google APIs
    }

    public ServletUnitClient getServletUnitClient() {
        return servletUnitClient;
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

    public User getUser() {
        return UserServiceFactory.getUserService().getCurrentUser();
    }

    public void setNotLoggedIn() {
        localServiceTestHelper.setEnvIsLoggedIn(false);
        getAppEngineAttributes().remove(USER_ID_ATTRIBUTE_NAME);
    }
}
