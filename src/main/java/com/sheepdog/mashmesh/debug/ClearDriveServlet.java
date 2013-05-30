package com.sheepdog.mashmesh.debug;

import com.sheepdog.mashmesh.DriveExporter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ClearDriveServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        DriveExporter driveExporter = new DriveExporter();
        driveExporter.deleteAllFiles();

        resp.setContentType("text/plain");
        resp.getWriter().write("OK");
    }
}
