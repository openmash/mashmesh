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

import com.google.api.client.util.Preconditions;
import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sheepdog.mashmesh.geo.GeocodeFailedException;
import com.sheepdog.mashmesh.geo.GeocodeNotFoundException;
import com.sheepdog.mashmesh.json.AvailableTimePeriodAdapter;
import com.sheepdog.mashmesh.models.AvailableTimePeriod;
import com.sheepdog.mashmesh.models.OfyService;
import com.sheepdog.mashmesh.models.UserProfile;
import com.sheepdog.mashmesh.models.VolunteerProfile;
import com.sheepdog.mashmesh.geo.GeoUtils;
import com.sheepdog.mashmesh.util.VelocityUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class EditUserProfileServlet extends HttpServlet {
    private static final String CREATE_PROFILE_TEMPLATE_PATH = "site/create.vm";
    private static final String EDIT_PROFILE_TEMPLATE_PATH = "site/edit.vm";

    private static final Logger logger = Logger.getLogger(EditUserProfileServlet.class.getCanonicalName());

    private UserProfile getUserProfile(HttpServletRequest req) throws IOException {
        return (UserProfile) req.getAttribute("userProfile");
    }

    private boolean isUncreatedUser(HttpServletRequest req, UserProfile userProfile) {
        return userProfile.getType() == UserProfile.UserType.NEW &&
            req.getParameter("userType") == null;
    }

    private void initializeUserProfile(HttpServletRequest req, UserProfile userProfile) {
        if (userProfile.getType() == UserProfile.UserType.NEW) {
            String userTypeString = req.getParameter("userType");
            Preconditions.checkNotNull(userTypeString);
            UserProfile.UserType userType = UserProfile.UserType.valueOf(userTypeString);
            userProfile.setType(userType);
        }
    }

    private boolean isAdmin() {
        return UserServiceFactory.getUserService().isUserAdmin();
    }

    private String createLogoutUrl() {
        return UserServiceFactory.getUserService().createLogoutURL("/");
    }

    private void renderCreateProfileTemplate(HttpServletResponse resp, UserProfile userProfile) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("isAdmin", isAdmin());
        context.put("logoutUrl", createLogoutUrl());
        context.put("userProfile", userProfile);

        resp.setContentType("text/html");
        Template template = VelocityUtils.getInstance().getTemplate(CREATE_PROFILE_TEMPLATE_PATH);
        template.merge(context, resp.getWriter());
    }

    private static Object popSessionAttribute(HttpServletRequest req, String attribute) {
        HttpSession session = req.getSession();
        Object value = session.getAttribute(attribute);

        if (value != null) {
            session.removeAttribute(attribute);
        }

        return value;
    }

    private Gson getGsonForAvailableTimePeriods() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(AvailableTimePeriod.class, new AvailableTimePeriodAdapter());
        return gsonBuilder.create();
    }

    private String serializeAvailableTimePeriods(List<AvailableTimePeriod> availableTimePeriods) {
        return getGsonForAvailableTimePeriods().toJson(availableTimePeriods);
    }

    private List<AvailableTimePeriod> deserializeAvailableTimePeriods(String availability) {
        Type availableTimePeriodListType = new TypeToken<List<AvailableTimePeriod>>(){}.getType();
        return getGsonForAvailableTimePeriods().fromJson(availability, availableTimePeriodListType);
    }

    private void renderEditProfileTemplate(HttpServletRequest req, HttpServletResponse resp,
                                           UserProfile userProfile, VolunteerProfile volunteerProfile)
            throws IOException {
        VelocityContext context = VelocityUtils.getContext();
        context.put("isAdmin", isAdmin());
        context.put("logoutUrl", createLogoutUrl());
        context.put("userProfile", userProfile);
        context.put("volunteerProfile", volunteerProfile);
        context.put("messageClass", popSessionAttribute(req, "flash.class"));
        context.put("message", popSessionAttribute(req, "flash.message"));

        if (userProfile.getType() == UserProfile.UserType.VOLUNTEER) {
            context.put("availability", serializeAvailableTimePeriods(volunteerProfile.getAvailableTimePeriods()));
        }

        resp.setContentType("text/html");
        Template template = VelocityUtils.getInstance().getTemplate(EDIT_PROFILE_TEMPLATE_PATH);
        template.merge(context, resp.getWriter());
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
        UserProfile userProfile = getUserProfile(req);
        VolunteerProfile volunteerProfile = VolunteerProfile.getOrCreate(userProfile);

        if (isUncreatedUser(req, userProfile)) {
            // Render the "create profile" page.
            renderCreateProfileTemplate(resp, userProfile);
        } else {
            initializeUserProfile(req, userProfile);
            renderEditProfileTemplate(req, resp, userProfile, volunteerProfile);
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
        UserProfile userProfile = getUserProfile(req);
        VolunteerProfile volunteerProfile = VolunteerProfile.getOrCreate(userProfile);

        initializeUserProfile(req, userProfile);

        boolean isValid = true;
        String fullName = req.getParameter("name");
        String email = req.getParameter("email");
        String address = req.getParameter("location"); // TODO: Fix naming conventions
        GeoPt location = null;
        try {
            location = GeoUtils.geocode(address);
        } catch (GeocodeFailedException e) {
            logger.log(Level.SEVERE, "Geocoding '" + address + "' failed", e);
            resp.setStatus(500);
            return;
        } catch (GeocodeNotFoundException e) {
            logger.log(Level.WARNING, "Invalid location: '" + address + "'", e);
            isValid = false;
        }

        String comments = req.getParameter("comments");

        userProfile.setFullName(fullName);
        userProfile.setEmail(email);
        userProfile.setAddress(address);
        userProfile.setLocation(location);
        userProfile.setComments(comments);

        if (userProfile.getType() == UserProfile.UserType.VOLUNTEER) {
            float maximumDistanceMiles = Float.parseFloat(req.getParameter("maximumDistance"));
            volunteerProfile.setMaximumDistanceMiles(maximumDistanceMiles);

            String availability = req.getParameter("availability");
            List<AvailableTimePeriod> availableTimePeriods = deserializeAvailableTimePeriods(availability);
            volunteerProfile.setAvailableTimePeriods(availableTimePeriods);

            volunteerProfile.setLocation(location);
        }

        if (!isValid) {
            resp.setStatus(400);
            renderEditProfileTemplate(req, resp, userProfile, volunteerProfile);
        } else {
            OfyService.ofy().put(userProfile);

            if (userProfile.getType() == UserProfile.UserType.VOLUNTEER) {
                volunteerProfile.updateDocument(userProfile);
                OfyService.ofy().put(volunteerProfile);
            }

            HttpSession session = req.getSession();
            session.setAttribute("flash.class", "success");
            session.setAttribute("flash.message", "Your profile has been saved");

            resp.sendRedirect(req.getRequestURI());
        }
    }
}
