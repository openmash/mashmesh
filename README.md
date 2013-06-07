OpenMash Volunteer Ride Sharing (VRS)
=====================================

TODO: Write a short blurb about the purpose of the application and where it
      can be found.

Installation
------------


### Prerequisites

You will need to install [maven](http://maven.apache.org/) version 3 or higher
to build and run mashmesh.


### Configuring API Access

Before running the project, you will need to allow it to access several
Google APIs.

First, visit the [Google API Console](https://code.google.com/apis/console/‎).
If this is your first Google API project, click "Create project..." on the
landing page to get started. Otherwise, if you have an existing project,
select "Create..." from the projects dropdown in the upper left of the window.

Next, select "Services" from the left-hand navigation bar. Click the toggle
switch in the "Status" columns of the services table to enable the following
services:

- Drive API
- Drive SDK
- Fusion Tables API
- Google Maps API v3
- Static Maps API

TODO: Add picture of the services page with a status toggle highlighted.

After this, you will need to create and configure service account.
Select "API access" from the left-hand navigation bar, and then click
"Create an OAuth 2.0 client ID...". Give the product an appropriate
product name, and optionally configure a product logo and home page
URL. Click "Next", and on the next page select "Service account" as the
application type. Click "Create client ID" to complete the process.
You will be presented with a prompt to download a private key. Save
the key to `src/main/webapp/WEB-INF/serviceAccountKey.p12` and close
the prompt.

You will also need to create a web application client ID. Click the
"Create another client ID..." button, and this time select "Web application"
as the client type. You can safely ignore the "site or hostname" field -
it is not used by the application. Click "Create client ID" to generate
a web application ID.

Finally, you will need to configure the application to use the API
credentials. Copy `src/main/webapp/WEB-INF/application.properties.template`
to `src/main/webapp/WEB-INF/application.properties`. Edit application.properties
and fill in the missing fields as follows:

- Configure `google.apiKey` with the "API key" value listed in the "Simple
  API Access" section.
- Set `google.oauth.consumerKey` to the "Client ID" value listed under
  the "Client ID for web applications" section.
- Set `google.oauth.consumerSecret` to the "Client secret" value listed
  in the "Client ID for web applications" section.
- Fill the `google.oauth.serviceAccount` property with the "Email address"
  value given in the "Service account" section.

TODO: Add a picture of the API access page with appropriate highlighting.


### Local Development

To run the unit and integration tests bundled with the application, run
the command `mvn test`. To run a local development server, issue the command
`mvn appengine:devserver`.


### Deployment
