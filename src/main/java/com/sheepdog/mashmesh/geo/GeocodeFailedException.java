package com.sheepdog.mashmesh.geo;

import com.google.code.geocoder.model.GeocoderStatus;

public class GeocodeFailedException extends GeocodeException {
    private String address;
    private GeocoderStatus status;

    public GeocodeFailedException(String address, GeocoderStatus status) {
        this.address = address;
        this.status = status;
    }

    public String getAddress() {
        return address;
    }

    public GeocoderStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return String.format("Failed to geocode '%s': %s", address, status.value());
    }
}
