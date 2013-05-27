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
    marker.setPosition(null); // Hide the marker by default.

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

    var setUnknownLocation = function() {
        location.setValid(null);
    };

    mashmesh.userProfile.markMap = function (address) {
        mashmesh.userProfile.location.invalidate();

        $.ajax({
            type: "GET",
            url: "/resources/geocode",
            data: {address: address},
            dataType: "json",
            success: function (data) {
                var latLng = new google.maps.LatLng(data.latitude, data.longitude);
                setValidLocation(latLng);
            },
            error: function (xhr) {
                if (xhr.status == 410) {
                    setInvalidLocation();
                } else {
                    setUnknownLocation();
                }
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
