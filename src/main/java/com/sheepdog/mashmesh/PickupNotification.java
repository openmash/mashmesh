package com.sheepdog.mashmesh;


import com.sheepdog.mashmesh.models.RideRequest;
import com.sheepdog.mashmesh.models.UserProfile;
import com.sheepdog.mashmesh.polyline.Point;
import com.sheepdog.mashmesh.polyline.PolylineDecoder;
import com.sheepdog.mashmesh.polyline.PolylineEncoder;
import com.sheepdog.mashmesh.util.ApplicationConfiguration;
import com.sheepdog.mashmesh.util.EmailUtils;
import com.sheepdog.mashmesh.util.VelocityUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.mail.MessagingException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

// TODO: The structure of this class doesn't really make any sense at all...
public class PickupNotification {
    // TODO: Pull out these constants
    private static final int MAXIMUM_URL_LENGTH = 2048;
    private static final String STATIC_MAPS_ENDPOINT_URL = "https://maps.googleapis.com/maps/api/staticmap";
    private static final String DYNAMIC_MAPS_URL = "https://maps.google.com/maps";

    public static final String VOLUNTEER_NOTIFICATION_TEMPLATE_PATH = "notifications/volunteerDirections.vm";
    public static final String PATIENT_NOTIFICATION_TEMPLATE_PATH = "notifications/patientPickup.vm";
    private static final String FAILURE_NOTIFICATION_TEMPLATE_PATH = "notifications/failure.vm";

    // TODO: Would we need to inject these?
    private static final DateTimeFormatter dateFormatter = DateTimeFormat.fullDate();
    private static final DateTimeFormatter timeFormatter = DateTimeFormat.shortTime();

    private final RideRequest rideRequest;
    private final UserProfile volunteerUserProfile;
    private final Itinerary itinerary;

    public PickupNotification(RideRequest rideRequest, UserProfile volunteerUserProfile, Itinerary itinerary) {
        this.rideRequest = rideRequest;
        this.volunteerUserProfile = volunteerUserProfile;
        this.itinerary = itinerary;
    }

    private UserProfile getPatientProfile() {
        return rideRequest.getPatientProfile();
    }

    private String getAppointmentAddress() {
        return rideRequest.getAppointmentAddress();
    }

    private URI createStaticMapUri(String overviewPolyline) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(STATIC_MAPS_ENDPOINT_URL);

        uriBuilder.addParameter("sensor", "false");
        uriBuilder.addParameter("key", ApplicationConfiguration.getApiKey());
        uriBuilder.addParameter("size", "600x400");
        uriBuilder.addParameter("markers", "color:green|label:A|" + itinerary.getStartLatLng());
        uriBuilder.addParameter("markers", "color:blue|label:B|" + itinerary.getPickupLatLng());
        uriBuilder.addParameter("markers", "color:blue|label:C|" + itinerary.getEndLatLng());
        uriBuilder.addParameter("language", "en_US");
        uriBuilder.addParameter("maptype", "roadmap");

        if (!overviewPolyline.isEmpty()) {
            uriBuilder.addParameter("path", "weight:4|color:blue|enc:" + overviewPolyline);
        }

        return uriBuilder.build();
    }

    private URI createDynamicMapUri() throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(DYNAMIC_MAPS_URL);
        uriBuilder.addParameter("saddr", volunteerUserProfile.getAddress());
        uriBuilder.addParameter("daddr", getPatientProfile().getAddress() + " to:" + getAppointmentAddress());
        return uriBuilder.build();
    }

    private static String formatTime(DateTime dateTime) throws IOException {
        return timeFormatter.print(dateTime);
    }

    private static String formatDate(DateTime dateTime) throws IOException {
        return dateFormatter.print(dateTime);
    }

    private static String getAppointmentSummary(String appointmentAddress, DateTime dateTime) throws IOException {
        return String.format("%s on %s at %s",
                appointmentAddress, formatDate(dateTime), formatTime(dateTime));
    }

    public String renderVolunteerNotification() throws IOException, URISyntaxException {
        URI staticMapUri = createStaticMapUri(itinerary.getOverviewPolyline());
        URI dynamicMapUri = createDynamicMapUri();

        // If the polyline is too long, we need to reduce it in order for the map to render
        if (staticMapUri.toString().length() > MAXIMUM_URL_LENGTH) {
            String overviewPolyline = itinerary.getOverviewPolyline();
            double minimumPolylineDistance = PolylineEncoder.DEFAULT_MINIMUM_DISTANCE;
            List<Point> points = new PolylineDecoder(overviewPolyline).getPoints();

            while (staticMapUri.toString().length() > MAXIMUM_URL_LENGTH) {
                PolylineEncoder polylineEncoder = new PolylineEncoder(minimumPolylineDistance);
                List<Point> filteredPoints = polylineEncoder.filterPoints(points);
                String filteredOverviewPolyline = polylineEncoder.encodePoints(filteredPoints);
                staticMapUri = createStaticMapUri(filteredOverviewPolyline);
                minimumPolylineDistance *= 2;
            }
        }

        String baseUrl = ApplicationConfiguration.getBaseUrl() + "/view";
        String acceptUrl = baseUrl + "/acceptPickup/?rideRequestId=" + rideRequest.getId();
        String declineUrl = baseUrl + "/declinePickup/?rideRequestId=" + rideRequest.getId();

        Context context = new VelocityContext();
        context.put("volunteerUserProfile", volunteerUserProfile);
        context.put("patientProfile", getPatientProfile());
        context.put("appointmentDate", formatDate(itinerary.getArrivalTime()));
        context.put("appointmentTime", formatTime(itinerary.getArrivalTime()));
        context.put("appointmentAddress", getAppointmentAddress());
        context.put("departureTime", formatTime(itinerary.getDepartureTime()));
        context.put("arrivalTime", formatTime(itinerary.getArrivalTime()));
        context.put("pickupTime", formatTime(itinerary.getPickupTime()));
        context.put("rideRequestId", rideRequest.getId());
        context.put("staticMapUrl", staticMapUri.toString());
        context.put("dynamicMapUrl", dynamicMapUri.toString());
        context.put("directionLegs", itinerary.getLegs());
        context.put("acceptUrl", acceptUrl);
        context.put("declineUrl", declineUrl);

        return VelocityUtils.renderTemplateToString(VOLUNTEER_NOTIFICATION_TEMPLATE_PATH, context);
    }

    public void sendVolunteerNotification() throws IOException, URISyntaxException, MessagingException {
        String summary = getAppointmentSummary(getAppointmentAddress(), itinerary.getArrivalTime());
        String subject = "Appointment Pickup: " + summary;
        String volunteerNotification = renderVolunteerNotification();
        EmailUtils.sendEmail(volunteerUserProfile.getEmail(), subject, volunteerNotification);
    }

    public static void sendPatientNotification(RideRequest rideRequest, UserProfile volunteerUserProfile)
            throws IOException, MessagingException {
        Context context = new VelocityContext();
        context.put("patientProfile", rideRequest.getPatientProfile());
        context.put("appointmentAddress", rideRequest.getAppointmentAddress());
        context.put("appointmentDate", formatDate(rideRequest.getAppointmentTime()));
        context.put("appointmentTime", formatTime(rideRequest.getAppointmentTime()));
        context.put("volunteerUserProfile", volunteerUserProfile);
        context.put("pickupTime", formatTime(rideRequest.getPendingRideRecord().getPickupTime()));

        String summary = getAppointmentSummary(rideRequest.getAppointmentAddress(), rideRequest.getAppointmentTime());
        String subject = "Appointment Pickup: " + summary;
        String templatePath = PATIENT_NOTIFICATION_TEMPLATE_PATH;
        String volunteerNotification = VelocityUtils.renderTemplateToString(templatePath, context);
        EmailUtils.sendEmail(rideRequest.getPatientProfile().getEmail(), subject, volunteerNotification);
    }

    public static String renderFailureNotification(RideRequest rideRequest) throws IOException {
        Context context = new VelocityContext();
        context.put("patientProfile", rideRequest.getPatientProfile());
        context.put("appointmentAddress", rideRequest.getAppointmentAddress());
        context.put("appointmentDate", formatDate(rideRequest.getAppointmentTime()));
        context.put("appointmentTime", formatTime(rideRequest.getAppointmentTime()));
        return VelocityUtils.renderTemplateToString(FAILURE_NOTIFICATION_TEMPLATE_PATH, context);
    }

    public static void sendFailureNotification(RideRequest rideRequest) throws IOException, MessagingException {
        String summary = getAppointmentSummary(rideRequest.getAppointmentAddress(), rideRequest.getAppointmentTime());
        String subject = "No Pickup Available: " + summary;
        String patientNotification = renderFailureNotification(rideRequest);
        EmailUtils.sendEmail(rideRequest.getPatientProfile().getEmail(), subject, patientNotification, "admins");
    }
}