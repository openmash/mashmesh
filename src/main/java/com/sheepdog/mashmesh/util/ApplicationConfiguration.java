package com.sheepdog.mashmesh.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ApplicationConfiguration {
    private static final String PROPERTIES_FILE_PATH = "WEB-INF/application.properties";
    private static Properties applicationProperties = null;

    // XXX: It would have been better to just use Spring in the first place than to
    //      have to reinvent the wheel.
    private static Properties getApplicationProperties() {
        if (applicationProperties == null) {
            Properties newApplicationProperties = new Properties();

            try {
                FileInputStream propertiesStream = new FileInputStream(PROPERTIES_FILE_PATH);
                newApplicationProperties.load(propertiesStream);
            } catch (IOException e) {
                // Given we're running on appengine, the absence of this file is a programmer error,
                // so there's no point using checked exceptions.
                throw new RuntimeException(e);
            }

            applicationProperties = newApplicationProperties;
        }

        return applicationProperties;
    }

    public static String getMapsApiKey() {
        return getApplicationProperties().getProperty("maps.apiKey");
    }

    public static String getNotificationEmailSender() {
        return getApplicationProperties().getProperty("notifications.emailSender");
    }

    public static String getFailureEmailAddress() {
        return getApplicationProperties().getProperty("notifications.failureEmailAddress");
    }
}