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

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.extensions.appengine.auth.oauth2.AppEngineCredentialStore;
import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.oauth2.Oauth2Scopes;

import java.util.Arrays;
import java.util.List;

public class OAuthAuthorization {
    private static final String OAUTH2_CLIENT_ID = ""; // TODO
    private static final String OAUTH2_CLIENT_SECRET = "";
    private static final List<String> SCOPES = Arrays.asList(Oauth2Scopes.USERINFO_EMAIL,
            Oauth2Scopes.USERINFO_PROFILE);

    public static final AuthorizationCodeFlow getAuthorizationCodeFlow() {
        GoogleAuthorizationCodeFlow.Builder flowBuilder = new GoogleAuthorizationCodeFlow.Builder(
                new UrlFetchTransport(),
                new JacksonFactory(),
                OAUTH2_CLIENT_ID,
                OAUTH2_CLIENT_SECRET,
                SCOPES);
        return flowBuilder.setCredentialStore(new AppEngineCredentialStore()).build();
    }
}
