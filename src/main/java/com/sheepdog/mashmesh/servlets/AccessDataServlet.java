package com.sheepdog.mashmesh.servlets;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.sheepdog.mashmesh.DriveExporter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AccessDataServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();

        if (!userService.isUserAdmin()) {
            resp.setStatus(403);
            return;
        }

        DriveExporter driveExporter = new DriveExporter();
        String folderUrl = driveExporter.shareFolder(user.getEmail());
        resp.sendRedirect(folderUrl);
    }
}
