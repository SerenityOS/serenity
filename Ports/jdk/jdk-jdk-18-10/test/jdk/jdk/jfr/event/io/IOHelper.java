/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.io;

import static jdk.test.lib.Asserts.assertEquals;
import static jdk.test.lib.Asserts.assertTrue;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jdk.jfr.event.io.IOEvent.EventType;

import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.Events;


// Helper class to match actual RecordedEvents to expected events.
public class IOHelper {

    public static void verifyEqualsInOrder(List<RecordedEvent> events, List<IOEvent> expectedEvents) throws Throwable {
        Collections.sort(events, Comparator.comparing(RecordedEvent::getStartTime));
        List<IOEvent> actualEvents = getTestEvents(events, expectedEvents);
        try {
            assertEquals(actualEvents.size(), expectedEvents.size(), "Wrong number of events.");
            for (int i = 0; i < actualEvents.size(); ++i) {
                assertEquals(actualEvents.get(i), expectedEvents.get(i), "Wrong event at pos " + i);
            }
        } catch (Throwable t) {
            for (RecordedEvent e: events) {
                System.out.println(e);
            }
            logEvents(actualEvents, expectedEvents);
            throw t;
        }
    }


    public static void verifyEquals(List<RecordedEvent> events, List<IOEvent> expectedEvents) throws Throwable {
        List<IOEvent> actualEvents = getTestEvents(events, expectedEvents);
        try {
            assertEquals(actualEvents.size(), expectedEvents.size(), "Wrong number of events");
            assertTrue(actualEvents.containsAll(expectedEvents), "Not all expected events received");
            assertTrue(expectedEvents.containsAll(actualEvents), "Received unexpected events");
        } catch (Throwable t) {
            logEvents(actualEvents, expectedEvents);
            throw t;
        }
    }


    private static List<IOEvent> getTestEvents(List<RecordedEvent> events, List<IOEvent> expectedEvents) throws Throwable {
        // Log all events
        for (RecordedEvent event : events) {
            String msg = event.getEventType().getName();
            boolean isSocket = IOEvent.EVENT_SOCKET_READ.equals(msg) || IOEvent.EVENT_SOCKET_WRITE.equals(msg);
            boolean isFile = IOEvent.EVENT_FILE_FORCE.equals(msg) || IOEvent.EVENT_FILE_READ.equals(msg) || IOEvent.EVENT_FILE_WRITE.equals(msg);
            boolean isFileReadOrWrite = IOEvent.EVENT_FILE_READ.equals(msg) || IOEvent.EVENT_FILE_WRITE.equals(msg);
            boolean isRead = IOEvent.EVENT_FILE_READ.equals(msg) || IOEvent.EVENT_SOCKET_READ.equals(msg);
            if (isFile) {
                msg += " : " + Events.assertField(event, "path").getValue();
            } else if (isSocket) {
                msg += " - " + Events.assertField(event, "host").getValue();
                msg += "." + Events.assertField(event, "address").getValue();
                msg += "." + Events.assertField(event, "port").getValue();
            }
            if (isSocket || isFileReadOrWrite) {
                String field = isRead ? "bytesRead" : "bytesWritten";
                msg += " : " + Events.assertField(event, field).getValue();
            }
            System.out.println(msg);
        }

        return events.stream()
                        .filter(event -> isTestEvent(event, expectedEvents))
                        .map(event -> IOEvent.createTestEvent(event))
                        .collect(Collectors.toList());
    }

    // A recording may contain extra events that are not part of the test.
    // This function filters out events that not belong to the test.
    public static boolean isTestEvent(RecordedEvent event, List<IOEvent> testEvents) {
        EventType eventType = IOEvent.getEventType(event);
        if (eventType == EventType.UnknownEvent) {
                return false;
        }

        // Only care about threads in the expected threads.
        final String threadName = event.getThread().getJavaName();
        if (testEvents.stream().noneMatch(te -> te.thread.equals(threadName))) {
                return false;
        }

        // Only care about files and sockets in expected events.
        final String address = IOEvent.getEventAddress(event);
        if (testEvents.stream().noneMatch(te -> te.address.equals(address))) {
                return false;
        }
        return true;
    }

    private static void logEvents(List<IOEvent> actualEvents, List<IOEvent> expectedEvents) {
        for (int i = 0; i < actualEvents.size(); ++i) {
            System.out.println("actual event[" + i + "] = " + actualEvents.get(i));
        }
        for (int i = 0; i < expectedEvents.size(); ++i) {
            System.out.println("expected event[" + i + "] = " + expectedEvents.get(i));
        }
    }

}
