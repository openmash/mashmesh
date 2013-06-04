package com.sheepdog.mashmesh.models;

import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.search.*;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.Unindexed;
import com.sheepdog.mashmesh.geo.GeoUtils;
import com.sheepdog.mashmesh.util.ApplicationConstants;
import org.joda.time.*;

import javax.persistence.Embedded;
import javax.persistence.Id;
import java.util.*;

@Entity
public class VolunteerProfile {
    private static final String INDEX_NAME = "volunteer-locations";
    private static final double DEFAULT_MAXIMUM_DISTANCE_MILES = 25;

    @Unindexed
    private static class AppointmentPeriod {
        private long startTimeMillis;
        @Indexed private long endTimeMillis;
    }

    @Unindexed
    private static class AvailableTimePeriod {
        private int day; // Monday = 1, Sunday = 7
        private LocalTime startTime;
        private LocalTime endTime;
    }

    @Id private String userId;
    @Unindexed private String documentId;
    @Unindexed private GeoPt location;
    @Unindexed private double maximumDistanceMiles = DEFAULT_MAXIMUM_DISTANCE_MILES;
    @Embedded private List<AppointmentPeriod> appointmentTimes = new ArrayList<AppointmentPeriod>();
    @Embedded private List<AvailableTimePeriod> availableTimePeriods = new ArrayList<AvailableTimePeriod>();

    // TODO: Testing
    public void setAlwaysAvailable() {
        availableTimePeriods.clear();

        for (int i = DateTimeConstants.MONDAY; i <= DateTimeConstants.SUNDAY; i++) {
            AvailableTimePeriod availableTimePeriod = new AvailableTimePeriod();
            availableTimePeriod.day = i;
            availableTimePeriod.startTime = new LocalTime(0, 0);
            availableTimePeriod.endTime = new LocalTime(0, 0);
            availableTimePeriods.add(availableTimePeriod);
        }
    }

    // TODO: Unit tests
    List<Interval> getAvailableIntervals(DateTime aroundDateTime) {
        List<Interval> intervals = new ArrayList<Interval>();
        int dayOfWeek = aroundDateTime.getDayOfWeek();

        for (AvailableTimePeriod availableTimePeriod : availableTimePeriods) {
            if (availableTimePeriod.day >= dayOfWeek - 1 && availableTimePeriod.day <= dayOfWeek + 1) {
                LocalDate date = aroundDateTime.toLocalDate();
                DateTime start = date.toDateTime(availableTimePeriod.startTime, aroundDateTime.getZone());
                DateTime end = date.toDateTime(availableTimePeriod.endTime, aroundDateTime.getZone());

                if (end.compareTo(start) <= 0) {
                    end = end.plusDays(1);
                }

                intervals.add(new Interval(start, end));
            }
        }

        Collections.sort(intervals, new Comparator<Interval>() {
            @Override
            public int compare(Interval i1, Interval i2) {
                return new Long(i1.getStartMillis()).compareTo(i2.getStartMillis());
            }
        });

        List<Interval> mergedIntervals = new ArrayList<Interval>();
        Interval lastInterval = null;

        for (Interval interval : intervals) {
            if (lastInterval != null && lastInterval.abuts(interval)) {
                mergedIntervals.remove(mergedIntervals.size() - 1);
                interval = lastInterval.withEnd(interval.getEnd());
            }

            lastInterval = interval;
            mergedIntervals.add(interval);
        }

        return mergedIntervals;
    }

    public void addAppointmentTime(DateTime departureTime, DateTime arrivalTime) {
        Duration commuteDuration = new Duration(departureTime, arrivalTime);
        DateTime startTime = departureTime;
        DateTime endTime = arrivalTime.plus(commuteDuration);

        AppointmentPeriod appointmentTime = new AppointmentPeriod();
        appointmentTime.startTimeMillis = startTime.getMillis();
        appointmentTime.endTimeMillis = endTime.getMillis();

        appointmentTimes.add(appointmentTime);
    }

    public boolean isTimeslotAvailable(DateTime startDateTime, DateTime endDateTime) {
        // Assuming that the total interval is less than a day long
        List<Interval> availableIntervals = getAvailableIntervals(startDateTime);
        Interval timeslot = new Interval(startDateTime, endDateTime);

        for (Interval availableInterval : availableIntervals) {
            if (availableInterval.contains(timeslot)) {
                return true;
            }
        }

        return false;
    }

    public boolean isTimeslotOccupied(DateTime startDateTime, DateTime endDateTime) {
        long startMillisecond = startDateTime.getMillis();
        long endMillisecond = endDateTime.getMillis();

        for (AppointmentPeriod appointmentTime : appointmentTimes) {
            long startTimeMillis = appointmentTime.startTimeMillis;
            long endTimeMills = appointmentTime.endTimeMillis;

            if (startTimeMillis < startMillisecond && endTimeMills > endMillisecond) {
                return true;
            }
        }

        return false;
    }

    public UserProfile getUserProfile() {
        Key<UserProfile> userProfileKey = Key.create(UserProfile.class, getUserId());
        return OfyService.ofy().find(userProfileKey);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public GeoPt getLocation() {
        return location;
    }

    public void setLocation(GeoPt location) {
        this.location = location;
    }

    public double getMaximumDistanceMiles() {
        return maximumDistanceMiles;
    }

    public void setMaximumDistanceMiles(double maximumDistanceMiles) {
        this.maximumDistanceMiles = maximumDistanceMiles;
    }

    public static Index getIndex() {
        IndexSpec indexSpec = IndexSpec.newBuilder().setName(INDEX_NAME).build();
        return SearchServiceFactory.getSearchService().getIndex(indexSpec);
    }

    public static int clearIndex() {
        Index index = getIndex();
        int documentCount = 0;

        while (true) {
            Query query = Query.newBuilder()
                .setOptions(QueryOptions.newBuilder().setLimit(1000).build())
                .build("");
            Collection<ScoredDocument> documents = index.search(query).getResults();

            documentCount += documents.size();

            if (documents.size() == 0) {
                break;
            }

            List<String> ids = new ArrayList<String>();

            for (ScoredDocument document : documents) {
                ids.add(document.getId());
            }

            index.delete(ids);
        }

        return documentCount;
    }

    public Document makeDocument(UserProfile userProfile) {
        GeoPoint location = GeoUtils.convertToGeoPoint(userProfile.getLocation());
        double maximumDistanceMeters = maximumDistanceMiles * ApplicationConstants.KILOMETERS_PER_MILE * 1000;
        Document.Builder documentBuilder = Document.newBuilder()
            .addField(Field.newBuilder().setName("userId").setText(getUserId()))
            .addField(Field.newBuilder().setName("maximumDistance").setNumber(maximumDistanceMeters))
            .addField(Field.newBuilder().setName("location").setGeoPoint(location));

        if (documentId != null) {
            documentBuilder.setId(documentId);
        }

        return documentBuilder.build();
    }

    public void updateDocument(UserProfile userProfile) {
        Index index = getIndex();
        Document document = makeDocument(userProfile);
        PutResponse response = index.put(document);
        documentId = response.getIds().get(0);
        // TODO: PutExceptions and transient errors.
    }

    public void removeExpiredAppointments(long cutOffTimeMillis) {
        List<AppointmentPeriod> appointmentPeriodsRemaining = new ArrayList<AppointmentPeriod>();

        for (AppointmentPeriod appointmentPeriod : appointmentTimes) {
            if (appointmentPeriod.endTimeMillis >= cutOffTimeMillis) {
                appointmentPeriodsRemaining.add(appointmentPeriod);
            }
        }

        appointmentTimes = appointmentPeriodsRemaining;
    }

    public Key<VolunteerProfile> getKey() {
        return Key.create(VolunteerProfile.class, getUserId());
    }

    public static VolunteerProfile getOrCreate(UserProfile userProfile) {
        Key<VolunteerProfile> volunteerProfileKey = Key.create(VolunteerProfile.class, userProfile.getUserId());
        VolunteerProfile volunteerProfile = OfyService.ofy().find(volunteerProfileKey);

        if (volunteerProfile == null) {
            volunteerProfile = new VolunteerProfile();
            volunteerProfile.setUserId(userProfile.getUserId());
        }

        return volunteerProfile;
    }

    public static QueryResultIterator<VolunteerProfile> withExpiredAppointments(long cutOffTimeMillis, int chunkSize) {
        return OfyService.ofy()
                .query(VolunteerProfile.class)
                .filter("appointmentTimes.endTimeMills <", cutOffTimeMillis)
                .chunkSize(chunkSize)
                .iterator();
    }
}
