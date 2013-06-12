/**
 *    Copyright 2013 Talend Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.sheepdog.mashmesh.debug;

import com.googlecode.objectify.Objectify;
import com.sheepdog.mashmesh.models.*;

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
        ofy.delete(ofy.query(RideRequest.class).chunkSize(100));

        int documentsDeleted = VolunteerProfile.clearIndex();

        resp.setContentType("text/plain");
        resp.getWriter().println(documentsDeleted + " text search documents deleted");
        resp.getWriter().println("OK");
    }
}
