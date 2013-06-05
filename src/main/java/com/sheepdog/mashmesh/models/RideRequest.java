package com.sheepdog.mashmesh.models;

import com.google.appengine.api.datastore.GeoPt;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.Unindexed;
import org.joda.time.DateTime;

import javax.persistence.Embedded;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.*;

@Entity
public class RideRequest {
    @Id Long id;
    @Indexed Key<UserProfile> patientUserProfileKey;
    @Unindexed String appointmentAddress;
    @Indexed GeoPt appointmentLocation;
    @Unindexed DateTime appointmentTime;
    @Indexed Set<String> declinedVolunteerUserIds = new HashSet<String>();

    @Unindexed Key<RideRecord> pendingRideRecordKey = null;
    @Unindexed Key<VolunteerProfile> pendingVolunteerProfileKey = null;

    @Transient UserProfile patientProfile = null;
    @Transient RideRecord pendingRideRecord = null;
    @Transient VolunteerProfile pendingVolunteerProfile = null;

    public Long getId() {
        return id;
    }

    public Key<UserProfile> getPatientUserProfileKey() {
        return patientUserProfileKey;
    }

    public void setPatientUserProfileKey(Key<UserProfile> patientUserProfileKey) {
        this.patientUserProfileKey = patientUserProfileKey;
    }

    public String getPatientAddress() {
        return getPatientProfile().getAddress();
    }

    public GeoPt getPatientLocation() {
        return getPatientProfile().getLocation();
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

    public boolean hasDeclined(String volunteerUserId) {
        return declinedVolunteerUserIds.contains(volunteerUserId);
    }

    public void addDeclinedVolunteerUserId(String declinedVolunteerUserId) {
        declinedVolunteerUserIds.add(declinedVolunteerUserId);
    }

    public Key<RideRecord> getPendingRideRecordKey() {
        return pendingRideRecordKey;
    }

    public void setPendingRideRecordKey(Key<RideRecord> pendingRideRecordKey) {
        this.pendingRideRecordKey = pendingRideRecordKey;
    }

    public Key<VolunteerProfile> getPendingVolunteerProfileKey() {
        return pendingVolunteerProfileKey;
    }

    public void setPendingVolunteerProfileKey(Key<VolunteerProfile> pendingVolunteerProfileKey) {
        this.pendingVolunteerProfileKey = pendingVolunteerProfileKey;
    }

    public UserProfile getPatientProfile() {
        if (patientProfile == null) {
            patientProfile = OfyService.ofy().get(patientUserProfileKey);
        }

        return patientProfile;
    }

    public RideRecord getPendingRideRecord() {
        if (pendingRideRecord == null) {
            pendingRideRecord = OfyService.ofy().get(pendingRideRecordKey);
        }

        return pendingRideRecord;
    }

    public VolunteerProfile getPendingVolunteerProfile() {
        if (pendingVolunteerProfile == null) {
            pendingVolunteerProfile = OfyService.ofy().get(pendingVolunteerProfileKey);
        }

        return pendingVolunteerProfile;
    }
}
