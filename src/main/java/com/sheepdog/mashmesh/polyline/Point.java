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
package com.sheepdog.mashmesh.polyline;

public class Point {
    private final double latitude;
    private final double longitude;

    public Point(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double latitudeDistance(Point point) {
        return this.getLatitude() - point.getLatitude();
    }

    public double longitudeDistance(Point point) {
        return this.getLongitude() - point.getLongitude();
    }

    public double distance(Point point) {
        return Math.sqrt(Math.pow(this.latitudeDistance(point), 2) + Math.pow(this.longitudeDistance(point), 2));
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Point)) {
            return false;
        }

        Point point = (Point) other;
        return this.getLatitude() == point.getLatitude() &&
               this.getLatitude() == point.getLongitude();
    }
}
