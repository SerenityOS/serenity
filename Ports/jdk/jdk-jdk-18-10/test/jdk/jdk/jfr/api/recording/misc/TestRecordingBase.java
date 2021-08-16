/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.recording.misc;

import static jdk.test.lib.Asserts.assertEquals;
import static jdk.test.lib.Asserts.assertFalse;
import static jdk.test.lib.Asserts.assertNotEquals;
import static jdk.test.lib.Asserts.assertNotNull;
import static jdk.test.lib.Asserts.assertNull;
import static jdk.test.lib.Asserts.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

import jdk.jfr.Recording;
import jdk.jfr.RecordingState;

/**
 * @test
 * @summary Basic tests for Recording
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.misc.TestRecordingBase
 */
public class TestRecordingBase {

    public static void main(String[] args) throws Throwable {
        testUninitialized();
        testUniqueIdentifier();
        testSetGetName();
        testSetGetDuration();
        testSetGetMaxAge();
        testSetGetDestination();
        testSetGetDumpOnExit();
        testSetGetToDisk();
        testSetGetToMaxSize();
        testGetSettings();
    }

    public static void testUninitialized() throws Throwable {
       Recording r = new Recording();
       assertNull(r.getDuration(), "Wrong uninitialized duration");
       assertNull(r.getStartTime(), "Wrong uninitialized startTime");
       assertNull(r.getStopTime(), "Wrong uninitialized stopTime");
       assertNull(r.getDestination(), "Wrong uninitialized destination");
       assertNull(r.getMaxAge(), "Wrong uninitialized maxAge");
       assertEquals(0L, r.getMaxSize(), "Wrong uninitialized maxSize"); // TODO: Should this be null? Why Long if never null?
       assertEquals(0L, r.getSize(), "Wrong uninitialized size");
       assertNotNull(r.getName(), "Uninitialized name should not be null");
       assertFalse(r.getName().isEmpty(), "Uninitialized name should not be empty");
       assertEquals(r.getState(), RecordingState.NEW, "Wrong uninitialized state");
       assertTrue(r.getSettings().isEmpty(), "Uninitialized settings should be empty");
       r.close();
    }

    public static void testUniqueIdentifier() throws Throwable {
        Recording r1 = new Recording();
        Recording r2 = new Recording();
        assertNotEquals(r1.getId(), r2.getId(), "Same identifier");
        r1.close();
        r2.close();
    }

    public static void testSetGetName() throws Throwable {
        Recording r = new Recording();
        final String name = "TestName";
        r.setName(name);
        assertEquals(name, r.getName(), "Wrong set/get name");
        r.close();
    }

    public static void testSetGetDuration() throws Throwable {
        Recording r = new Recording();
        final Duration duration = Duration.ofSeconds(60).plusMillis(50);
        r.setDuration(duration);
        assertEquals(duration, r.getDuration(), "Wrong set/get duration");
        r.close();
    }

    public static void testSetGetMaxAge() throws Throwable {
        Recording r = new Recording();
        final Duration maxAge = Duration.ofSeconds(60).plusMillis(50);
        r.setMaxAge(maxAge);
        assertEquals(maxAge, r.getMaxAge(), "Wrong set/get maxAge");
        r.close();
    }

    public static void testSetGetDestination() throws Throwable {
        Recording r = new Recording();
        final Path destination = Paths.get(".", "testSetGetDestination.jfr");
        r.setDestination(destination);
        assertEquals(destination, r.getDestination(), "Wrong set/get destination");
        r.close();
    }

    public static void testSetGetDumpOnExit() throws Throwable {
        Recording r = new Recording();
        r.setDumpOnExit(true);
        assertTrue(r.getDumpOnExit(), "Wrong set/get dumpOnExit true");
        r.setDumpOnExit(false);
        assertFalse(r.getDumpOnExit(), "Wrong set/get dumpOnExit false");
        r.close();
    }

    public static void testSetGetToDisk() throws Throwable {
        Recording r = new Recording();
        r.setToDisk(true);
        assertTrue(r.isToDisk(), "Wrong set/get isToDisk true");
        r.setToDisk(false);
        assertFalse(r.isToDisk(), "Wrong set/get isToDisk false");
        r.close();
    }

    public static void testSetGetToMaxSize() throws Throwable {
        Recording r = new Recording();
        final long maxSize = 10000000;
        r.setMaxSize(maxSize);
        assertEquals(maxSize, r.getMaxSize(), "Wrong set/get maxSize");
        r.close();
    }

    public static void testGetSettings() throws Throwable {
        String eventPath = "my/test/enabledPath";
        String settingName = "myTestSetting";
        String settingValue = "myTestValue";

        Recording r = new Recording();
        r.enable(eventPath).with(settingName, settingValue);

        boolean isEnabledPathFound = false;
        boolean isSettingFound = false;
        Map<String, String> settings = r.getSettings();
        for (String name : settings.keySet()) {
            System.out.println("name=" + name + ", value=" + settings.get(name));
            if (name.contains(eventPath) && name.contains("#enabled")) {
                isEnabledPathFound = true;
                assertEquals("true", settings.get(name), "Wrong value for enabled path: " + name);
            }
            if  (name.contains(eventPath) && name.contains(settingName)) {
                isSettingFound = true;
                assertEquals(settingValue, settings.get(name), "Wrong value for setting: " + name);
            }
        }
        assertTrue(isEnabledPathFound, "Enabled path not found in settings");
        assertTrue(isSettingFound, "Test setting not found in settings");
        r.close();
    }

}
