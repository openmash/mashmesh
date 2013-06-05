package com.sheepdog.mashmesh.resources;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.sheepdog.mashmesh.models.RideRequest;
import com.sheepdog.mashmesh.tasks.SendNotificationServlet;
import org.joda.time.DateTime;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

@Path("/notification")
public class SendNotificationResource {
    private static final String QUEUE_NAME = "notifications";

    @POST
    @Produces({MediaType.TEXT_PLAIN})
    public String scheduleNotification(@FormParam("patientEmail") String patientEmail,
                                       @FormParam("appointmentAddress") String appointmentAddress,
                                       @FormParam("appointmentTime") String appointmentTimeRfc339) {
        if (patientEmail == null || patientEmail.isEmpty() ||
                appointmentAddress == null || appointmentAddress.isEmpty() ||
                appointmentTimeRfc339 == null || appointmentTimeRfc339.isEmpty()) {
            // TODO: Actually report errors with bean validation.
            throw new WebApplicationException(400);
        }

        DateTime appointmentTime = DateTime.parse(appointmentTimeRfc339);
        RideRequest request = SendNotificationServlet.createRequest(patientEmail, appointmentAddress, appointmentTime);
        SendNotificationServlet.scheduleRequest(request);

        return "OK";
    }
}
