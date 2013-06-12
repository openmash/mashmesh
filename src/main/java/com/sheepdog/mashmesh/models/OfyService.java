/**
 *    Copyright 2013 Talend Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
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