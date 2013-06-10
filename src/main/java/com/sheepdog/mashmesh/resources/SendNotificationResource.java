package com.sheepdog.mashmesh.resources;

import com.sheepdog.mashmesh.models.RideRequest;
import com.sheepdog.mashmesh.tasks.SendNotificationTask;
import org.joda.time.DateTime;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

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
        RideRequest request = SendNotificationTask.createRequest(patientEmail, appointmentAddress, appointmentTime);
        SendNotificationTask.scheduleRequest(request);

        return "OK";
    }
}
