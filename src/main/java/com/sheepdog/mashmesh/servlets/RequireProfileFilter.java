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
package com.sheepdog.mashmesh.servlets;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.oauth2.model.Userinfo;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.sheepdog.mashmesh.models.UserProfile;
import com.sheepdog.mashmesh.util.GoogleApiUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RequireProfileFilter implements Filter {
    private static final String PROFILE_PATH = "/view/profile/";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    private static UserProfile createUserProfile(User user) throws IOException {
        UserProfile userProfile = UserProfile.create(user);

        try {
            Userinfo userInfo = GoogleApiUtils.getOauth2(userProfile.getEmail())
                    .userinfo()
                    .get()
                    .execute();
            userProfile.setFullName(userInfo.getName());
        } catch (GoogleJsonResponseException e) {
            int statusCode = e.getDetails().getCode();
            if (statusCode == 401 || statusCode == 403) {
                // TODO: Log a warning and continue
            } else {
                throw e;
            }
        }

        return userProfile;
    }

    private UserProfile getUserProfile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();

        if (user == null) {
            resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
            return null;
        }

        UserProfile userProfile = UserProfile.get(user);

        if (userProfile == null) {
            userProfile = createUserProfile(user);

            // Send the user to the profile page to create an account.
            if (!req.getRequestURI().startsWith(PROFILE_PATH)) {
                resp.sendRedirect(PROFILE_PATH);
                return null;
            }
        }

        // TODO: Unused?
        req.setAttribute("user", user);
        req.setAttribute("userProfile", userProfile);

        return userProfile;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        try {
            HttpServletRequest req = (HttpServletRequest) servletRequest;
            HttpServletResponse resp = (HttpServletResponse) servletResponse;
            UserProfile userProfile = getUserProfile(req, resp);

            if (userProfile == null) {
                return; // Don't continue chaining if we've performed a redirect
            }
        } catch (ClassCastException e) {
            // Ignore the exception, we're somehow running in an impossible non-HTTP servlet.
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
    }
}
