package com.sheepdog.mashmesh.debug;

import com.googlecode.objectify.Objectify;
import com.sheepdog.mashmesh.models.OfyService;
import com.sheepdog.mashmesh.models.RideRecord;
import com.sheepdog.mashmesh.models.UserProfile;
import com.sheepdog.mashmesh.models.VolunteerProfile;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ResetDatastoreServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Objectify ofy = OfyService.ofy();

        ofy.delete(ofy.query(UserProfile.class).chunkSize(100));
        ofy.delete(ofy.query(VolunteerProfile.class).chunkSize(100));
        ofy.delete(ofy.query(RideRecord.class).chunkSize(100));

        int documentsDeleted = VolunteerProfile.clearIndex();

        resp.setContentType("text/plain");
        resp.getWriter().println(documentsDeleted + " text search documents deleted");
        resp.getWriter().println("OK");
    }
}
