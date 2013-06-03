package com.sheepdog.mashmesh.geo;

import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.search.GeoPoint;
import com.google.code.geocoder.Geocoder;
import com.google.code.geocoder.GeocoderRequestBuilder;
import com.google.code.geocoder.model.GeocodeResponse;
import com.google.code.geocoder.model.GeocoderRequest;
import com.google.code.geocoder.model.GeocoderStatus;
import com.google.code.geocoder.model.LatLng;
import com.sheepdog.mashmesh.util.ApplicationConstants;
import com.sheepdog.mashmesh.util.CacheProxy;

public class GeoUtils {
    private static final Object INVALID_LOCATION = "invalid location";

    public static GeoPt parseGeoPt(String latlng) {
        if (latlng == null) {
            return null;
        }

        String[] parts = latlng.split(",", 2);
        // TODO: Validation
        return new GeoPt(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]));
    }

    public static GeoPoint convertToGeoPoint(GeoPt geoPt) {
        return new GeoPoint(geoPt.getLatitude(), geoPt.getLongitude());
    }

    public static String formatGeoPt(GeoPt geoPoint) {
        return String.format("geopoint(%f, %f)", geoPoint.getLatitude(), geoPoint.getLongitude());
    }

    public static double distance(GeoPt a, GeoPt b) {
        double radius = 6371; // The earth's radius in kilometers.
        double latitudeDistance = Math.toRadians(b.getLatitude() - a.getLatitude());
        double longitudeDistance = Math.toRadians(b.getLongitude() - a.getLongitude());
        double latitudeSine = Math.sin(latitudeDistance / 2);
        double longitudeSine = Math.sin(longitudeDistance / 2);
        double aLatitudeProjection = Math.cos(Math.toRadians(a.getLatitude()));
        double bLatitudeProjection = Math.cos(Math.toRadians(b.getLatitude()));
        double alpha = Math.pow(latitudeSine, 2) +
                Math.pow(longitudeSine, 2) * aLatitudeProjection * bLatitudeProjection;
        double distance = radius * 2 * Math.atan2(Math.sqrt(alpha), Math.sqrt(1 - alpha));
        return distance;
    }

    public static double distanceMiles(GeoPt a, GeoPt b) {
        return distance(a, b) / ApplicationConstants.KILOMETERS_PER_MILE;
    }

    public static GeoPt geocode(String address) throws GeocodeFailedException, GeocodeNotFoundException {
        // The underlying geocoder library fails if it receives an empty address.
        if (address.trim().isEmpty()) {
            throw new GeocodeFailedException(address, GeocoderStatus.INVALID_REQUEST);
        }

        String cacheKey = "geocode:" + address;
        CacheProxy cache = new CacheProxy();
        Object cacheEntry = cache.get(cacheKey);

        // Backwards comparison -- cacheEntry may be null
        if (INVALID_LOCATION.equals(cacheEntry)) {
            throw new GeocodeNotFoundException(address);
        }

        if (cacheEntry != null) {
            return (GeoPt) cacheEntry;
        }

        Geocoder geocoder = new Geocoder(); // TODO: Use Maps for Business?
        GeocoderRequest request = new GeocoderRequestBuilder().setAddress(address).getGeocoderRequest();
        GeocodeResponse response = geocoder.geocode(request);
        GeocoderStatus status = response.getStatus();

        if (status == GeocoderStatus.ZERO_RESULTS) {
            cache.put(cacheKey, INVALID_LOCATION);
            throw new GeocodeNotFoundException(address);
        } else if (status != GeocoderStatus.OK) {
            // We've encountered a temporary error, so return without caching the missing point.
            throw new GeocodeFailedException(address, response.getStatus());
        } else {
            LatLng location = response.getResults().get(0).getGeometry().getLocation();
            GeoPt geoPt = new GeoPt(location.getLat().floatValue(), location.getLng().floatValue());
            cache.put(cacheKey, geoPt);
            return geoPt;
        }
    }
}
