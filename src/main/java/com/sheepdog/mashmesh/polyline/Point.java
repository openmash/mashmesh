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
