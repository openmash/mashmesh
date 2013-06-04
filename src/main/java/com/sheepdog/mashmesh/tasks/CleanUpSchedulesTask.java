package com.sheepdog.mashmesh.tasks;

import com.sheepdog.mashmesh.VolunteerScheduleScrubber;
import org.joda.time.DateTime;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CleanUpSchedulesTask extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long cutOffTimeMillis = DateTime.now().minusDays(1).getMillis();
        VolunteerScheduleScrubber scrubber = new VolunteerScheduleScrubber();
        scrubber.run(cutOffTimeMillis);

        resp.setContentType("text/plain");
        resp.getWriter().write("OK");
    }
}
