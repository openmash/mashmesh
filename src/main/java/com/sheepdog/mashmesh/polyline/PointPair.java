package com.sheepdog.mashmesh.polyline;

public class PointPair {
    private final int fromIndex;
    private final int toIndex;

    public PointPair(int fromIndex, int toIndex) {
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    public int getFromIndex() {
        return fromIndex;
    }

    public int getToIndex() {
        return toIndex;
    }
}
