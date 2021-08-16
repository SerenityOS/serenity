/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.jfr.tool;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import jdk.jfr.Timespan;
import jdk.jfr.Timestamp;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.consumer.RecordingFile;
import jdk.jfr.tool.JSONValue.JSONArray;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;

/**
 * @test
 * @key jfr
 * @summary Tests print --json
 * @requires vm.hasJFR
 *
 * @library /test/lib /test/jdk
 * @modules jdk.jfr
 *
 * @run main/othervm jdk.jfr.tool.TestPrintJSON
 */
public class TestPrintJSON {

    public static void main(String... args) throws Throwable {

        Path recordingFile = ExecuteHelper.createProfilingRecording().toAbsolutePath();

        OutputAnalyzer output = ExecuteHelper.jfr("print", "--json", "--stack-depth", "999", recordingFile.toString());
        String json = output.getStdout();

        JSONValue o = JSONValue.parse(json);
        JSONValue recording = o.get("recording");
        JSONArray jsonEvents = recording.get("events").asArray();
        List<RecordedEvent> events = RecordingFile.readAllEvents(recordingFile);
        Collections.sort(events, new EndTicksComparator());
        // Verify events are equal
        Iterator<RecordedEvent> it = events.iterator();
        for (JSONValue jsonEvent : jsonEvents) {
            RecordedEvent recordedEvent = it.next();
            String typeName = recordedEvent.getEventType().getName();
            Asserts.assertEquals(typeName, jsonEvent.get("type").asString());
            assertEquals(jsonEvent, recordedEvent);
        }
        Asserts.assertFalse(events.size() != jsonEvents.size(), "Incorrect number of events");
    }

    private static void assertEquals(Object jsonObject, Object jfrObject) throws Exception {
        // Check object
        if (jfrObject instanceof RecordedObject) {
            JSONValue values = ((JSONValue) jsonObject).get("values");
            RecordedObject recObject = (RecordedObject) jfrObject;
            Asserts.assertEquals(values.size(), recObject.getFields().size());
            for (ValueDescriptor v : recObject.getFields()) {
                String name = v.getName();
                Object jsonValue = values.get(name);
                Object expectedValue = recObject.getValue(name);
                if (v.getAnnotation(Timestamp.class) != null) {
                    // Make instant of OffsetDateTime
                    String text = ((JSONValue) jsonValue).asString();
                    jsonValue = OffsetDateTime.parse(text).toInstant().toString();
                    expectedValue = recObject.getInstant(name);
                }
                if (v.getAnnotation(Timespan.class) != null) {
                    expectedValue = recObject.getDuration(name);
                }
                assertEquals(jsonValue, expectedValue);
                return;
            }
        }
        // Check array
        if (jfrObject != null && jfrObject.getClass().isArray()) {
            Object[] jfrArray = (Object[]) jfrObject;
            JSONArray jsArray = ((JSONArray) jsonObject);
            for (int i = 0; i < jfrArray.length; i++) {
                assertEquals(jsArray.get(i), jfrArray[i]);
            }
            return;
        }
        String jsonText = String.valueOf(jsonObject);
        // Double.NaN / Double.Inifinity is not supported by JSON format,
        // use null
        if (jfrObject instanceof Double) {
            double expected = ((Double) jfrObject);
            if (Double.isInfinite(expected) || Double.isNaN(expected)) {
                Asserts.assertEquals("null", jsonText);
                return;
            }
            double value = Double.parseDouble(jsonText);
            Asserts.assertEquals(expected, value);
            return;
        }
        // Float.NaN / Float.Inifinity is not supported by JSON format,
        // use null
        if (jfrObject instanceof Float) {
            float expected = ((Float) jfrObject);
            if (Float.isInfinite(expected) || Float.isNaN(expected)) {
                Asserts.assertEquals("null", jsonText);
                return;
            }
            float value = Float.parseFloat(jsonText);
            Asserts.assertEquals(expected, value);
            return;
        }
        if (jfrObject instanceof Integer) {
            Integer expected = ((Integer) jfrObject);
            double value = Double.parseDouble(jsonText);
            Asserts.assertEquals(expected.doubleValue(), value);
            return;
        }
        if (jfrObject instanceof Long) {
            Long expected = ((Long) jfrObject);
            double value = Double.parseDouble(jsonText);
            Asserts.assertEquals(expected.doubleValue(), value);
            return;
        }

        String jfrText = String.valueOf(jfrObject);
        Asserts.assertEquals(jfrText, jsonText, "Primitive values don't match. JSON = " + jsonText);
    }
}
