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

package jdk.jfr.api.recording.event;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import jdk.jfr.EventType;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.SimpleEvent;
import jdk.test.lib.jfr.SimpleEventHelper;

/**
 * @test
 * @summary Enable, disable, enable event during recording.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.event.TestReEnableMultiple
 */
public class TestReEnableMultiple {
    private static final String EVENT_PATH = EventNames.FileWrite;
    private static final Random rand = new Random(0);

    public static void main(String[] args) throws Throwable {
        Recording rA = new Recording();
        Recording rB = new Recording();

        final Path path = Paths.get(".", "dummy.txt").toAbsolutePath();
        rA.start();
        rB.start();

        SimpleEventHelper.enable(rA, false);
        SimpleEventHelper.enable(rA, false);

        int expectedMyEvents = 0;
        int expectedIoEvents = 0;
        for (int i = 0; i < 20; ++i) {
            SimpleEventHelper.createEvent(i);
            if (isMyEventEnabled(rA, rB)) {
                expectedMyEvents++;
                System.out.println("Expect MyEvent.id " + i);
            }

            Files.write(path, "A".getBytes());
            if (isIoEnabled(rA, rB)) {
                expectedIoEvents++;
            }

            for (int j = 0; j < 4; ++j) {
                Recording r = (rand.nextInt(2) == 0) ? rA : rB;
                updateSettings(r);
            }
        }

        rA.stop();
        rB.stop();

        verifyEventCount(rA, expectedMyEvents, expectedIoEvents, path);
        verifyEventCount(rB, expectedMyEvents, expectedIoEvents, path);

        rA.close();
        rB.close();
    }

    private static void verifyEventCount(Recording r, int expectedMyEvents, int expectedIoEvents, Path path) throws Exception {
        int actualMyEvents = 0;
        int actualIoEvents = 0;
        for (RecordedEvent event : Events.fromRecording(r)) {
            if (Events.isEventType(event, EVENT_PATH)) {
                if (path.toString().equals(Events.assertField(event, "path").getValue())) {
                    actualIoEvents++;
                }
            } else {
                Asserts.assertTrue(Events.isEventType(event, SimpleEvent.class.getName()));
                System.out.println("Got MyEvent.id=" + Events.assertField(event, "id").getValue());
                actualMyEvents++;
            }
        }
        System.out.printf("MyEvents: expected=%d, actual=%d%n", expectedMyEvents, actualMyEvents);
        System.out.printf("IoEvents: expected=%d, actual=%d%n", expectedIoEvents, actualIoEvents);
        Asserts.assertEquals(expectedMyEvents, actualMyEvents, "Wrong number of MyEvents");
        Asserts.assertEquals(expectedIoEvents, actualIoEvents, "Wrong number of IoEvents");
    }

    private static void updateSettings(Recording r) {
        boolean doEnable = rand.nextInt(3) == 0; // Disable 2 of 3, since event
                                                 // is enabled by union of
                                                 // recordings.
        boolean doMyEvent = rand.nextInt(2) == 0;
        if (doMyEvent) {
            SimpleEventHelper.enable(r, doEnable);
        } else {
            if (doEnable) {
                r.enable(EVENT_PATH).withoutStackTrace();
            } else {
                r.disable(EVENT_PATH);
            }
        }
    }

    private static boolean isMyEventEnabled(Recording rA, Recording rB) {
        long eventTypeId = EventType.getEventType(SimpleEvent.class).getId();
        String settingName = eventTypeId + "#enabled";
        return isEnabled(rA, settingName) || isEnabled(rB, settingName);
    }

    private static boolean isIoEnabled(Recording rA, Recording rB) {
        String settingName = EVENT_PATH + "#enabled";
        return isEnabled(rA, settingName) || isEnabled(rB, settingName);
    }

    private static boolean isEnabled(Recording r, String settingName) {
        // System.out.printf("R(%s) %s=%s%n", r.getName(), settingName,
        // r.getSettings().get(settingName));
        return Boolean.parseBoolean(r.getSettings().get(settingName));
    }
}
