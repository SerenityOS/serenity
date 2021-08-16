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

package jdk.jfr.api.event;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Test enable/disable event and verify recording has expected events.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.event.TestEnableDisable
 */

public class TestEnableDisable {

    public static void main(String[] args) throws Exception {
        List<MyEvent> expectedEvents = new ArrayList<>();
        Recording r = new Recording();

        r.start();
        createEvent(expectedEvents, true); // id=0 Custom event classes are enabled by default.

        r.disable(MyEvent.class);
        createEvent(expectedEvents, false); // id=1
        r.enable(MyEvent.class);
        createEvent(expectedEvents, true);  // id=2

        // enable/disable by event setting name
        String eventSettingName = String.valueOf(EventType.getEventType(MyEvent.class).getId());
        System.out.println("eventSettingName=" + eventSettingName);

        r.disable(eventSettingName);
        createEvent(expectedEvents, false); // id=3
        r.enable(eventSettingName);
        createEvent(expectedEvents, true);

        r.stop();
        createEvent(expectedEvents, false);

        Iterator<MyEvent> expectedIterator = expectedEvents.iterator();
        for (RecordedEvent event : Events.fromRecording(r)) {
            System.out.println("event.id=" + Events.assertField(event, "id").getValue());
            Asserts.assertTrue(expectedIterator.hasNext(), "Found more events than expected");
            Events.assertField(event, "id").equal(expectedIterator.next().id);
        }
        Asserts.assertFalse(expectedIterator.hasNext(), "Did not find all expected events.");

        r.close();
    }

    private static int eventId = 0;
    private static void createEvent(List<MyEvent> expectedEvents, boolean isExpected) {
        MyEvent event = new MyEvent();
        event.begin();
        event.id = eventId;
        event.commit();

        if (isExpected) {
            expectedEvents.add(event);
        }
        eventId++;
    }

    private static class MyEvent extends Event {
        private int id;
    }

}
