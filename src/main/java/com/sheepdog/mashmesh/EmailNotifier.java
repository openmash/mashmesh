package com.sheepdog.mashmesh;


import com.sheepdog.mashmesh.models.UserProfile;
import com.sheepdog.mashmesh.util.ApplicationConfiguration;
import com.sheepdog.mashmesh.util.VelocityConfiguration;
import org.apache.http.client.utils.URIBuilder;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Properties;

// TODO: The structure of this class doesn't really make any sense at all...
public class EmailNotifier {
    // TODO: Pull out these constants
    private static final int MAXIMUM_URL_LENGTH = 2048;
    private static final String STATIC_MAPS_ENDPOINT_URL = "http://maps.googleapis.com/maps/api/staticmap";
    private static final String DYNAMIC_MAPS_URL = "https://maps.google.com/maps";

    private static final String DIRECTIONS_TEMPLATE_PATH = "WEB-INF/templates/notifications/directions.vm";

    private static final Session session = Session.getDefaultInstance(new Properties());

    private URI createStaticMapUri(String fromLatLng, String toLatLng, String viaLatLng, String polyline)
            throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(STATIC_MAPS_ENDPOINT_URL);

        uriBuilder.addParameter("sensor", "false");
        uriBuilder.addParameter("key", ApplicationConfiguration.getMapsApiKey());
        uriBuilder.addParameter("size", "600x400");
        uriBuilder.addParameter("markers", "color:green|label:A|" + fromLatLng);
        uriBuilder.addParameter("markers", "color:blue|label:B|" + viaLatLng);
        uriBuilder.addParameter("markers", "color:blue|label:C|" + toLatLng);
        uriBuilder.addParameter("language", "en_US");
        uriBuilder.addParameter("maptype", "roadmap");

        if (polyline != null) {
            uriBuilder.addParameter("path", "weight:4|color:blue|enc:" + polyline);
        }

        return uriBuilder.build();
    }

    private URI createDynamicMapUri(String fromLocation, String toLocation, String viaLocation) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(DYNAMIC_MAPS_URL);
        uriBuilder.addParameter("saddr", fromLocation);
        uriBuilder.addParameter("daddr", viaLocation + " to:" + toLocation);
        return uriBuilder.build();
    }

    private static String formatDateTime(DateTimeFormatter formatter, DateTime dateTime) throws IOException {
        StringWriter writer = new StringWriter();
        formatter.printTo(writer, dateTime);
        return writer.toString();
    }

    public String renderNotification(UserProfile patientProfile, UserProfile volunteerUserProfile,
                                     Itinerary itinerary, String appointmentAddress)
            throws URISyntaxException, IOException {
        DateTimeFormatter dateFormatter = DateTimeFormat.fullDate();
        DateTimeFormatter timeFormatter = DateTimeFormat.shortTime();

        String fromLatLng = itinerary.getStartLatLng();
        String toLatLng = itinerary.getEndLatLng();
        String viaLatLng = itinerary.getStartLeg().getEndLatLng();

        URI staticMapUri = createStaticMapUri(fromLatLng, toLatLng, viaLatLng, itinerary.getOverviewPolyline());
        URI dynamicMapUri = createDynamicMapUri(volunteerUserProfile.getAddress(), appointmentAddress,
                patientProfile.getAddress());

        if (staticMapUri.toString().length() > MAXIMUM_URL_LENGTH) {
            // If the polyline is too long, we need to eliminate it in order for the map to render
            staticMapUri = createStaticMapUri(fromLatLng, toLatLng, viaLatLng, null);
        }

        Context context = new VelocityContext();
        context.put("volunteerUserProfile", volunteerUserProfile);
        context.put("patientProfile", patientProfile);
        context.put("appointmentDate", formatDateTime(dateFormatter, itinerary.getArrivalTime()));
        context.put("appointmentTime", formatDateTime(timeFormatter, itinerary.getArrivalTime()));
        context.put("appointmentAddress", appointmentAddress);
        context.put("departureTime", formatDateTime(timeFormatter, itinerary.getDepartureTime()));
        context.put("arrivalTime", formatDateTime(timeFormatter, itinerary.getArrivalTime()));
        context.put("pickupTime", formatDateTime(timeFormatter, itinerary.getLegs().get(0).getArrivalTime()));
        context.put("staticMapUrl", staticMapUri.toString());
        context.put("dynamicMapUrl", dynamicMapUri.toString());
        context.put("directionLegs", itinerary.getLegs());

        Template template = VelocityConfiguration.getInstance().getTemplate(DIRECTIONS_TEMPLATE_PATH);
        StringWriter writer = new StringWriter();
        template.merge(context, writer);

        return writer.toString();
    }

    public void sendEmail(String recipient, String htmlMessage) throws MessagingException {
        InternetAddress sender = new InternetAddress(ApplicationConfiguration.getNotificationEmailSender());
        InternetAddress receiver = new InternetAddress(recipient);
        Message message = new MimeMessage(session);
        message.setFrom(sender);
        message.addRecipient(Message.RecipientType.TO, receiver);
        message.setSubject("Appointment Notification"); // TODO: Create a relevant subject
        message.setContent(htmlMessage, "text/html");
        Transport.send(message);
    }
}
