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
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.SimpleEvent;
import jdk.test.lib.jfr.SimpleEventHelper;

/**
 * @test
 * @summary Enable, disable, enable event during recording.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.event.TestRecordingEnableDisable
 */
public class TestRecordingEnableDisable {
    private static final String EVENT_PATH = "java.file_write";
    private static final Random rand = new Random(0);

    public static void main(String[] args) throws Throwable {

        Recording rA = new Recording();
        Recording rB = new Recording();

        rA.setName("rA");
        rB.setName("rB");

        final Path path = Paths.get(".", "my.jfr");
        rA.start();
        rB.start();

        for (int i = 0; i < 30; ++i) {
            SimpleEventHelper.createEvent(i);
            if (isMyEventEnabled(rA, rB)) {
                System.out.println("MyEvent enabled");
            }
            else {
                System.out.println("MyEvent disabled");
            }

            Files.write(path, "A".getBytes());
            if (isIoEnabled(rA, rB)) {
                System.out.println("IoEvent enabled");
            }
            else {
                System.out.println("IoEvent disabled");
            }
            Recording r = ((i % 2) == 0) ? rA : rB;
            updateSettings(r);
        }

        rA.stop();
        rB.stop();
        rA.close();
        rB.close();
    }


    private static void updateSettings(Recording r) {
        int operationIndex = rand.nextInt(4);
        switch (operationIndex) {
            case 0:
                SimpleEventHelper.enable(r, true);
                break;
            case 1:
                SimpleEventHelper.enable(r, false);
                break;
            case 2:
                r.enable(EVENT_PATH).withoutStackTrace();
                break;
            case 3:
                r.disable(EVENT_PATH);
                break;
            default:
                Asserts.fail("Wrong operataionIndex. Test error");
            }
    }

    private static boolean isMyEventEnabled(Recording rA, Recording rB) {
        long eventTypeId = EventType.getEventType(SimpleEvent.class).getId();
        String settingName = "@" + eventTypeId + "#enabled";
        return isEnabled(rA, settingName) || isEnabled(rB, settingName);
    }

    private static boolean isIoEnabled(Recording rA, Recording rB) {
        String settingName = EVENT_PATH + "#enabled";
        return isEnabled(rA, settingName) || isEnabled(rB, settingName);
    }

    private static boolean isEnabled(Recording r, String settingName) {
        System.out.printf("R(%s) %s=%s%n", r.getName(), settingName, r.getSettings().get(settingName));
        return Boolean.parseBoolean(r.getSettings().get(settingName));
    }
}
