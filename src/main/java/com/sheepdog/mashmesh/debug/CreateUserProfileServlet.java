package com.sheepdog.mashmesh.debug;

import com.sheepdog.mashmesh.models.UserProfile;
import com.sheepdog.mashmesh.servlets.EditUserProfileServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CreateUserProfileServlet extends HttpServlet {
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String email = req.getParameter("email");
        UserProfile userProfile = UserProfile.getByEmail(email);

        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setUserId(email);
            userProfile.setEmail(email);
        }

        req.setAttribute("userProfile", userProfile);

        new EditUserProfileServlet().doPost(req, resp);

        resp.setStatus(200);
        resp.setContentType("text/plain");
        resp.getWriter().write("OK");
    }
}
