OpenMash Volunteer Ride Sharing (VRS)
=====================================

The OpenMash Volunteer Ride Sharing (VRS) application demonstrates how the OpenMash
integration bus extends the reach of VistA by leveraging SaaS offerings such as the Google
Application Engine (GAE)  and Google Apps. Volunteers and patients in the VA system
register to be matched up. When an appointment is scheduled for a patient, an available
volunteer is located, and a pickup request is sent to them with detailed directions.
Volunteers may accept or decline the request; if the request is declined, another volunteer
will be notified.

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
switch (shown below) in the "Status" columns of the services table to enable
the following services:

- Drive API
- Drive SDK
- Fusion Tables API
- Google Maps API v3
- Static Maps API

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

The following image shows where each field is found in the API Console:

![API Access Fields](https://raw.github.com/openmash/mashmesh/master/doc/client-credentials.png)


### Local Development

To run the unit and integration tests bundled with the application, run
the command `mvn test`. To run a local development server, issue the command
`mvn appengine:devserver`.


### Deployment

The OpenMash VRS application runs on Google AppEngine. In order to
deploy it, you will need to create an AppEngine project and configure
the application to deploy changes to it.

First, navigate to https://appengine.google.com/. Click on the "Create
Application" button to create the application. You may be prompted to
verify your Google account if you have no already done so. After your
account is verified, you will be presented with a "Create an Application"
screen. Choose a unique application identifier and application title for
the application, and leave the authentication open to all Google Accounts.
Click "Create Application" to complete the process.

Finally, edit `src/main/webapp/WEB-INF/appengine-web.xml` and change
the content of the `<application>openmash-rides</application>` section
to the application identifier you chose when creating your AppEngine
application. Execute the command `mvn appengine:update` to deploy your
application. When deployment finishes, you will be able to access the
application at `http://<your-application-identifier>.appspot.com`.
