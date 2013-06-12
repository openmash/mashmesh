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
