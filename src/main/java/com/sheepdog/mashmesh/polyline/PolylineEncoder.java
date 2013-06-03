package com.sheepdog.mashmesh.polyline;

import java.util.*;

public class PolylineEncoder {
    public static final double DEFAULT_MINIMUM_DISTANCE = 0.00001;

    private final double minimumDistance;

    public PolylineEncoder(double minimumDistance) {
        this.minimumDistance = minimumDistance;
    }

    void encodeNumber(StringBuilder encodedString, int number) {
        while (number >= 0x20) {
            int nextByte = (0x20 | (number & 0x1f)) + 63;
            encodedString.appendCodePoint(nextByte);
            number >>= 5;
        }

        encodedString.appendCodePoint(number + 63);
    }

    void encodeSignedNumber(StringBuilder encodedString, int number) {
        int signedNumber = (number < 0) ? ~(number << 1) : (number << 1);
        encodeNumber(encodedString, signedNumber);
    }

    double distance(Point p0, Point s0, Point s1, double segmentLength) {
        double result;

        if (s0.equals(s1)) {
            result = s1.distance(p0);
        } else {
            double u = ((p0.latitudeDistance(s0) * s1.latitudeDistance(s0)) +
                    (p0.longitudeDistance(s0) * s1.longitudeDistance(s0))) / segmentLength;

            if (u <= 0) {
                result = p0.distance(s0);
            } else if (u >= 1) {
                result = p0.distance(s1);
            } else {
                double dLat = p0.latitudeDistance(s0) - u * s1.latitudeDistance(s0);
                double dLng = p0.longitudeDistance(s0) - u * s1.longitudeDistance(s0);
                result = Math.sqrt(Math.pow(dLat, 2) + Math.pow(dLng, 2));
            }
        }

        return result;
    }

    public List<Point> filterPoints(List<Point> points) {
        Deque<PointPair> stack = new ArrayDeque<PointPair>(points.size());
        double[] distances = new double[points.size()];
        Arrays.fill(distances, 0);

        if (points.size() <= 2) {
            return points;
        }

        stack.push(new PointPair(0, points.size() - 1));

        while (!stack.isEmpty()) {
            PointPair current = stack.pop();
            Point fromPoint = points.get(current.getFromIndex());
            Point toPoint = points.get(current.getToIndex());
            double maximumDistance = 0;
            int maximumDistanceIndex = 0;
            double segmentLength = fromPoint.distance(toPoint);

            for (int i = current.getFromIndex() + 1; i < current.getToIndex(); i++) {
                double pairwiseDistance = this.distance(points.get(i), fromPoint, toPoint, segmentLength);

                if (pairwiseDistance > maximumDistance) {
                    maximumDistance = pairwiseDistance;
                    maximumDistanceIndex = i;
                }
            }

            if (maximumDistance > minimumDistance) {
                distances[maximumDistanceIndex] = maximumDistance;
                stack.push(new PointPair(current.getFromIndex(), maximumDistanceIndex));
                stack.push(new PointPair(maximumDistanceIndex, current.getToIndex()));
            }
        }

        List<Point> remainingPoints = new ArrayList<Point>();

        // We need to be careful to make sure that we include both the first and
        // last point in the output.
        remainingPoints.add(points.get(0));

        for (int i = 1; i < points.size() - 1; i++) {
            if (distances[i] > 0) {
                remainingPoints.add(points.get(i));
            }
        }

        remainingPoints.add(points.get(points.size() - 1));

        return remainingPoints;
    }

    public String encodePoints(List<Point> points) {
        StringBuilder encodedString = new StringBuilder();
        int lat = 0;
        int lng = 0;

        for (Point point : points) {
            int late5 = (int)Math.floor(point.getLatitude() * 1e5);
            int lnge5 = (int)Math.floor(point.getLongitude() * 1e5);
            int dLat = late5 - lat;
            int dLng = lnge5 - lng;
            encodeSignedNumber(encodedString, dLat);
            encodeSignedNumber(encodedString, dLng);
            lat = late5;
            lng = lnge5;
        }

        return encodedString.toString();
    }
}
