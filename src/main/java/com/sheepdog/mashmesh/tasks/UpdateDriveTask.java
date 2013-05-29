package com.sheepdog.mashmesh.tasks;

import com.sheepdog.mashmesh.DriveExporter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class UpdateDriveTask extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        DriveExporter driveExporter = new DriveExporter();

        driveExporter.deleteAllFiles();
        driveExporter.snapshotUserTable();
        driveExporter.updateRideTable();

        resp.setContentType("text/plain");
        resp.getWriter().write(driveExporter.getFolder().getAlternateLink());
    }
}
