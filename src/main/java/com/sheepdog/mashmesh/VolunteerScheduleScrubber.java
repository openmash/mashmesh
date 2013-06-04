package com.sheepdog.mashmesh;

import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.apphosting.api.ApiProxy;
import com.sheepdog.mashmesh.models.OfyService;
import com.sheepdog.mashmesh.models.VolunteerProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class VolunteerScheduleScrubber {
    private static final int CHUNK_SIZE = 100;
    private static final int RESERVED_WRITE_MILLIS = 30000;

    private static Logger logger = Logger.getLogger(VolunteerScheduleScrubber.class.getCanonicalName());

    private boolean isTerminatedEarly = false;

    public boolean isTerminatedEarly() {
        return isTerminatedEarly;
    }

    private boolean hasTimeRemaining() {
        return ApiProxy.getCurrentEnvironment().getRemainingMillis() > RESERVED_WRITE_MILLIS;
    }

    public void run(long cutOffTimeMillis) {
        int updatedProfileCount = 0;
        QueryResultIterator<VolunteerProfile> profileIterator = VolunteerProfile.withExpiredAppointments(
                cutOffTimeMillis, CHUNK_SIZE);
        List<VolunteerProfile> updatedProfiles = new ArrayList<VolunteerProfile>(CHUNK_SIZE);

        while (profileIterator.hasNext() && hasTimeRemaining()) {
            for (int i = 0; i < CHUNK_SIZE && profileIterator.hasNext(); i++) {
                VolunteerProfile nextProfile = profileIterator.next();
                nextProfile.removeExpiredAppointments(cutOffTimeMillis);

                updatedProfiles.add(nextProfile);
                updatedProfileCount++;
            }

            OfyService.ofy().put(updatedProfiles);
            updatedProfiles.clear();
        }

        logger.info(String.format("Updated schedules for %d profiles", updatedProfileCount));

        if (profileIterator.hasNext()) {
            isTerminatedEarly = true;
            logger.warning("Terminating early due to request timeout");
        }
    }
}
