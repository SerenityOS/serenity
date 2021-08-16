/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jmx;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import jdk.management.jfr.FlightRecorderMXBean;
import jdk.test.lib.Asserts;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.TestRecordingOptions
 */
public class TestRecordingOptions {
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void main(String[] args) throws Exception {
        Map<String, String> options = new HashMap<>();
        options.put("name", "myName");
        options.put("maxAge", "2 h");
        options.put("maxSize", "1234567890");
        options.put("dumpOnExit", "false");
        options.put("disk", "false");
        options.put("duration", "1 h"); // don't want recording to stop
        options.put("destination", "." + File.separator + "dump.jfr");
        FlightRecorderMXBean bean = JmxHelper.getFlighteRecorderMXBean();
        long recId = bean.newRecording();
        Map<String, String> defaults = bean.getRecordingOptions(recId);
        bean.setRecordingOptions(recId, options);

        // Verify that all options have been set. We only check the option we
        // have set. Unknown options are ignored.
        Map<String, String> outOptions = bean.getRecordingOptions(recId);
        logMap("set options", options);
        logMap("get options", outOptions);
        for (String key : options.keySet()) {
            Asserts.assertTrue(outOptions.containsKey(key), "Missing key " + key);
            Asserts.assertEquals(options.get(key), outOptions.get(key), "Wrong value for key " + key);
        }

        // Verify options in RecordingInfo
        Asserts.assertEquals(outOptions.get("name"), "myName", "Wrong name");
        Asserts.assertEquals(outOptions.get("maxAge"), "2 h", "Wrong maxAge");
        Asserts.assertEquals(outOptions.get("maxSize"), "1234567890", "Wrong maxSize");
        Asserts.assertEquals(outOptions.get("dumpOnExit"), "false", "Wrong dumpOnExit");
        Asserts.assertEquals(outOptions.get("disk"), "false", "Wrong disk");
        Asserts.assertEquals(outOptions.get("duration"), "1 h", "Wrong duration");
        Asserts.assertEquals(outOptions.get("destination"), "." + File.separator + "dump.jfr", "Wrong destination");

        // try empty map
        bean.setRecordingOptions(recId, new HashMap<>());

        // try map that does not have string keys
        Map<Integer, String> invalidKeys = new HashMap<>();
        invalidKeys.put(4711, "value");
        try {
            bean.setRecordingOptions(recId, (Map) invalidKeys);
            throw new Error("Expected IllagalStateException for non String key");
        } catch (IllegalArgumentException iae) {
            // OK, as expected
        }
        // try map that does not have string values
        Map<String, Integer> invalidValues = new HashMap<>();
        invalidValues.put("duration", 4711);
        try {
            bean.setRecordingOptions(recId, (Map) invalidKeys);
            throw new Error("Expected IllagalStateException for non String value");
        } catch (IllegalArgumentException iae) {
            // OK, as expected
        }

        // Try one incorrect value, and make sure non
        // of the other values are set.
        Map<String, String> lastIncorrect = new LinkedHashMap<>();
        lastIncorrect.put("duration", "10 h");
        lastIncorrect.put("whatever", "4711");
        try {
            bean.setRecordingOptions(recId, lastIncorrect);
            throw new Error("Expected IllagalStateException for incorrect key");
        } catch (IllegalArgumentException iae) {
            // ok
            Asserts.assertEquals("1 h", bean.getRecordingOptions(recId).get("duration"));
        }

        // verify that defaults are set back, if we use null
        Map<String, String> nullMap = new HashMap<>();
        nullMap.put("name", null);
        nullMap.put("maxAge", null);
        nullMap.put("maxSize", null);
        nullMap.put("dumpOnExit", null);
        nullMap.put("disk", null);
        nullMap.put("duration", null);
        nullMap.put("destination", null);
        bean.setRecordingOptions(recId, nullMap);
        Asserts.assertEquals(bean.getRecordingOptions(recId), defaults);

        bean.closeRecording(recId);
    }

    private static void logMap(String name, Map<String, String> map) {
        for (String key : map.keySet()) {
            System.out.printf("%s: %s=%s%n", name, key, map.get(key));
        }
    }
}
