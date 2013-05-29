package com.sheepdog.mashmesh.util;

import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.CommonGoogleClientRequestInitializer;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

class TwoLeggedOAuthRequestInitializer extends CommonGoogleClientRequestInitializer {
    private final String emailAddress;

    public TwoLeggedOAuthRequestInitializer(String apiKey, String emailAddress) {
        super(apiKey);
        this.emailAddress = emailAddress;
    }

    @Override
    public void initialize(AbstractGoogleClientRequest<?> request) throws IOException {
        Map<String, Object> customKeys = Collections.singletonMap("xoauth_requestor_id", (Object) emailAddress);
        request.setUnknownKeys(customKeys);
    }
}
