package com.sheepdog.mashmesh.resources;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.sheepdog.mashmesh.models.UserProfile;
import com.sheepdog.mashmesh.util.CalendarUtils;
import org.joda.time.DateTime;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

@Path("/eventNotification")
public class SendEventNotificationResource {
    private static final String QUEUE_NAME = "notifications";

    private void verifyPatientEmail(String patientEmail) {
        UserProfile patientProfile = UserProfile.getByEmail(patientEmail);
        if (patientProfile == null || patientProfile.getType() != UserProfile.UserType.PATIENT) {
            Response response = Response.status(Response.Status.NOT_FOUND)
                    .entity("No such patient: " + patientEmail)
                    .build();
            throw new WebApplicationException(response);
        }
    }

    private Event getCalendarEvent(String patientEmail, String calendarId, String eventId) {
        Calendar calendar = CalendarUtils.getCalendar(patientEmail);
        try {
            return calendar.events().get(calendarId, eventId).execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getDetails().getCode() == 404) {
                String message = String.format("No such calendar event: calendarId=%s, eventId=%s",
                        calendarId, eventId);
                Response response = Response.status(Response.Status.NOT_FOUND)
                        .entity(message)
                        .build();
                throw new WebApplicationException(response);
            } else {
                throw new WebApplicationException(e);
            }
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }
    }

    @POST
    @Path("{patientEmail}")
    @Produces({MediaType.TEXT_PLAIN})
    public String scheduleNotification(@PathParam("patientEmail") String patientEmail,
                                       @FormParam("calendarId") String calendarId,
                                       @FormParam("eventId") String eventId) throws IOException {
        verifyPatientEmail(patientEmail);
        Event event = getCalendarEvent(patientEmail, calendarId, eventId);
        String appointmentTime = event.getStart().getDateTime().toStringRfc3339();

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
