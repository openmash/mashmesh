package com.sheepdog.mashmesh;

import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.search.*;
import com.googlecode.objectify.Key;
import com.sheepdog.mashmesh.geo.GeoUtils;
import com.sheepdog.mashmesh.models.OfyService;
import com.sheepdog.mashmesh.models.VolunteerProfile;
import com.sheepdog.mashmesh.util.ApplicationConfiguration;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class VolunteerLocator {
    public static final int ESTIMATED_MILES_PER_HOUR = 40;

    private final GeoPt patientLocation;
    private final GeoPt appointmentLocation;
    private final DateTime appointmentDateTime;

    public VolunteerLocator(GeoPt patientLocation, GeoPt appointmentLocation, DateTime appointmentDateTime) {
        this.patientLocation = patientLocation;
        this.appointmentLocation = appointmentLocation;
        this.appointmentDateTime = appointmentDateTime;
    }

    public List<VolunteerProfile> getNearbyVolunteers() {
        String sortString = String.format("distance(location, %s)", GeoUtils.formatGeoPt(patientLocation));
        String queryString = String.format("distance(location, %s) < maximumDistance",
                GeoUtils.formatGeoPt(patientLocation));

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
                .build(ApplicationConfiguration.isDevelopment() ? "" : queryString);

        Collection<? extends Document> documents = VolunteerProfile.getIndex().search(query).getResults();

        List<Key<VolunteerProfile>> volunteerProfileKeys = new ArrayList<Key<VolunteerProfile>>();

        for (Document document : documents) {
            String userId = document.getOnlyField("userId").getText();
            Key<VolunteerProfile> volunteerProfileKey = Key.create(VolunteerProfile.class, userId);
            volunteerProfileKeys.add(volunteerProfileKey);
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
        double distanceToAppointment = GeoUtils.distanceMiles(patientLocation, appointmentLocation);

        for (VolunteerProfile volunteerProfile : volunteerProfiles) {
            double distanceToPatient = GeoUtils.distanceMiles(volunteerProfile.getLocation(), patientLocation);
            double distanceFromAppointment = GeoUtils.distanceMiles(appointmentLocation, volunteerProfile.getLocation());
            double hoursToAppointment = (distanceToPatient + distanceToAppointment) / ESTIMATED_MILES_PER_HOUR;
            double hoursFromAppointment = distanceFromAppointment / ESTIMATED_MILES_PER_HOUR;

            DateTime startDateTime = appointmentDateTime.minusMinutes((int) (hoursToAppointment * 60));
            DateTime endDateTime = appointmentDateTime.plusMinutes((int) (hoursFromAppointment * 60));

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
            return volunteerProfiles.iterator().next();
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