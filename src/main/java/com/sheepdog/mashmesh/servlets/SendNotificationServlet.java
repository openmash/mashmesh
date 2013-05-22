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

import com.google.appengine.api.datastore.*;
import com.googlecode.objectify.ObjectifyService;
import com.sheepdog.mashmesh.EmailNotifier;
import com.sheepdog.mashmesh.Itinerary;
import com.sheepdog.mashmesh.models.UserProfile;
import com.sheepdog.mashmesh.models.VolunteerProfile;
import com.sheepdog.mashmesh.util.GeoUtils;
import com.sheepdog.mashmesh.util.VelocityConfiguration;
import org.apache.velocity.app.VelocityEngine;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SendNotificationServlet extends HttpServlet {
    private static final DateTimeFormatter iso8601Parser = ISODateTimeFormat.dateTimeParser().withOffsetParsed();

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
        String patientEmail = req.getParameter("patientEmail");
        // TODO: Pull this from a calendar invitation
        String appointmentAddress = req.getParameter("appointmentAddress");
        String arriveAt = req.getParameter("arriveAt");
        DateTime arrivalDateTime = iso8601Parser.parseDateTime(arriveAt);

        GeoPt appointmentGeoPt = GeoUtils.geocode(appointmentAddress);

        UserProfile patientProfile = UserProfile.getByEmail(patientEmail);
        VolunteerProfile volunteerProfile = VolunteerProfile.getEligibleVolunteer(
                patientProfile.getLocation(), appointmentGeoPt, arrivalDateTime);
        UserProfile volunteerUserProfile = volunteerProfile.getUserProfile();

        EmailNotifier emailNotifier = new EmailNotifier();

        try {
            Itinerary itinerary = Itinerary.fetch(volunteerUserProfile.getAddress(), appointmentAddress,
                    patientProfile.getAddress(), arrivalDateTime.minusMinutes(5));

            // TODO: Handle data races
            volunteerProfile.addAppointmentTime(itinerary.getDepartureTime(), itinerary.getArrivalTime());
            ObjectifyService.ofy().save().entity(volunteerProfile).now();

            String html = emailNotifier.renderNotification(patientProfile, volunteerUserProfile,
                    itinerary, appointmentAddress);
            emailNotifier.sendEmail(volunteerUserProfile.getEmail(), html);

            resp.setStatus(200);
            resp.setContentType("text/html");
            resp.getWriter().write(html);
        } catch (URISyntaxException e) {
            e.printStackTrace(); // TODO
        } catch (MessagingException e) {
            e.printStackTrace(); // TODO
        }
    }
}
