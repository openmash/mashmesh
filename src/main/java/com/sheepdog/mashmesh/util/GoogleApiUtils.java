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

import com.google.api.client.auth.oauth.OAuthHmacSigner;
import com.google.api.client.auth.oauth.OAuthParameters;
import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.services.CommonGoogleClientRequestInitializer;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.fusiontables.Fusiontables;
import com.google.api.services.fusiontables.FusiontablesScopes;
import com.google.api.services.oauth2.Oauth2;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collection;

public class GoogleApiUtils {
    private static final HttpTransport HTTP_TRANSPORT = new UrlFetchTransport();
    private static final JacksonFactory JSON_FACTORY = new JacksonFactory();

    private static final Collection<String> SERVICE_ACCOUNT_SCOPES = Arrays.asList(
            DriveScopes.DRIVE,
            FusiontablesScopes.FUSIONTABLES
    );

    private static GoogleCredential serviceCredential = null;

    private static OAuthParameters getClientOAuthParameters() {
        OAuthHmacSigner signer = new OAuthHmacSigner();
        signer.clientSharedSecret = ApplicationConfiguration.getOAuthConsumerSecret();

        OAuthParameters oauthParameters = new OAuthParameters();
        oauthParameters.version = "1.0";
        oauthParameters.consumerKey = ApplicationConfiguration.getOAuthConsumerKey();
        oauthParameters.signer = signer;
        return oauthParameters;
    }

    public static Calendar getCalendar(final String emailAddress) {
        String apiKey = ApplicationConfiguration.getApiKey();
        GoogleClientRequestInitializer requestInitializer =
                new TwoLeggedOAuthRequestInitializer(apiKey, emailAddress);

        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getClientOAuthParameters())
                .setApplicationName(ApplicationConfiguration.getOAuthApplicationName())
                .setGoogleClientRequestInitializer(requestInitializer)
                .build();
    }

    public static Oauth2 getOauth2(String emailAddress) {
        String apiKey = ApplicationConfiguration.getApiKey();
        GoogleClientRequestInitializer requestInitializer =
                new TwoLeggedOAuthRequestInitializer(apiKey, emailAddress);

        return new Oauth2.Builder(HTTP_TRANSPORT, JSON_FACTORY, getClientOAuthParameters())
                .setApplicationName(ApplicationConfiguration.getOAuthApplicationName())
                .setGoogleClientRequestInitializer(requestInitializer)
                .build();
    }

    private static GoogleCredential getServiceCredential() throws GeneralSecurityException, IOException {
        if (serviceCredential == null) {
            serviceCredential = new GoogleCredential.Builder().setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountId(ApplicationConfiguration.getOAuthServiceAccount())
                .setServiceAccountScopes(SERVICE_ACCOUNT_SCOPES)
                .setServiceAccountPrivateKeyFromP12File(new File("WEB-INF/serviceAccountKey.p12"))
                .build();
        }

        return serviceCredential;
    }

    public static Fusiontables getFusiontables() throws GeneralSecurityException, IOException {
        return new Fusiontables.Builder(HTTP_TRANSPORT, JSON_FACTORY, getServiceCredential())
                .setApplicationName(ApplicationConfiguration.getOAuthApplicationName())
                .build();
    }

    public static Drive getDrive() throws GeneralSecurityException, IOException {
        String apiKey = ApplicationConfiguration.getApiKey();
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getServiceCredential())
                .setApplicationName(ApplicationConfiguration.getOAuthApplicationName())
                .setGoogleClientRequestInitializer(new CommonGoogleClientRequestInitializer(apiKey))
                .build();
    }
}
