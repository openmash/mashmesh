package com.sheepdog.mashmesh;

import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.search.*;
import com.googlecode.objectify.Key;
import com.sheepdog.mashmesh.geo.GeoUtils;
import com.sheepdog.mashmesh.models.OfyService;
import com.sheepdog.mashmesh.models.RideRequest;
import com.sheepdog.mashmesh.models.UserProfile;
import com.sheepdog.mashmesh.models.VolunteerProfile;
import com.sheepdog.mashmesh.util.ApplicationConfiguration;
import org.joda.time.DateTime;

import java.util.*;
import java.util.logging.Logger;

public class VolunteerLocator {
    private static final Logger logger = Logger.getLogger(VolunteerLocator.class.getCanonicalName());
    public static final int ESTIMATED_MILES_PER_HOUR = 40;

    private final RideRequest rideRequest;

    public VolunteerLocator(RideRequest rideRequest) {
        this.rideRequest = rideRequest;
    }

    private GeoPt getPatientLocation() {
        return rideRequest.getPatientProfile().getLocation();
    }

    private GeoPt getAppointmentLocation() {
        return rideRequest.getAppointmentLocation();
    }

    private DateTime getAppointmentTime() {
        return rideRequest.getAppointmentTime();
    }

    public List<VolunteerProfile> getNearbyVolunteers() {
        String sortString = String.format("distance(location, %s)", GeoUtils.formatGeoPt(getPatientLocation()));

        SortOptions sortOptions = SortOptions.newBuilder()
                .addSortExpression(SortExpression.newBuilder()
                        .setExpression(sortString)
                        .setDefaultValueNumeric(200000)
                        .setDirection(SortExpression.SortDirection.ASCENDING))
                .build();

        QueryOptions queryOptions = QueryOptions.newBuilder()
                .setLimit(1000)
                .setSortOptions(sortOptions)
                .build();

        Query query = Query.newBuilder()
                .setOptions(queryOptions)
                .build("");

        Collection<? extends Document> documents = VolunteerProfile.getIndex().search(query).getResults();

        List<Key<VolunteerProfile>> volunteerProfileKeys = new ArrayList<Key<VolunteerProfile>>();

        for (Document document : documents) {
            String userId = document.getOnlyField("userId").getText();

            if (!rideRequest.hasDeclined(userId)) {
                Key<VolunteerProfile> volunteerProfileKey = Key.create(VolunteerProfile.class, userId);
                volunteerProfileKeys.add(volunteerProfileKey);
            }
        }

        Map<Key<VolunteerProfile>, VolunteerProfile> profilesByKey = OfyService.ofy().get(volunteerProfileKeys);
        List<VolunteerProfile> volunteerProfiles = new ArrayList<VolunteerProfile>(volunteerProfileKeys.size());

        for (Key<VolunteerProfile> volunteerProfileKey : volunteerProfileKeys) {
            volunteerProfiles.add(profilesByKey.get(volunteerProfileKey));
        }

        return volunteerProfiles;
    }

    public List<VolunteerProfile> selectEligibleVolunteers(List<VolunteerProfile> volunteerProfiles) {
        List<VolunteerProfile> availableVolunteerProfiles = new ArrayList<VolunteerProfile>();
        double distanceToAppointment = GeoUtils.distanceMiles(getPatientLocation(), getAppointmentLocation());

        for (VolunteerProfile volunteerProfile : volunteerProfiles) {
            double distanceToPatient = GeoUtils.distanceMiles(volunteerProfile.getLocation(), getPatientLocation());
            double distanceFromAppointment = GeoUtils.distanceMiles(getAppointmentLocation(),
                    volunteerProfile.getLocation());
            double hoursToAppointment = (distanceToPatient + distanceToAppointment) / ESTIMATED_MILES_PER_HOUR;
            double hoursFromAppointment = distanceFromAppointment / ESTIMATED_MILES_PER_HOUR;

            DateTime startDateTime = getAppointmentTime().minusMinutes((int) (hoursToAppointment * 60));
            DateTime endDateTime = getAppointmentTime().plusMinutes((int) (hoursFromAppointment * 60));

            if (volunteerProfile.isTimeslotAvailable(startDateTime, endDateTime) &&
                    !volunteerProfile.isTimeslotOccupied(startDateTime, endDateTime) &&
                    volunteerProfile.getMaximumDistanceMiles() > distanceToPatient + distanceToAppointment) {
                availableVolunteerProfiles.add(volunteerProfile);
            }
        }

        return availableVolunteerProfiles;
    }

    public VolunteerProfile selectAppropriateVolunteer(List<VolunteerProfile> volunteerProfiles) {
        // TODO: Improve accuracy with the distance matrix API.
        if (volunteerProfiles.size() == 0) {
            return null;
        } else {
            Collections.sort(volunteerProfiles, new Comparator<VolunteerProfile>() {
                @Override
                public int compare(VolunteerProfile profileA, VolunteerProfile profileB) {
                    double distanceA = GeoUtils.distance(profileA.getLocation(), getPatientLocation());
                    double distanceB = GeoUtils.distance(profileB.getLocation(), getPatientLocation());
                    return (int) Math.signum(distanceA - distanceB);
                }
            });

            return volunteerProfiles.get(0);
        }
    }

    public VolunteerProfile getEligibleVolunteer() {
        List<VolunteerProfile> nearbyVolunteers = getNearbyVolunteers();
        List<VolunteerProfile> eligibleVolunteers = selectEligibleVolunteers(nearbyVolunteers);
        VolunteerProfile volunteer = selectAppropriateVolunteer(eligibleVolunteers);

        // TODO: Raise an exception if no volunteer is found.
        return volunteer;
    }
}