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
package com.sheepdog.mashmesh.util;

import com.google.api.services.fusiontables.model.Column;
import com.google.api.services.fusiontables.model.Table;
import com.google.appengine.api.datastore.GeoPt;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 * FusionTableContentWriterTest checks that the FusionTableContentWriter produces
 * CSV files in the format the Google Fusion Tables expects. We are specifically
 * concerned with geospatial and datetime encoding, as well as escaping of special
 * characters.
 */
public class FusionTableContentWriterTest {
    private static String getOutputString(FusionTableContentWriter writer) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writer.getInputStreamContent().writeTo(outputStream);
        return new String(outputStream.toByteArray(), "UTF-8");
    }

    @Test
    public void testComplexRow() throws IOException {
        Table table = new Table().setColumns(Arrays.asList(
                new Column().setName("first").setType("LOCATION"),
                new Column().setName("second").setType("STRING"),
                new Column().setName("third").setType("STRING"),
                new Column().setName("fourth").setType("DATETIME")
        ));

        FusionTableContentWriter writer = new FusionTableContentWriter(table);
        DateTime dateTime = DateTime.parse("2013-05-22T11:00:00-03:00");
        writer.writeRecord(new GeoPt(44.990288f, -64.131035f), "a string", "another string", dateTime);

        Assert.assertEquals(
                "\"first\",\"second\",\"third\",\"fourth\"\n" +
                "\"44.990288 -64.131035\",\"a string\",\"another string\",\"2013-05-22T11:00:00-03:00\"\n",
                getOutputString(writer)
        );
    }

    @Test
    public void testSingleColumn() throws IOException {
        Column column = new Column().setName("string column").setType("STRING");
        Table table = new Table().setColumns(Collections.singletonList(column));
        FusionTableContentWriter writer = new FusionTableContentWriter(table);
        writer.writeRecord("First string in the column");
        writer.writeRecord("Second string");

        Assert.assertEquals(
                "\"string column\"\n" +
                "\"First string in the column\"\n" +
                "\"Second string\"\n",
                getOutputString(writer)
        );
    }

    @Test
    public void testMultipleColumns() throws IOException {
        Table table = new Table().setColumns(Arrays.asList(
            new Column().setName("first column").setType("STRING"),
            new Column().setName("second column").setType("STRING")
        ));

        FusionTableContentWriter writer = new FusionTableContentWriter(table);
        writer.writeRecord("First string", "Second");
        writer.writeRecord("Next row string", "");
        writer.writeRecord("", "not empty");

        Assert.assertEquals(
                "\"first column\",\"second column\"\n" +
                "\"First string\",\"Second\"\n" +
                "\"Next row string\",\"\"\n" +
                "\"\",\"not empty\"\n",
                getOutputString(writer)
        );
    }

    @Test
    public void testGeoptEncoding() throws IOException {
        Table table = new Table().setColumns(Arrays.asList(
                new Column().setName("name").setType("STRING"),
                new Column().setName("location").setType("LOCATION")
        ));

        FusionTableContentWriter writer = new FusionTableContentWriter(table);
        writer.writeRecord("John Smith", new GeoPt(40.767838f, -73.981972f));
        writer.writeRecord("Jane Adams", new GeoPt(44.990288f, -64.131035f));
        writer.writeRecord("Bob Mcconnell", new GeoPt(46.099918f, -60.754677f));

        Assert.assertEquals(
                "\"name\",\"location\"\n" +
                "\"John Smith\",\"40.767838 -73.981972\"\n" +
                "\"Jane Adams\",\"44.990288 -64.131035\"\n" +
                "\"Bob Mcconnell\",\"46.099918 -60.754677\"\n",
                getOutputString(writer)
        );
    }

    @Test
    public void testDatetimeEncoding() throws IOException {
        Table table = new Table().setColumns(Arrays.asList(
                new Column().setName("timestamp").setType("DATETIME"),
                new Column().setName("event").setType("STRING")
        ));

        FusionTableContentWriter writer = new FusionTableContentWriter(table);
        DateTime dateTime = DateTime.parse("2013-06-01T23:00:01-07:00");
        writer.writeRecord(dateTime, "appointment scheduled");

        Assert.assertEquals(
                "\"timestamp\",\"event\"\n" +
                "\"2013-06-01T23:00:01-07:00\",\"appointment scheduled\"\n",
                getOutputString(writer)
        );
    }

    @Test
    public void testEscaping() throws IOException {
        Table table = new Table().setColumns(Collections.singletonList(
                 new Column().setName("comments").setType("STRING")
        ));

        FusionTableContentWriter writer = new FusionTableContentWriter(table);
        writer.writeRecord("I will try to be at least five minutes early.\nIf I don't see you, I'll \"honk\"");
        writer.writeRecord("this is the second comment");

        Assert.assertEquals(
                "\"comments\"\n" +
                "\"I will try to be at least five minutes early.\n" +
                "If I don't see you, I'll \"\"honk\"\"\"\n" +
                "\"this is the second comment\"\n",
                getOutputString(writer)
        );
    }
}
