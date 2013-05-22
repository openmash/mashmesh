/**
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sheepdog.mashmesh.servlets;

import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.common.base.Preconditions;
import com.sheepdog.mashmesh.models.OfyService;
import com.sheepdog.mashmesh.models.UserProfile;
import com.sheepdog.mashmesh.models.VolunteerProfile;
import com.sheepdog.mashmesh.util.GeoUtils;
import com.sheepdog.mashmesh.util.VelocityConfiguration;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserProfileServlet extends HttpServlet {
    private static final String PATIENT_TEMPLATE_PATH = "WEB-INF/templates/notifications/profile.vm";

    private User getUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();

        if (user == null) {
            resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
        }

        return user;
    }

    private UserProfile getUserProfile(HttpServletRequest req, User user) throws IOException {
        UserProfile userProfile = UserProfile.getOrCreate(user);

        if (userProfile.getType() == UserProfile.UserType.NEW) {
            String userTypeString = req.getParameter("userType");
            Preconditions.checkNotNull(userTypeString);
            UserProfile.UserType userType = UserProfile.UserType.valueOf(userTypeString);
            userProfile.setType(userType);
        }

        return userProfile;
    }

    private void renderTemplate(HttpServletResponse resp, UserProfile userProfile, VolunteerProfile volunteerProfile)
            throws IOException {
        String logoutUrl = UserServiceFactory.getUserService().createLogoutURL("/");

        VelocityContext context = new VelocityContext();
        context.put("logoutUrl", logoutUrl);
        context.put("userProfile", userProfile);
        context.put("volunteerProfile", volunteerProfile);

        Template template = VelocityConfiguration.getInstance().getTemplate(PATIENT_TEMPLATE_PATH);
        template.merge(context, resp.getWriter());
        resp.setContentType("text/html");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
        User user = getUser(req, resp);

        if (user == null) {
            return;
        }

        UserProfile userProfile = getUserProfile(req, user);
        VolunteerProfile volunteerProfile = VolunteerProfile.getOrCreate(user);
        renderTemplate(resp, userProfile, volunteerProfile);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
        User user = getUser(req, resp);

        if (user == null) {
            return;
        }

        UserProfile userProfile = getUserProfile(req, user);
        VolunteerProfile volunteerProfile = VolunteerProfile.getOrCreate(user);

        String fullName = req.getParameter("name");
        String email = req.getParameter("email");
        String address = req.getParameter("location"); // TODO: Fix naming conventions
        GeoPt location = GeoUtils.geocode(address);
        String comments = req.getParameter("comments");

        userProfile.setFullName(fullName);
        userProfile.setEmail(email);
        userProfile.setAddress(address);
        userProfile.setLocation(location);
        userProfile.setComments(comments);

        if (userProfile.getType() == UserProfile.UserType.VOLUNTEER) {
            float maximumDistanceMiles = Float.parseFloat(req.getParameter("maximumDistance"));
            volunteerProfile.setMaximumDistanceMiles(maximumDistanceMiles);
            volunteerProfile.setLocation(location);
        }

        boolean isValid = true; // TODO: Validation

        if (!isValid) {
            resp.setStatus(400);
            renderTemplate(resp, userProfile, volunteerProfile);
        } else {
            OfyService.ofy().save().entity(userProfile).now();

            if (userProfile.getType() == UserProfile.UserType.VOLUNTEER) {
                OfyService.ofy().save().entity(volunteerProfile).now();
                volunteerProfile.updateDocument(userProfile);
            }

            resp.sendRedirect(req.getRequestURI());
        }
    }
}
