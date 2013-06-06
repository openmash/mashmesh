var mashmesh = {userProfile: {}};

mashmesh.userProfile.initForm = function () {
    $(".uniForm").uniform();

    var inclusiveRange = function (start, end) {
        var i, range = [];
        for (i = start; i <= end; i++) {
            range.push(i);
        }
        return range;
    };

    var $availability = $("#availability");

    if ($availability.length > 0) {
        var available = new Available({
            "$parent": $("#availabilitySelector"),
            days: [
                {name: "Sun", dayId: 7},
                {name: "Mon", dayId: 1},
                {name: "Tue", dayId: 2},
                {name: "Wed", dayId: 3},
                {name: "Thu", dayId: 4},
                {name: "Fri", dayId: 5},
                {name: "Sat", dayId: 6}
            ],
            hours: inclusiveRange(6, 18),
            onChanged: function (availableIntervals) {
                // TODO: Use json2.js
                var json = JSON.stringify(availableIntervals);
                $availability.val(json);
            }
        });

        var serializedAvailability = $availability.val();
        if (serializedAvailability !== "") {
            available.deserialize(JSON.parse(serializedAvailability));
        }
    }
};

mashmesh.userProfile.initMap = function(selector) {
    var MAXIMUM_GEOCODING_ATTEMPTS = 4;
    var INVALID_LOCATION = "INVALID LOCATION";

    var center = new google.maps.LatLng(0, 0);
    var mapOptions = {
        zoom: 0,
        center: center,
        mapTypeId: google.maps.MapTypeId.HYBRID
    };

    var $mapCanvas = $(selector);
    var map = new google.maps.Map($mapCanvas[0], mapOptions);
    var geocoder = new google.maps.Geocoder();
    var marker = new google.maps.Marker({
        position: center,
        map: map
    });

    marker.setPosition(null); // Hide the marker by default.

    var location = {
        _isValid: null,
        _callback: null,
        _geocodingRetryTask: null,
        _geocodeCache: {}
    };

    location.invalidate = function () {
        location._isValid = null;
        location._callback = null;

        if (location._geocodingRetryTask !== null) {
            clearInterval(location._geocodingRetryTask);
            location._geocodingRetryTask = null;
        }
    }

    location.setValid = function (isValid) {
        location._isValid = isValid;
        if (location._callback !== null) {
            location._callback(isValid);
        }
    };

    location.isValid = function (callback) {
        if (location._isValid === null) {
            location._callback = callback;
        }
        return location._isValid;
    };

    location.getCachedGeocode = function (address) {
        if (location._geocodeCache.hasOwnProperty(address)) {
            return location._geocodeCache[address];
        } else {
            return null;
        }
    };

    location.setCachedGeocode = function (address, latlng) {
        location._geocodeCache[address] = latlng;
    };

    mashmesh.userProfile.location = location;

    var setValidLocation = function (geocode) {
        map.panTo(geocode);
        marker.setPosition(geocode);
        map.setZoom(16);
        location.setValid(true);
    };

    var setInvalidLocation = function () {
        map.panTo(center);
        marker.setPosition(null);
        map.setZoom(0);
        location.setValid(false);
    };

    var setUnknownLocation = function() {
        location.setValid(null);
    };

    var attemptToMarkMap = function (address, attempts) {
        location.invalidate();

        geocoder.geocode({address: address}, function(results, status) {
            if (status === google.maps.GeocoderStatus.OK) {
                var latlng = results[0].geometry.location;
                location.setCachedGeocode(address, latlng);
                setValidLocation(latlng);
            } else if (status === google.maps.GeocoderStatus.ZERO_RESULTS) {
                location.setCachedGeocode(address, INVALID_LOCATION);
                setInvalidLocation();
            } else if (status === google.maps.GeocoderStatus.OVER_QUERY_LIMIT
                       || status === google.maps.GeocoderStatus.REQUEST_DENIED) {
                attempts++;

                if (attempts >= MAXIMUM_GEOCODING_ATTEMPTS) {
                    setUnknownLocation();
                } else {
                    // Back off and try the request again
                    location._geocodingRetryTask = setTimeout(function() {
                        attemptToMarkMap(address, attempts);
                    }, 1000 * attempts);
                }
            } else {
                console.log("Geocoding failed:", status, results);
            }
        });
    };

    mashmesh.userProfile.markMap = function (address) {
        // Don't attempt to geocode invalid addresses.
        if ($.trim(address) === "") {
            setInvalidLocation();
            return;
        }

        var latlng = location.getCachedGeocode(address);

        if (latlng === INVALID_LOCATION) {
            setInvalidLocation();
        } else if (latlng !== null) {
            setValidLocation(latlng);
        } else {
            attemptToMarkMap(address, 0);
        }
    };
};

mashmesh.userProfile.initValidation = function () {
    $(".parsleyForm").parsley({
        validators: {
            location: function (val, _, self) {
                return mashmesh.userProfile.location.isValid(function (isValid) {
                    self.updtConstraint({
                        name: "location",
                        valid: isValid
                    }, "This must be a valid location");
                    self.manageValidationResult();
                });
            }
        },
        messages: {
            location: "This must be a valid location"
        }
    });
};

$(document).ready(function() {
    mashmesh.userProfile.initForm();
    mashmesh.userProfile.initMap("#map-canvas");
    mashmesh.userProfile.initValidation();

    var $location = $("#location");

    var updateLocation = function () {
        var address = $location.val();
        mashmesh.userProfile.markMap(address);
    };

    $(".parsleyForm").parsley("addListener", {
        onFieldValidate: function ($elem) {
            if ($elem.attr("id") === "location") {
                updateLocation();
            }
        }
    });

    updateLocation();
});
