package com.sheepdog.mashmesh.util;

import com.google.api.client.auth.oauth.OAuthHmacSigner;
import com.google.api.client.auth.oauth.OAuthParameters;
import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.services.CommonGoogleClientRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarRequest;
import com.google.api.services.calendar.CalendarRequestInitializer;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.fusiontables.Fusiontables;
import com.google.api.services.fusiontables.FusiontablesScopes;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

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
        CalendarRequestInitializer calendarRequestInitializer = new CalendarRequestInitializer(apiKey) {
            @Override
            public void initializeCalendarRequest(CalendarRequest<?> request) {
                Map<String, Object> customKeys = Collections.singletonMap("xoauth_requestor_id", (Object)emailAddress);
                request.setUnknownKeys(customKeys);
            }
        };

        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getClientOAuthParameters())
                .setApplicationName(ApplicationConfiguration.getOAuthApplicationName())
                .setCalendarRequestInitializer(calendarRequestInitializer)
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
