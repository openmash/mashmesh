package com.sheepdog.mashmesh.resources;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

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
                                       @FormParam("appointmentTime") String appointmentTime) {
        if (patientEmail == null || patientEmail.isEmpty() ||
                appointmentAddress == null || appointmentAddress.isEmpty() ||
                appointmentTime == null || appointmentTime.isEmpty()) {
            // TODO: Actually report errors with bean validation.
            throw new WebApplicationException(400);
        }

        TaskOptions task = withUrl("/tasks/notification/")
                .method(TaskOptions.Method.GET)
                .param("patientEmail", patientEmail)
                .param("appointmentAddress", appointmentAddress)
                .param("appointmentTime", appointmentTime);

        Queue queue = QueueFactory.getQueue(QUEUE_NAME);
        queue.add(task);

        return "OK";
    }
}
