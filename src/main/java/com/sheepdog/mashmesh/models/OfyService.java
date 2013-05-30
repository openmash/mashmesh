package com.sheepdog.mashmesh.models;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;

public class OfyService {
    static {
        factory().register(UserProfile.class);
        factory().register(VolunteerProfile.class);
        factory().register(RideRecord.class);

        factory().getConversions().add(new OfyJodaInstantConverter());
    }

    public static Objectify ofy() {
        return ObjectifyService.begin();
    }

    public static ObjectifyFactory factory() {
        return ObjectifyService.factory();
    }
}