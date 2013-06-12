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

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.Objectify;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DateTimeSerializationTest {
    private final LocalServiceTestHelper localServiceTestHelper =
            new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

    @Before
    public void setUp() {
        localServiceTestHelper.setUp();
    }

    @After
    public void tearDown() {
        localServiceTestHelper.tearDown();
    }

    @Test
    public void testTimeZoneSerialization() {
        String dateTimeIso8601 = "2013-06-06T20:00:00-03:00";
        RideRecord rideRecord = new RideRecord();
        DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis().withOffsetParsed();
        DateTime dateTime = dateTimeFormatter.parseDateTime(dateTimeIso8601);
        rideRecord.setPickupTime(dateTime);

        Objectify ofy = OfyService.ofy();
        ofy.put(rideRecord);

        RideRecord restored = ofy.get(rideRecord.getKey());
        Assert.assertEquals(dateTimeIso8601, dateTimeFormatter.print(restored.getPickupTime()));
    }
}
