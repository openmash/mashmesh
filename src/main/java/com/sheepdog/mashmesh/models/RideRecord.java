package com.sheepdog.mashmesh.models;

import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.Unindexed;
import com.googlecode.objectify.condition.IfFalse;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import javax.persistence.Id;
import java.util.Collection;

@Entity
public class RideRecord {
    @Id private Long id;

    @Indexed private Key<UserProfile> volunteerUserProfile;
    @Unindexed private GeoPt volunteerLocation;
    @Unindexed private DateTime departureTime;

    @Indexed private Key<UserProfile> patientProfile;
    @Unindexed private GeoPt patientLocation;
    @Unindexed private DateTime pickupTime;

    @Indexed private DateTime arrivalTime;

    @Indexed private String appointmentAddress;
    @Unindexed private GeoPt appointmentLocation;
    @Indexed private DateTime appointmentTime;

    @Unindexed private double distanceMiles;
    @Indexed(IfFalse.class) private boolean isExported = false;

    public Long getId() {
        return id;
    }

    public Key<UserProfile> getVolunteerUserProfile() {
        return volunteerUserProfile;
    }

    public void setVolunteerUserProfile(Key<UserProfile> volunteerUserProfile) {
        this.volunteerUserProfile = volunteerUserProfile;
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

    public Key<UserProfile> getPatientProfile() {
        return patientProfile;
    }

    public void setPatientProfile(Key<UserProfile> patientProfile) {
        this.patientProfile = patientProfile;
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

    public boolean isExported() {
        return isExported;
    }

    public void setIsExported(boolean isExported) {
        this.isExported = isExported;
    }

    public static QueryResultIterable<RideRecord> getExportableRecords() {
        return OfyService.ofy().query(RideRecord.class)
                .filter("isExported", false)
                .fetch();
    }
}
