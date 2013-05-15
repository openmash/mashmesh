Wireframes and Ideas
====================

This document has a few of the example integrations that we were able to come
up with between VistA and Google services. Right now, most of the ideas are
variations on the theme of "veterans may need rides to their appointments",
though we'd like to be able to choose the best one or two and cut it down to
a useful demo.

Due limitations in Balsamiq mockups, routes are missing from the wireframes.
Whenever there is a map with a series of markers, it's safe to assume that
there would be a route included as well.


1. A Veteran can view their upcoming appointments
-------------------------------------------------

Veterans may want to view their upcoming appointments, and get directions
from their home to their appointment, or between appointments. The displayed
map would include the route to take between appointments (omitted here because
of problems with the mockup tool.)
This idea is less interesting because it has a lot of overlap with MyHealtheVet.

![wireframe for veteran appointments](https://raw.github.com/openmash/mashmesh/master/wireframes/veteran-views-appointments.png)


2. A Veteran can email directions for their appointments in a day
-----------------------------------------------------------------

A veteran might want to email directions for the appointments they have
book in a day to someone else, and include a personalized message. The map
could include the route for the day, with the appointment times annotated
on it.

This would make use of Google's Static Maps API, but isn't particularly
compelling.

![wireframe for veterans emailing directions](https://raw.github.com/openmash/mashmesh/master/wireframes/veteran-sends-directions.png)

![wireframe for a volunteer receiving directions](https://raw.github.com/openmash/mashmesh/master/wireframes/volunteer-receives-directions.png)


3. Volunteer Transportation Networks and Volunteers can organize pickups
------------------------------------------------------------------------

Volunteer Transportation Networks and Volunteers are sometimes needed to
drive veterans to and from appointments. An interactive annotated map
would make it easier to organize when to pick veterans up in order to
get them to their appointments.

![wireframe for a VTN organizing a route](https://raw.github.com/openmash/mashmesh/master/wireframes/vtn-organizes-pickups.png)

Google's distance matrix API might also make it possible to build a demo
that autogenerates pickups and plots them on a map with the data exported
from VistA.

![wireframe for a VTN generating a pickup route](https://raw.github.com/openmash/mashmesh/master/wireframes/vtn-autogenerates-routes.png)

Another idea might be to give volunteers a way to help out and offer a
drive to someone travelling in the same direction as they are.

![wireframe for a volunteer organizing pickups along a predetermined route](https://raw.github.com/openmash/mashmesh/master/wireframes/volunteer-organizes-pickups-along-a-predetermined-route.png)


4. Using Google's Data Analysis APIs
------------------------------------

If we have enough data, there may be some opportunities for interesting
data analyses using Google's Fusion Tables and Prediction APIs.
Fusion Tables can generate visualizations like heat maps, plots and
network diagrams, while the Prediction API uses machine learning to
answer questions like "how many people will have appointments on
December 15th, 2013?"
