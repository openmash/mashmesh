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
package com.sheepdog.mashmesh.integration;

import com.sheepdog.mashmesh.TestLocationConstants;
import com.sheepdog.mashmesh.models.AvailableTimePeriod;
import org.joda.time.DateTimeConstants;

import java.util.Arrays;

public class IntegrationTestConstants {
    public static final PatientConfig PATIENT_1 = new PatientConfig()
            .setName("John Smith")
            .setEmail("john.smith@example.com")
            .setAddress(TestLocationConstants.EAST_BAYSHORE_EPA_ADDRESS);

    public static final PatientConfig PATIENT_2 = new PatientConfig()
            .setName("Chris Travis")
            .setEmail("chris.travis@example.com")
            .setAddress(TestLocationConstants.MENLO_PARK_ADDRESS);

    public static final PatientConfig DISTANT_PATIENT = new PatientConfig()
            .setName("James Gravel")
            .setEmail("james.gravel@example.com")
            .setAddress(TestLocationConstants.OAKLAND_ADDRESS);

    public static final VolunteerConfig VOLUNTEER_1 = new VolunteerConfig()
            .setName("Diane Fraser")
            .setEmail("diane.fraser@gmail.com")
            .setComments("I will try to be five minutes early")
            .setMaximumDistance(20)
            .setAddress(TestLocationConstants.UNIVERSITY_AVENUE_PA_ADDRESS)
            .setAvailableTimePeriods(Arrays.asList(
                    new AvailableTimePeriod().setDay(DateTimeConstants.SUNDAY).setStartTime(6).setEndTime(20),
                    new AvailableTimePeriod().setDay(DateTimeConstants.TUESDAY).setStartTime(6).setEndTime(10),
                    new AvailableTimePeriod().setDay(DateTimeConstants.TUESDAY).setStartTime(13).setEndTime(17),
                    new AvailableTimePeriod().setDay(DateTimeConstants.WEDNESDAY).setStartTime(6).setEndTime(9),
                    new AvailableTimePeriod().setDay(DateTimeConstants.WEDNESDAY).setStartTime(11).setEndTime(12),
                    new AvailableTimePeriod().setDay(DateTimeConstants.WEDNESDAY).setStartTime(13).setEndTime(18),
                    new AvailableTimePeriod().setDay(DateTimeConstants.THURSDAY).setStartTime(6).setEndTime(8),
                    new AvailableTimePeriod().setDay(DateTimeConstants.SATURDAY).setStartTime(13).setEndTime(20)
            ));

    public static final String VOLUNTEER_1_UNAVAILABLE_TIME = "2013-06-06T10:20:00-0700";
    public static final String VOLUNTEER_1_AVAILABLE_TIME = "2013-06-05T16:30:00-0700";

    public static final VolunteerConfig VOLUNTEER_2 = new VolunteerConfig()
            .setName("Taylor Osmond")
            .setEmail("t.osmond42@gmail.com")
            .setMaximumDistance(10)
            .setAddress(TestLocationConstants.E_OKEEFE_EPA_ADDRESS)
            .setAvailableTimePeriods(Arrays.asList(
                    new AvailableTimePeriod().setDay(DateTimeConstants.MONDAY).setStartTime(10).setEndTime(18),
                    new AvailableTimePeriod().setDay(DateTimeConstants.TUESDAY).setStartTime(10).setEndTime(18),
                    new AvailableTimePeriod().setDay(DateTimeConstants.WEDNESDAY).setStartTime(10).setEndTime(18),
                    new AvailableTimePeriod().setDay(DateTimeConstants.FRIDAY).setStartTime(10).setEndTime(18)
            ));

    public static final String ONLY_VOLUNTEER_2_UNAVAILABLE_TIME = "2013-06-06T07:00:00-0700";
    public static final String ONLY_VOLUNTEER_2_AVAILABLE_TIME = "2013-06-10T13:00:00-0700";

    public static final String VOLUNTEER_1_AND_2_UNAVAILABLE_TIME = "2013-06-06T12:00:00-0700";
    public static final String VOLUNTEER_1_AND_2_AVAILABLE_TIME = "2013-06-04T14:00:00-0700";
}
