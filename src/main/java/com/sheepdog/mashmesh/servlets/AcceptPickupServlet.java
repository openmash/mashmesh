package com.sheepdog.mashmesh.servlets;

import com.google.api.client.util.Preconditions;
import com.google.appengine.api.users.UserServiceFactory;
import com.googlecode.objectify.Objectify;
import com.sheepdog.mashmesh.PickupNotification;
import com.sheepdog.mashmesh.models.*;
import com.sheepdog.mashmesh.util.VelocityUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AcceptPickupServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(AcceptPickupServlet.class.getCanonicalName());

    private static final String ACCEPT_PICKUP_TEMPLATE_PATH = "site/acceptPickup.vm";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Objectify rideRequestOfy = OfyService.transactionOfy();

        try {
            String rideRequestId = req.getParameter("rideRequestId");
            Preconditions.checkNotNull(rideRequestId);

            RideRequest rideRequest = rideRequestOfy.get(RideRequest.class, Long.parseLong(rideRequestId));
            UserProfile userProfile = (UserProfile) req.getAttribute("userProfile");

            VolunteerProfile volunteerProfile = rideRequest.getPendingVolunteerProfile();

            // Make sure that we're not trying to answer a pickup request meant for someone else.
            if (!volunteerProfile.getUserId().equals(userProfile.getUserId())) {
                resp.setStatus(403);
                // TODO: Explanatory page
                return;
            }

            // TODO: Centralize this logic with the rest of the notification logic
            RideRecord rideRecord = rideRequest.getPendingRideRecord();
            rideRecord.setExportable(true);
            OfyService.ofy().put(rideRecord);

            PickupNotification.sendPatientNotification(rideRequest, userProfile);

            rideRequestOfy.delete(rideRequest);
            rideRequestOfy.getTxn().commit();

            // TODO: Make sure that pickup requests timeout and trigger a rejection.

            DateTimeFormatter dateFormatter = DateTimeFormat.fullDate();
            DateTimeFormatter timeFormatter = DateTimeFormat.shortTime();

            VelocityContext context = new VelocityContext();
            context.put("userProfile", userProfile);
            context.put("isAdmin", UserServiceFactory.getUserService().isUserAdmin());
            context.put("patientProfile", rideRequest.getPatientProfile());
            context.put("appointmentAddress", rideRequest.getAppointmentAddress());
            context.put("appointmentTime", timeFormatter.print(rideRequest.getAppointmentTime()));
            context.put("appointmentDate", dateFormatter.print(rideRequest.getAppointmentTime()));
            context.put("pickupTime", timeFormatter.print(rideRecord.getPickupTime()));

            resp.setContentType("text/html");
            Template template = VelocityUtils.getInstance().getTemplate(ACCEPT_PICKUP_TEMPLATE_PATH);
            template.merge(context, resp.getWriter());
        } catch (MessagingException e) {
            logger.log(Level.SEVERE, "Failed to send patient notification", e);
            resp.setStatus(500);
        } finally {
            if (rideRequestOfy.getTxn().isActive()) {
                rideRequestOfy.getTxn().rollback();
            }
        }
    }
}
