package com.sheepdog.mashmesh;

import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.fusiontables.Fusiontables;
import com.google.api.services.fusiontables.model.Column;
import com.google.api.services.fusiontables.model.Table;
import com.sheepdog.mashmesh.models.OfyService;
import com.sheepdog.mashmesh.models.RideRecord;
import com.sheepdog.mashmesh.models.UserProfile;
import com.sheepdog.mashmesh.util.FusionTableContentWriter;
import com.sheepdog.mashmesh.util.GoogleApiUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class DriveExporter {
    private static final String DRIVE_FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
    private static final String DRIVE_FOLDER_TITLE = "OpenMash Rides Data";
    private static final String DRIVE_FOLDER_DESCRIPTION =
            "Fusion Tables for demographic data captured by OpenMash Rides";

    private static final String RIDE_TABLE_TITLE = "Historical Rides";
    private static final String RIDE_TABLE_DESCRIPTION = "Ride details for all rides";

    private final Drive drive;
    private final Fusiontables fusiontables;
    private ParentReference folder;

    public DriveExporter() throws IOException {
        try {
            drive = GoogleApiUtils.getDrive();
            fusiontables = GoogleApiUtils.getFusiontables();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }

        File folderFile = getFolder();
        folder = new ParentReference().setId(folderFile.getId());
    }

    private File findFileByTitle(String title) throws IOException {
        // TODO: Can this be made deterministic in the presence of duplicates?
        String query = String.format("title = '%s'", title);
        FileList matchingFiles = drive.files().list()
                .setQ(query)
                .execute();

        if (!matchingFiles.getItems().isEmpty()) {
            return matchingFiles.getItems().get(0);
        } else {
            return null;
        }
    }

    private void setPermissions(File file) throws IOException {
        // Readable by anyone who has the URL
        Permission readPermission = new Permission();
        readPermission.setRole("reader");
        readPermission.setType("anyone");
        readPermission.setValue("ignored");
        readPermission.setWithLink(true);
        drive.permissions().insert(file.getId(), readPermission).execute();
    }

    public File getFolder() throws IOException {
        File folder = findFileByTitle(DRIVE_FOLDER_TITLE);

        if (folder == null) {
            folder = new File()
                    .setTitle(DRIVE_FOLDER_TITLE)
                    .setDescription(DRIVE_FOLDER_DESCRIPTION)
                    .setMimeType(DRIVE_FOLDER_MIME_TYPE)
                    .setShared(true);

            folder = drive.files().insert(folder).execute();
            setPermissions(folder);
        }

        return folder;
    }

    // TODO: Testing
    public void deleteAllFiles() throws IOException {
        for (File file : drive.files().list().execute().getItems()) {
            drive.files().delete(file.getId()).execute();
        }

        folder = new ParentReference().setId(getFolder().getId());
    }

    public void addToFolder(File file) throws IOException {
        drive.parents().insert(file.getId(), folder).execute();
    }

    public void snapshotUserTable() throws IOException {
        DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
        String timestamp = formatter.print(DateTime.now());
        Table patientTable = new Table()
                .setName("User Profile Data " + timestamp)
                .setDescription("Locations of patients and volunteers as of " + timestamp)
                .setIsExportable(true)
                .setColumns(Arrays.asList(
                        new Column().setName("User Type").setType("STRING"),
                        new Column().setName("Location").setType("LOCATION")
                ));

        patientTable = fusiontables.table().insert(patientTable).execute();

        FusionTableContentWriter fusionTableWriter = new FusionTableContentWriter(patientTable);

        for (UserProfile userProfile : UserProfile.listAll()) {
            fusionTableWriter.writeRecord(userProfile.getType().name(), userProfile.getLocation());
        }

        AbstractInputStreamContent streamContent = fusionTableWriter.getInputStreamContent();
        fusiontables.table().importRows(patientTable.getTableId(), streamContent).execute();

        File patientFile = findFileByTitle(patientTable.getName());
        setPermissions(patientFile);
        addToFolder(patientFile);
    }

    private Table getRideTable() throws IOException {
        File rideFile = findFileByTitle(RIDE_TABLE_TITLE);
        Table rideTable;

        if (rideFile != null) {
            rideTable = fusiontables.table().get(rideFile.getId()).execute();
        } else {
            rideTable = new Table()
                    .setName(RIDE_TABLE_TITLE)
                    .setDescription(RIDE_TABLE_DESCRIPTION)
                    .setIsExportable(true)
                    .setColumns(Arrays.asList(
                            new Column().setName("Volunteer Location").setType("LOCATION"),
                            new Column().setName("Departure Time").setType("DATETIME"),
                            new Column().setName("Patient Location").setType("LOCATION"),
                            new Column().setName("Pickup Time").setType("DATETIME"),
                            new Column().setName("Appointment Address").setType("STRING"),
                            new Column().setName("Appointment Location").setType("LOCATION"),
                            new Column().setName("Appointment Time").setType("DATETIME"),
                            new Column().setName("Distance (miles)").setType("NUMBER"),
                            new Column().setName("Travel Time (minutes)").setType("NUMBER")
                    ));

            rideTable = fusiontables.table().insert(rideTable).execute();
            rideFile = findFileByTitle(RIDE_TABLE_TITLE);
            setPermissions(rideFile);
            addToFolder(rideFile);
        }

        return rideTable;
    }

    private Collection<RideRecord> getExportableRideRecords() {
        Collection<RideRecord> rideRecords = new ArrayList<RideRecord>();

        for (RideRecord rideRecord : RideRecord.getExportableRecords()) {
            rideRecords.add(rideRecord);
        }

        return rideRecords;
    }

    private void exportRideRecords(Table rideTable, Collection<RideRecord> rideRecords) throws IOException {
        FusionTableContentWriter fusionTableWriter = new FusionTableContentWriter(rideTable);

        for (RideRecord rideRecord : rideRecords) {
            fusionTableWriter.writeRecord(
                    rideRecord.getVolunteerLocation(),
                    rideRecord.getDepartureTime(),
                    rideRecord.getPatientLocation(),
                    rideRecord.getPickupTime(),
                    rideRecord.getAppointmentAddress(),
                    rideRecord.getAppointmentLocation(),
                    rideRecord.getAppointmentTime(),
                    rideRecord.getDistanceMiles(),
                    rideRecord.getTripMinutes()
            );
        }

        AbstractInputStreamContent streamContent = fusionTableWriter.getInputStreamContent();
        fusiontables.table().importRows(rideTable.getTableId(), streamContent).execute();
    }

    private void markRideRecordsUnexportable(Collection<RideRecord> rideRecords) {
        for (RideRecord rideRecord : rideRecords) {
            rideRecord.setIsExported(true);
        }

        OfyService.ofy().put(rideRecords);
    }

    public void updateRideTable() throws IOException {
        Table rideTable = getRideTable();
        Collection<RideRecord> rideRecords = getExportableRideRecords();
        exportRideRecords(rideTable, rideRecords);
        markRideRecordsUnexportable(rideRecords);
    }
}
