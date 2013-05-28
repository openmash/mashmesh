package com.sheepdog.mashmesh.util;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.fusiontables.Fusiontables;
import com.google.api.services.fusiontables.model.Column;
import com.google.api.services.fusiontables.model.Table;
import com.google.appengine.api.datastore.GeoPt;
import com.sheepdog.mashmesh.models.UserProfile;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DriveExporter {
    private static String DRIVE_FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
    private static String DRIVE_FOLDER_TITLE = "OpenMash Rides Data";
    private static String DRIVE_FOLDER_DESCRIPTION = "Fusion Tables for demographic data captured by OpenMash Rides";

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
            folder = new File();
            folder.setTitle(DRIVE_FOLDER_TITLE);
            folder.setDescription(DRIVE_FOLDER_DESCRIPTION);
            folder.setMimeType(DRIVE_FOLDER_MIME_TYPE);
            folder.setShared(true);

            folder = drive.files().insert(folder).execute();
            setPermissions(folder);
        }

        return folder;
    }

    // TODO: Testing
    public void deleteFolder() throws IOException {
        drive.files().delete(getFolder().getId()).execute();
        folder = new ParentReference().setId(getFolder().getId());
    }

    private static String formatLatLng(GeoPt geoPt) {
        return String.format("%f %f", geoPt.getLatitude(), geoPt.getLongitude());
    }

    private static void writeCsvLine(StringBuilder stringBuilder, String... fields) {
        for (int i = 0; i < fields.length; i++) {
            String separator =  (i < fields.length - 1) ? "," : "\n";
            stringBuilder.append(fields[i])
                         .append(separator);
        }
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

        StringBuilder csvBuilder = new StringBuilder();
        List<String> columnNames = new ArrayList<String>();

        for (Column column : patientTable.getColumns()) {
            columnNames.add(column.getName());
        }

        writeCsvLine(csvBuilder, columnNames.toArray(new String[columnNames.size()]));

        for (UserProfile userProfile : UserProfile.listAll()) {
            writeCsvLine(csvBuilder, userProfile.getType().name(), formatLatLng(userProfile.getLocation()));
        }

        InputStream csvInputStream = new ByteArrayInputStream(csvBuilder.toString().getBytes());
        InputStreamContent csvContent = new InputStreamContent("application/octet-stream", csvInputStream);
        fusiontables.table().importRows(patientTable.getTableId(), csvContent)
                .setStartLine(0)
                .execute();

        File patientFile = findFileByTitle(patientTable.getName());
        setPermissions(patientFile);
        drive.parents().insert(patientFile.getId(), folder).execute();
    }
}
