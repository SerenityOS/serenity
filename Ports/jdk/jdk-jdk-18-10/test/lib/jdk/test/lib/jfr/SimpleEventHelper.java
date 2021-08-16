/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.test.lib.jfr;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;

public class SimpleEventHelper {

    public static void enable(Recording r, boolean isEnabled) {
        if (isEnabled) {
            r.enable(SimpleEvent.class).withThreshold(Duration.ofMillis(0)).withoutStackTrace();
        } else {
            r.disable(SimpleEvent.class);
        }
    }

    public static SimpleEvent createEvent(int id) {
        SimpleEvent event = new SimpleEvent();
        event.begin();
        event.id = id;
        event.end();
        event.commit();
        return event;
    }

    public static void verifyEvents(Recording r, int ... ids) throws Exception {
        List<Integer> eventIds = new ArrayList<>();
        for (RecordedEvent event : Events.fromRecording(r)) {
            if (Events.isEventType(event, SimpleEvent.class.getName())) {
                int id = Events.assertField(event, "id").getValue();
                System.out.printf("recording %s: event.id=%d%n", r.getName(), id);
                eventIds.add(id);
            }
        }
        Asserts.assertEquals(eventIds.size(), ids.length, "Wrong number of events");
        for (int i = 0; i < ids.length; ++i) {
            Asserts.assertEquals(eventIds.get(i).intValue(), ids[i], "Wrong id in event");
        }
    }

    public static void verifyContains(List<RecordedEvent> events, int ... ids) throws Exception {
        Set<Integer> missingIds = new HashSet<>();
        for (int id : ids) {
            missingIds.add(id);
        }
        for (RecordedEvent event : getSimpleEvents(events)) {
            int id = Events.assertField(event, "id").getValue();
            System.out.printf("event.id=%d%n", id);
            missingIds.remove(Integer.valueOf(id));
        }
        if (!missingIds.isEmpty()) {
            missingIds.forEach(id -> System.out.println("Missing MyEvent with id " + id));
            Asserts.fail("Missing some MyEvent events");
        }
    }

    public static void verifyNotContains(List<RecordedEvent> events, int ... ids) throws Exception {
        for (RecordedEvent event : getSimpleEvents(events)) {
            int eventId = Events.assertField(event, "id").getValue();
            System.out.printf("event.id=%d%n", eventId);
            for (int id : ids) {
                Events.assertField(event, "id").notEqual(id);
            }
        }
    }

    public static List<RecordedEvent> getSimpleEvents(List<RecordedEvent> events) {
        List<RecordedEvent> myEvents = new ArrayList<>();
        for (RecordedEvent event : events) {
            if (Events.isEventType(event, SimpleEvent.class.getName())) {
                myEvents.add(event);
            }
        }
        return myEvents;
    }
}
