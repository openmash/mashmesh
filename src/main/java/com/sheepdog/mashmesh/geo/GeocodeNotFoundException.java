package com.sheepdog.mashmesh.geo;

public class GeocodeNotFoundException extends GeocodeException {
    private String address;

    public GeocodeNotFoundException(String address) {
        this.address = address;
    }

    public String getMessage() {
        return String.format("Address '%s' does not exist", address);
    }
}
