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
package com.sheepdog.mashmesh.util;

import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.ApiProxy;

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

    public static String getApiKey() {
        return getApplicationProperties().getProperty("google.apiKey");
    }

    public static String getAppId() {
        return SystemProperty.applicationId.get();
    }

    public static boolean isDevelopment() {
        return SystemProperty.environment.value() == SystemProperty.Environment.Value.Development;
    }

    public static String getBaseUrl() {
        if (isDevelopment()) {
            // TODO: Make maven use this base URL!
            return getApplicationProperties().getProperty("web.development.baseUrl");
        } else {
            return String.format("http://%s.appspot.com", getAppId());
        }
    }

    public static String getNotificationEmailSender() {
        return String.format("notifications@%s.appspotmail.com", getAppId());
    }

    public static String getOAuthApplicationName() {
        return getApplicationProperties().getProperty("google.oauth.applicationName");
    }

    public static String getOAuthConsumerKey() {
        return getApplicationProperties().getProperty("google.oauth.consumerKey");
    }

    public static String getOAuthConsumerSecret() {
        return getApplicationProperties().getProperty("google.oauth.consumerSecret");
    }

    public static String getOAuthServiceAccount() {
        return getApplicationProperties().getProperty("google.oauth.serviceAccount");
    }
}
