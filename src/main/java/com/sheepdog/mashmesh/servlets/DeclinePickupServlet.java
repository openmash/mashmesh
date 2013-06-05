package com.sheepdog.mashmesh.servlets;

import com.google.api.client.util.Preconditions;
import com.google.appengine.api.users.UserServiceFactory;
import com.googlecode.objectify.Objectify;
import com.sheepdog.mashmesh.models.*;
import com.sheepdog.mashmesh.tasks.SendNotificationServlet;
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

            SendNotificationServlet.scheduleRequest(rideRequest);

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
