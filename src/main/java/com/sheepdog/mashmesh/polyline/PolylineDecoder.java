package com.sheepdog.mashmesh.polyline;

import java.util.ArrayList;
import java.util.List;

public class PolylineDecoder {
    private final String polyline;
    private final List<Point> points;
    private int index;

    public PolylineDecoder(String polyline) {
        this.polyline = polyline;
        this.index = 0;
        this.points = decode();
    }

    private int readNumber() {
        int number = 0;
        int shift = 0;
        int nextByte;

        do {
            nextByte = polyline.codePointAt(index) - 63;
            number |= (nextByte & 0x1f) << shift;
            index++;
            shift += 5;
        } while (nextByte >= 0x20);

        return ((number & 1) == 1) ? ~(number >> 1) : (number >> 1);
    }

    private List<Point> decode() {
        List<Point> points = new ArrayList<Point>();
        int lat = 0;
        int lng = 0;

        while (index < polyline.length()) {
            lat += readNumber();
            lng += readNumber();
            points.add(new Point(lat * 1e-5, lng * 1e-5));
        }

        return points;
    }

    public List<Point> getPoints() {
        return points;
    }
}
