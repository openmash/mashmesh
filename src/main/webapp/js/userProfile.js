var mashmesh = {userProfile: {}};

mashmesh.userProfile.initForm = function () {
    $(".uniForm").uniform();
};

mashmesh.userProfile.initMap = function(selector) {
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

    var location = {
        _isValid: null,
        _callback: null,
        geocode: null
    };

    location.invalidate = function () {
        location._isValid = null;
        location.geocode = null;
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
    }

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

    mashmesh.userProfile.markMap = function (location) {
        var request = {
            address: location,
            region: "us"
        };

        mashmesh.userProfile.location.invalidate();

        geocoder.geocode(request, function (results, status) {
            if (status !== google.maps.GeocoderStatus.OK || results.length == 0) {
                setInvalidLocation();
            } else {
                var geocode = results[0].geometry.location;
                setValidLocation(geocode);
            }
        });
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
