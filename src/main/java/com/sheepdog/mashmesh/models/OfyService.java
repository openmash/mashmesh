package com.sheepdog.mashmesh.models;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;

public class OfyService {
    static {
        factory().register(UserProfile.class);
        factory().register(VolunteerProfile.class);
        factory().register(RideRequest.class);
        factory().register(RideRecord.class);

        factory().getConversions().add(new OfyJodaInstantConverter());
        factory().getConversions().add(new OfyJodaLocalTimeConverter());
    }

    public static Objectify ofy() {
        return ObjectifyService.begin();
    }

    public static Objectify transactionOfy() {
        return ObjectifyService.beginTransaction();
    }

    public static ObjectifyFactory factory() {
        return ObjectifyService.factory();
    }
}