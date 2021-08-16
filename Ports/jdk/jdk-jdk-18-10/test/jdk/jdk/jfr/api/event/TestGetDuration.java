/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.api.event;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.CommonHelper;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.SimpleEvent;

/**
 * @test
 * @summary Test for RecordedEvent.getDuration()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.event.TestGetDuration
 */
public class TestGetDuration {

    private static final int DURATIONAL_EVENT_ID = 1;
    private static final int INSTANT_EVENT_ID = 2;

    public static void main(String[] args) throws Exception {
        verifyCustomEvents();
        verifyNativeEvents();
    }

    private static void verifyCustomEvents() throws Exception {
        boolean fastTimeEnabled = CommonHelper.hasFastTimeEnabled();
        System.out.println("Fast time enabled: " + fastTimeEnabled);
        Recording r = new Recording();
        r.enable(SimpleEvent.class).withoutStackTrace();
        r.enable(EventNames.CPUTimeStampCounter); // for debugging purposes
        r.start();

        SimpleEvent durational = new SimpleEvent();
        durational.begin();
        durational.id = DURATIONAL_EVENT_ID;
        if (!fastTimeEnabled) {
            // if we have a low resolution clock sleep until time changes
            CommonHelper.waitForSystemCurrentMillisToChange();
        }
        durational.end();
        durational.commit();

        SimpleEvent instant = new SimpleEvent();
        instant.id = INSTANT_EVENT_ID;
        instant.commit();

        r.stop();

        List<RecordedEvent> recordedEvents = Events.fromRecording(r);
        List<RecordedEvent> testEvents = new ArrayList<>();
        for (RecordedEvent e : recordedEvents) {
            System.out.println(e); // for debugging time related issues
            if (!e.getEventType().getName().equals(EventNames.CPUTimeStampCounter)) {
                testEvents.add(e);
            }
        }
        Events.hasEvents(testEvents);
        for (RecordedEvent re : testEvents) {
            int id = re.getValue("id");
            Asserts.assertEquals(re.getDuration(), Duration.between(re.getStartTime(), re.getEndTime()));
            switch (id) {
                case DURATIONAL_EVENT_ID:
                    Asserts.assertTrue(!re.getDuration().isNegative() && !re.getDuration().isZero());
                    break;
                case INSTANT_EVENT_ID:
                    Asserts.assertTrue(re.getDuration().isZero());
                    break;
            }
        }
    }

    private static void verifyNativeEvents() throws Exception {
        Recording r = new Recording();
        r.enable(EventNames.JVMInformation);
        r.enable(EventNames.ThreadSleep);
        r.start();
        // Should trigger a sleep event even if we
        // have a low resolution clock
        Thread.sleep(200);
        r.stop();
        List<RecordedEvent> recordedEvents = Events.fromRecording(r);
        Events.hasEvents(recordedEvents);
        for (RecordedEvent re : recordedEvents) {
            Asserts.assertEquals(re.getDuration(), Duration.between(re.getStartTime(), re.getEndTime()));
            switch (re.getEventType().getName()) {
                case EventNames.JVMInformation:
                    Asserts.assertTrue(re.getDuration().isZero());
                    break;
                case EventNames.ThreadSleep:
                    Asserts.assertTrue(!re.getDuration().isNegative() && !re.getDuration().isZero());
                    break;
                default:
                    Asserts.fail("Unexpected event: " + re);
                    break;
            }
        }
    }

}
