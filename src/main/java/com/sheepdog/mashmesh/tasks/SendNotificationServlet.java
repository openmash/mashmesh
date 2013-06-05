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

package com.sheepdog.mashmesh.tasks;

import com.google.api.client.util.Preconditions;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.googlecode.objectify.Objectify;
import com.sheepdog.mashmesh.PickupNotification;
import com.sheepdog.mashmesh.Itinerary;
import com.sheepdog.mashmesh.VolunteerLocator;
import com.sheepdog.mashmesh.geo.GeocodeFailedException;
import com.sheepdog.mashmesh.geo.GeocodeNotFoundException;
import com.sheepdog.mashmesh.models.*;
import com.sheepdog.mashmesh.geo.GeoUtils;
import org.joda.time.DateTime;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

public class SendNotificationServlet extends HttpServlet {
    private static final String TASK_URL_PATH = "/tasks/notification/";
    private static final String QUEUE_NAME = "notifications";

    private static Logger logger = Logger.getLogger(SendNotificationServlet.class.getCanonicalName());

    public static RideRequest createRequest(String patientEmail, String appointmentAddress, DateTime appointmentTime) {
        UserProfile patientProfile = UserProfile.getByEmail(patientEmail);
        Preconditions.checkNotNull(patientProfile);

        RideRequest rideRequest = new RideRequest();

        rideRequest.setPatientUserProfileKey(patientProfile.getKey());
        rideRequest.setAppointmentAddress(appointmentAddress);
        rideRequest.setAppointmentTime(appointmentTime);

        try {
            rideRequest.setAppointmentLocation(GeoUtils.geocode(appointmentAddress));
        } catch (GeocodeFailedException e) {
            logger.log(Level.SEVERE, "Failed to fetch geocode for " + appointmentAddress, e);
            return null; // TODO: Exception
        } catch (GeocodeNotFoundException e) {
            logger.log(Level.WARNING, "Address " + appointmentAddress + " does not exist");
            return null;
        }

        OfyService.ofy().put(rideRequest);
        return rideRequest;
    }

    public static void scheduleRequest(RideRequest rideRequest) {
        TaskOptions task = withUrl(TASK_URL_PATH)
                .method(TaskOptions.Method.POST)
                .param("rideRequestId", Long.toString(rideRequest.getId()));

        Queue queue = QueueFactory.getQueue(QUEUE_NAME);
        queue.add(task);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doPost(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String rideRequestId = req.getParameter("rideRequestId");
        Preconditions.checkNotNull(rideRequestId);

        Objectify rideRecordOfy = OfyService.transactionOfy();

        RideRequest rideRequest = rideRecordOfy.get(RideRequest.class, Long.parseLong(rideRequestId));
        UserProfile patientProfile = rideRequest.getPatientProfile();

        if (patientProfile.getType() != UserProfile.UserType.PATIENT) {
            logger.warning("Non-patient user profile found: " + patientProfile.getUserId());
            rideRecordOfy.getTxn().rollback();
            return;
        }

        VolunteerLocator volunteerLocator = new VolunteerLocator(rideRequest);
        VolunteerProfile volunteerProfile = volunteerLocator.getEligibleVolunteer();

        try {
            resp.setStatus(200);
            resp.setContentType("text/html");

            if (volunteerProfile == null) {
                PickupNotification.sendFailureNotification(rideRequest);
                String html = PickupNotification.renderFailureNotification(rideRequest);
                resp.getWriter().write(html);
                return;
            }

            UserProfile volunteerUserProfile = volunteerProfile.getUserProfile();
            Itinerary itinerary = Itinerary.fetch(
                    volunteerUserProfile.getAddress(),
                    rideRequest.getAppointmentAddress(),
                    rideRequest.getPatientAddress(),
                    rideRequest.getAppointmentTime().minusMinutes(5));

            PickupNotification pickupNotification = new PickupNotification(rideRequest, volunteerUserProfile, itinerary);
            pickupNotification.sendVolunteerNotification();

            // TODO: Handle data races
            // TODO: Include time to go home after the appointment
            volunteerProfile.addAppointmentTime(rideRequest, itinerary.getDepartureTime(), itinerary.getArrivalTime());
            OfyService.ofy().put(volunteerProfile);

            RideRecord rideRecord = new RideRecord(rideRequest, volunteerUserProfile, itinerary);
            OfyService.ofy().put(rideRecord);

            rideRequest.setPendingVolunteerProfileKey(volunteerProfile.getKey());
            rideRequest.setPendingRideRecordKey(rideRecord.getKey());
            rideRecordOfy.put(rideRequest); // TODO: Handle concurrent modification
            rideRecordOfy.getTxn().commit();

            String html = pickupNotification.renderVolunteerNotification();
            resp.getWriter().write(html);
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, "URI syntax error building map URL: ", e);
        } catch (MessagingException e) {
            // TODO: Add a retry strategy if sending email fails
            logger.log(Level.SEVERE, "Email sending error: ", e);
        } finally {
            if (rideRecordOfy.getTxn().isActive()) {
                rideRecordOfy.getTxn().rollback();
            }
        }
    }
}
