package com.sheepdog.mashmesh;

import com.google.appengine.api.datastore.GeoPt;

public class TestLocationConstants {
    public static final String EAST_BAYSHORE_EPA_ADDRESS = "1760 East Bayshore Road, East Palo Alto, CA";
    public static final GeoPt EAST_BAYSHORE_EPA_GEOPT = new GeoPt(37.4597f, -122.137973f);

    public static final String UNIVERSITY_AVENUE_PA_ADDRESS = "1000 University Avenue, Palo Alto, CA";
    public static final GeoPt UNIVERSITY_AVENUE_PA_GEOPT = new GeoPt(37.444969f, -122.162521f);

    public static final String MENLO_PARK_ADDRESS = "611 Coleman Avenue, Menlo Park, CA";
    public static final GeoPt MENLO_PARK_GEOPT = new GeoPt(37.461421f, -122.160246f);

    public static final String OAKLAND_ADDRESS = "696 18th Street, Oakland, CA";
    public static final GeoPt OAKLAND_GEOPT = new GeoPt(37.8094f, -122.275194f);

    public static final String PALO_ALTO_MEDICAL_FOUNDATION_ADDRESS = "Palo Alto Medical Foundation, Palo Alto, CA";
    public static final GeoPt PALO_ALTO_MEDICAL_FOUNDATION_GEOPT = new GeoPt(37.44012f, -122.161173f);
}
