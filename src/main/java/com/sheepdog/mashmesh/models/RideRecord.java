package com.sheepdog.mashmesh.models;

import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.Unindexed;
import com.googlecode.objectify.condition.IfFalse;
import com.googlecode.objectify.condition.IfTrue;
import com.sheepdog.mashmesh.Itinerary;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import javax.persistence.Id;

@Entity
public class RideRecord {
    @Id private Long id;

    @Indexed private Key<UserProfile> volunteerUserProfileKey;
    @Unindexed private GeoPt volunteerLocation;
    @Unindexed private DateTime departureTime;

    @Indexed private Key<UserProfile> patientProfileKey;
    @Unindexed private GeoPt patientLocation;
    @Unindexed private DateTime pickupTime;

    @Indexed private DateTime arrivalTime;

    @Indexed private String appointmentAddress;
    @Unindexed private GeoPt appointmentLocation;
    @Indexed private DateTime appointmentTime;

    @Unindexed private double distanceMiles;
    @Indexed(IfTrue.class) private boolean isExportable = false;

    public RideRecord() {
    }

    public RideRecord(RideRequest rideRequest, UserProfile volunteerUserProfile, Itinerary itinerary) {
        this.volunteerUserProfileKey = volunteerUserProfile.getKey();
        this.volunteerLocation = volunteerUserProfile.getLocation();
        this.departureTime = itinerary.getDepartureTime();

        this.patientProfileKey = rideRequest.getPatientUserProfileKey();
        this.patientLocation = rideRequest.getPatientLocation();
        this.pickupTime = itinerary.getPickupTime();

        this.arrivalTime = itinerary.getArrivalTime();

        this.appointmentAddress = rideRequest.getAppointmentAddress();
        this.appointmentLocation = rideRequest.getAppointmentLocation();
        this.appointmentTime = rideRequest.getAppointmentTime();

        this.distanceMiles = itinerary.getDistanceMiles();
    }

    public Long getId() {
        return id;
    }

    public Key<RideRecord> getKey() {
        return new Key<RideRecord>(RideRecord.class, id);
    }

    public Key<UserProfile> getVolunteerUserProfileKey() {
        return volunteerUserProfileKey;
    }

    public void setVolunteerUserProfileKey(Key<UserProfile> volunteerUserProfileKey) {
        this.volunteerUserProfileKey = volunteerUserProfileKey;
    }

    public GeoPt getVolunteerLocation() {
        return volunteerLocation;
    }

    public void setVolunteerLocation(GeoPt volunteerLocation) {
        this.volunteerLocation = volunteerLocation;
    }

    public DateTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(DateTime departureTime) {
        this.departureTime = departureTime;
    }

    public Key<UserProfile> getPatientProfileKey() {
        return patientProfileKey;
    }

    public void setPatientProfileKey(Key<UserProfile> patientProfileKey) {
        this.patientProfileKey = patientProfileKey;
    }

    public GeoPt getPatientLocation() {
        return patientLocation;
    }

    public void setPatientLocation(GeoPt patientLocation) {
        this.patientLocation = patientLocation;
    }

    public DateTime getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(DateTime pickupTime) {
        this.pickupTime = pickupTime;
    }

    public DateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(DateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getAppointmentAddress() {
        return appointmentAddress;
    }

    public void setAppointmentAddress(String appointmentAddress) {
        this.appointmentAddress = appointmentAddress;
    }

    public GeoPt getAppointmentLocation() {
        return appointmentLocation;
    }

    public void setAppointmentLocation(GeoPt appointmentLocation) {
        this.appointmentLocation = appointmentLocation;
    }

    public DateTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(DateTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public double getDistanceMiles() {
        return distanceMiles;
    }

    public void setDistanceMiles(double distanceMiles) {
        this.distanceMiles = distanceMiles;
    }

    public double getTripMinutes() {
        return new Duration(departureTime, arrivalTime).getStandardMinutes();
    }

    public boolean isExportable() {
        return isExportable;
    }

    public void setExportable(boolean isExportable) {
        this.isExportable = isExportable;
    }

    public static QueryResultIterable<RideRecord> getExportableRecords() {
        return OfyService.ofy().query(RideRecord.class)
                .filter("isExportable", true)
                .fetch();
    }
}
