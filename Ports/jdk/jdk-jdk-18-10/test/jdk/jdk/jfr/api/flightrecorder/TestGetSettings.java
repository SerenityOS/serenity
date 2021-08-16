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

package jdk.jfr.api.flightrecorder;

import static jdk.test.lib.Asserts.assertEquals;
import static jdk.test.lib.Asserts.assertTrue;

import java.time.Duration;
import java.util.Map;

import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Recording;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.flightrecorder.TestGetSettings
 */
public class TestGetSettings {

    public static void main(String[] args) throws Throwable {
        final long minThresholdNanos = 1000000;
        final String dummyEventPath = "mydummy/event/path";
        final String myEventSettingName = String.valueOf(EventType.getEventType(MyEvent.class).getId());
        System.out.println("myEventSettingName=" + myEventSettingName);

        // Settings should be merged to include the most number of events (minimum threshold).
        Recording r1 = new Recording();
        r1.enable(MyEvent.class).withThreshold(Duration.ofNanos(minThresholdNanos * 3));
        r1.enable(MyEvent.class).withThreshold(Duration.ofNanos(minThresholdNanos * 2));
        r1.enable(dummyEventPath).withThreshold(Duration.ofNanos(minThresholdNanos));
        r1.start();

        ExpectedSetting[] expectedR1 = {
            new ExpectedSetting(myEventSettingName, "enabled", "true"),
            new ExpectedSetting(myEventSettingName, "threshold", Long.toString(minThresholdNanos * 2) + " ns"),
            new ExpectedSetting(dummyEventPath, "enabled", "true"),
            new ExpectedSetting(dummyEventPath, "threshold", Long.toString(minThresholdNanos) + " ns"),
        };

        verifySettings(r1.getSettings(), expectedR1);

        // Start another recording. Recorder settings should be merged from both recordings.
        Recording r2 = new Recording();
        r2.enable(MyEvent.class).withThreshold(Duration.ofNanos(minThresholdNanos));
        r2.disable(dummyEventPath);
        r2.start();

        ExpectedSetting[] expectedR2 = {
            new ExpectedSetting(myEventSettingName, "enabled", "true"),
            new ExpectedSetting(myEventSettingName, "threshold", Long.toString(minThresholdNanos) + " ns"),
            new ExpectedSetting(dummyEventPath, "enabled", "false")
        };

        verifySettings(r1.getSettings(), expectedR1);
        verifySettings(r2.getSettings(), expectedR2);

        // Stop first recording. Recorder should use settings from r2.
        r1.stop();
        verifySettings(r2.getSettings(), expectedR2);

        r2.stop();
        r1.close();
        r2.close();
    }

    private static void verifySettings(Map<String, String> settings, ExpectedSetting ... expectedSettings) {
        for (String name : settings.keySet()) {
            System.out.printf("Settings: %s=%s%n", name, settings.get(name));
        }
        for (ExpectedSetting expected : expectedSettings) {
            boolean isFound = false;
            for (String name : settings.keySet()) {
                if (name.contains(expected.name) && name.contains(expected.option)) {
                    final String value = settings.get(name);
                    String msg = String.format("got: %s=%s, expected: %s", name, value, expected.toString());
                    assertEquals(value, expected.value, msg);
                    System.out.println("OK: " + msg);
                    isFound = true;
                    break;
                }
            }
            assertTrue(isFound, "Missing setting " + expected.toString());
        }
    }

    private static class MyEvent extends Event {
    }

    private static class ExpectedSetting {
        String name;
        String option;
        String value;

        public ExpectedSetting(String name, String option, String value) {
            this.name = name;
            this.option = option;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format("name=%s, option=%s, value=%s", name, option, value);
        }
    }
}
