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
import com.google.appengine.api.users.UserServiceFactory;
import com.googlecode.objectify.Objectify;
import com.sheepdog.mashmesh.models.*;
import com.sheepdog.mashmesh.tasks.SendNotificationTask;
import com.sheepdog.mashmesh.util.VelocityUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DeclinePickupServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // TODO: Tie these together into an XG tranasction
        Objectify rideRequestOfy = OfyService.transactionOfy();
        Objectify volunteerProfileOfy = OfyService.transactionOfy();

        try {
            String rideRequestId = req.getParameter("rideRequestId");
            Preconditions.checkNotNull(rideRequestId);

            RideRequest rideRequest = rideRequestOfy.get(RideRequest.class, Long.parseLong(rideRequestId));
            UserProfile userProfile = (UserProfile) req.getAttribute("userProfile");

            VolunteerProfile volunteerProfile = volunteerProfileOfy.get(rideRequest.getPendingVolunteerProfileKey());

            // Make sure that we're not trying to answer a pickup request meant for someone else.
            if (!volunteerProfile.getUserId().equals(userProfile.getUserId())) {
                resp.setStatus(403);
                // TODO: Explanatory page
                return;
            }

            // TODO: Centralize this logic with the rest of the notification logic
            RideRecord rideRecord = rideRequest.getPendingRideRecord();
            OfyService.ofy().delete(rideRecord);

            rideRequest.setPendingRideRecordKey(null);
            rideRequest.setPendingVolunteerProfileKey(null);
            rideRequest.addDeclinedVolunteerUserId(volunteerProfile.getUserId());
            rideRequestOfy.put(rideRequest);

            volunteerProfile.removeAppointmentTime(rideRequest);
            volunteerProfileOfy.put(volunteerProfile);

            rideRequestOfy.getTxn().commit();
            volunteerProfileOfy.getTxn().commit();

            SendNotificationTask.scheduleRequest(rideRequest);

            VelocityContext context = new VelocityContext();
            context.put("userProfile", userProfile);
            context.put("isAdmin", UserServiceFactory.getUserService().isUserAdmin());
            context.put("patientProfile", rideRequest.getPatientProfile());
            context.put("appointmentAddress", rideRequest.getAppointmentAddress());

            resp.setContentType("text/html");
            Template template = VelocityUtils.getInstance().getTemplate("site/declinePickup.vm");
            template.merge(context, resp.getWriter());
        } finally {
            if (rideRequestOfy.getTxn().isActive()) {
                rideRequestOfy.getTxn().rollback();
            }

            if (volunteerProfileOfy.getTxn().isActive()) {
                volunteerProfileOfy.getTxn().rollback();
            }
        }
    }
}
