package com.sheepdog.mashmesh.resources;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.sheepdog.mashmesh.util.CalendarUtils;
import org.joda.time.DateTime;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import java.io.IOException;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

@Path("/eventNotification")
public class SendEventNotificationResource {
    private static final String QUEUE_NAME = "notifications";

    @POST
    @Produces({MediaType.TEXT_PLAIN})
    public String scheduleNotification(@FormParam("patientEmail") String patientEmail,
                                       @FormParam("calendarId") String calendarId,
                                       @FormParam("eventId") String eventId) throws IOException {
        Calendar calendar = CalendarUtils.getCalendar(patientEmail);
        Event event = calendar.events().get(calendarId, eventId).execute();
        String appointmentTime = event.getStart().getDateTime().toStringRfc3339();

        // TODO: Validate the email address before passing off to the queue.

        TaskOptions task = withUrl("/tasks/notification/")
                .method(TaskOptions.Method.GET)
                .param("patientEmail", patientEmail)
                .param("appointmentAddress", event.getLocation())
                .param("appointmentTime", appointmentTime);

        Queue queue = QueueFactory.getQueue(QUEUE_NAME);
        queue.add(task);

        return "OK";
    }
}
