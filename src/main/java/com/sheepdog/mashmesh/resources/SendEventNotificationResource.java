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
package com.sheepdog.mashmesh.resources;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.sheepdog.mashmesh.models.UserProfile;
import com.sheepdog.mashmesh.util.GoogleApiUtils;

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
        Calendar calendar = GoogleApiUtils.getCalendar(patientEmail);
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
