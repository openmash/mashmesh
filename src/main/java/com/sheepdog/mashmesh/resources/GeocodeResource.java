package com.sheepdog.mashmesh.resources;

import com.google.appengine.api.datastore.GeoPt;
import com.sheepdog.mashmesh.geo.GeoUtils;
import com.sheepdog.mashmesh.geo.GeocodeFailedException;
import com.sheepdog.mashmesh.geo.GeocodeNotFoundException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/geocode")
public class GeocodeResource {
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public GeoPt getAddress(@QueryParam("address") String address) {
        if (address == null || address.trim().isEmpty()) {
            throw new WebApplicationException(400);
        }

        try {
            return GeoUtils.geocode(address);
        } catch (GeocodeNotFoundException e) {
            throw new WebApplicationException(410);
        } catch (GeocodeFailedException e) {
            throw new WebApplicationException(503);
        }
    }
}
