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
package com.sheepdog.mashmesh;

import com.google.api.client.util.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sheepdog.mashmesh.util.ApplicationConstants;
import org.apache.http.client.utils.URIBuilder;
import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO: Clean up the naming conventions for manually parsed JSON (or maybe switch to using
//       a mapper.)
public class Itinerary {
    private static final String DIRECTIONS_ENDPOINT_URL = "http://maps.googleapis.com/maps/api/directions/json";

    public static class Step {
        private final String htmlInstructions;
        private final String htmlDestination;
        private final String distance;

        private Step(String htmlInstructions, String distance) {
            String[] instructions = htmlInstructions.split("<div ", 2);
            this.htmlInstructions = instructions[0];
            this.htmlDestination = instructions.length < 2 ? "" : "<div " + instructions[1];
            this.distance = distance;
        }

        public String getHtmlInstructions() {
            return htmlInstructions;
        }

        public String getDistance() {
            return distance;
        }

        public String getHtmlDestination() {
            return htmlDestination;
        }
    }

    public static class Leg {
        private List<Step> steps;
        private DateTime departureTime;
        private String startLatLng;
        private DateTime arrivalTime;
        private String endLatLng;
        private double distanceMeters;

        private Leg() {
        }

        public List<Step> getSteps() {
            return steps;
        }

        public DateTime getDepartureTime() {
            return departureTime;
        }

        public DateTime getArrivalTime() {
            return arrivalTime;
        }

        public String getStartLatLng() {
            return startLatLng;
        }

        public String getEndLatLng() {
            return endLatLng;
        }

        public double getDistanceMeters() {
            return distanceMeters;
        }

        public double getDistanceMiles() {
            return distanceMeters / 1000 / ApplicationConstants.KILOMETERS_PER_MILE;
        }
    }

    private String startAddress;
    private String endAddress;
    private List<Leg> legs;
    private String overviewPolyline;

    private Itinerary() {
    }

    private static String getLatLng(JsonObject latLngObject) {
        String lat = latLngObject.get("lat").getAsString();
        String lng = latLngObject.get("lng").getAsString();
        return String.format("%s,%s", lat, lng);
    }

    public static Itinerary fetch(String fromLocation, String toLocation, String viaLocation, DateTime arrivalTime)
            throws URISyntaxException, IOException {
        URIBuilder uriBuilder = new URIBuilder(DIRECTIONS_ENDPOINT_URL);
        uriBuilder.addParameter("origin", fromLocation);
        uriBuilder.addParameter("destination", toLocation);
        uriBuilder.addParameter("mode", "driving");
        uriBuilder.addParameter("language", "en_US"); // TODO: Configurable?
        uriBuilder.addParameter("region", "us");
        uriBuilder.addParameter("waypoints", viaLocation);
        uriBuilder.addParameter("sensor", "false");

        URL url = uriBuilder.build().toURL();
        BufferedReader responseReader = new BufferedReader(new InputStreamReader(url.openStream()));

        JsonParser parser = new JsonParser();
        JsonObject responseObject = parser.parse(responseReader).getAsJsonObject();
        JsonObject route = responseObject.getAsJsonArray("routes").get(0).getAsJsonObject();
        JsonArray legs = route.getAsJsonArray("legs");

        JsonObject startLeg = legs.get(0).getAsJsonObject();
        JsonObject endLeg = legs.get(legs.size() - 1).getAsJsonObject();

        DateTime departureTime = arrivalTime;
        List<Leg> directionLegs = new ArrayList<Leg>();

        Preconditions.checkState(legs.size() == 2, "Expected two direction legs in response");

        // Process the legs in reverse order so that we can compute departure and arrival
        //  times by working backwards from the desired arrival time.
        for (int i = legs.size() - 1; i >= 0; i--) {
            JsonObject leg = legs.get(i).getAsJsonObject();
            List<Step> directionSteps = new ArrayList<Step>();
            DateTime legArrivalTime = departureTime;

            for (JsonElement stepElement : leg.getAsJsonArray("steps")) {
                JsonObject stepObject = stepElement.getAsJsonObject();
                int duration = stepObject.getAsJsonObject("duration").get("value").getAsInt();
                String htmlInstructions = stepObject.get("html_instructions").getAsString();
                String distance = stepObject.getAsJsonObject("distance").get("text").getAsString();

                departureTime = departureTime.minusSeconds(duration);
                directionSteps.add(new Step(htmlInstructions, distance));
            }

            Leg directionLeg = new Leg();
            directionLeg.departureTime = departureTime;
            directionLeg.startLatLng = getLatLng(leg.getAsJsonObject("start_location"));
            directionLeg.arrivalTime = legArrivalTime;
            directionLeg.endLatLng = getLatLng(leg.getAsJsonObject("end_location"));
            directionLeg.distanceMeters = leg.getAsJsonObject("distance").get("value").getAsInt();
            directionLeg.steps = Collections.unmodifiableList(directionSteps);
            directionLegs.add(directionLeg);
        }

        Collections.reverse(directionLegs);

        Itinerary itinerary = new Itinerary();
        itinerary.startAddress = startLeg.get("start_address").getAsString();
        itinerary.endAddress = endLeg.get("end_address").getAsString();
        itinerary.legs = Collections.unmodifiableList(directionLegs);
        itinerary.overviewPolyline = route.getAsJsonObject("overview_polyline").get("points").getAsString();

        return itinerary;
    }

    public Leg getStartLeg() {
        return legs.get(0);
    }

    public Leg getEndLeg() {
        return legs.get(legs.size() - 1);
    }

    public String getStartAddress() {
        return startAddress;
    }

    public String getStartLatLng() {
        return getStartLeg().getStartLatLng();
    }

    public String getPickupLatLng() {
        return getStartLeg().getEndLatLng();
    }

    public DateTime getPickupTime() {
        return getStartLeg().getArrivalTime();
    }

    public String getEndAddress() {
        return endAddress;
    }

    public double getDistanceMiles() {
        double distanceMiles = 0.0;

        for (Leg leg : legs) {
            distanceMiles += leg.getDistanceMiles();
        }

        return distanceMiles;
    }

    public String getEndLatLng() {
        return getEndLeg().getEndLatLng();
    }

    public DateTime getDepartureTime() {
        return getStartLeg().getDepartureTime();
    }

    public DateTime getArrivalTime() {
        return getEndLeg().getArrivalTime();
    }

    public List<Leg> getLegs() {
        return legs;
    }

    public String getOverviewPolyline() {
        return overviewPolyline;
    }
}
