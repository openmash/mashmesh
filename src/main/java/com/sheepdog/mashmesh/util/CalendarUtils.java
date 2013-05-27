package com.sheepdog.mashmesh.util;

import com.google.api.client.auth.oauth.OAuthHmacSigner;
import com.google.api.client.auth.oauth.OAuthParameters;
import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarRequest;
import com.google.api.services.calendar.CalendarRequestInitializer;

import java.util.Collections;
import java.util.Map;

public class CalendarUtils {
    public static Calendar getCalendar(final String emailAddress) {
        HttpTransport transport = new UrlFetchTransport();
        JacksonFactory jsonFactory = new JacksonFactory();

        OAuthHmacSigner signer = new OAuthHmacSigner();
        signer.clientSharedSecret = ApplicationConfiguration.getOAuthConsumerSecret();

        final OAuthParameters oauthParameters = new OAuthParameters();
        oauthParameters.version = "1.0";
        oauthParameters.consumerKey = ApplicationConfiguration.getOAuthConsumerKey();
        oauthParameters.signer = signer;

        String apiKey = ApplicationConfiguration.getApiKey();
        CalendarRequestInitializer calendarRequestInitializer = new CalendarRequestInitializer(apiKey) {
            @Override
            public void initializeCalendarRequest(CalendarRequest<?> request) {
                Map<String, Object> customKeys = Collections.singletonMap("xoauth_requestor_id", (Object)emailAddress);
                request.setUnknownKeys(customKeys);
            }
        };

        return new Calendar.Builder(transport, jsonFactory, oauthParameters)
                .setApplicationName(ApplicationConfiguration.getOAuthApplicationName())
                .setCalendarRequestInitializer(calendarRequestInitializer)
                .build();
    }
}
