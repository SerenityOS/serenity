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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.CommonHelper;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Test getStartTime() and getEndTime(). Verify startTime <= endTime
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.event.TestEventTime
 */
public class TestEventTime {

    static List<TimeEvent> actualOrder = new ArrayList<>();

    public static void main(String[] args) throws Throwable {
        Recording r = new Recording();
        r.enable(MyEvent.class).withoutStackTrace();
        // for debugging time related issues
        r.enable(EventNames.CPUTimeStampCounter);
        r.start();
        MyEvent event1 = beginEvent(1);
        MyEvent event2 = beginEvent(2);
        endEvent(event1);
        MyEvent event3 = beginEvent(3);
        endEvent(event2);
        endEvent(event3);

        r.stop();

        List<RecordedEvent> events = Events.fromRecording(r);

        RecordedEvent recEvent1 = findEvent(1, events);
        RecordedEvent recEvent2 = findEvent(2, events);
        RecordedEvent recEvent3 = findEvent(3, events);

        List<TimeEvent> recordedOrder = new ArrayList<>();
        recordedOrder.add(new TimeEvent(1, true, recEvent1.getStartTime()));
        recordedOrder.add(new TimeEvent(1, false, recEvent1.getEndTime()));
        recordedOrder.add(new TimeEvent(2, true, recEvent2.getStartTime()));
        recordedOrder.add(new TimeEvent(2, false, recEvent2.getEndTime()));
        recordedOrder.add(new TimeEvent(3, true, recEvent3.getStartTime()));
        recordedOrder.add(new TimeEvent(3, false, recEvent3.getEndTime()));
        Collections.sort(recordedOrder);

        printTimedEvents("Actual order", actualOrder);
        printTimedEvents("Recorded order", recordedOrder);

        for (int i = 0; i < 6; i++) {
            if (!actualOrder.get(i).equals(recordedOrder.get(i))) {
                throw new Exception("Event times not in expected order. Was " + recordedOrder.get(1) + " but expected " + actualOrder.get(1));
            }
        }
    }

    private static void printTimedEvents(String heading, List<TimeEvent> recordedOrder) {
        System.out.println();
        System.out.println(heading);
        System.out.println("======================");
        for (TimeEvent t : recordedOrder) {
            System.out.println(t.toString());
        }
    }

    private static MyEvent beginEvent(int id) throws Exception {
        MyEvent event = new MyEvent(id);
        event.begin();
        if (!CommonHelper.hasFastTimeEnabled()) {
            CommonHelper.waitForSystemCurrentMillisToChange();;
        }
        actualOrder.add(new TimeEvent(id, true));
        return event;
    }

    private static void endEvent(MyEvent event) throws Exception {
        event.end();
        if (!CommonHelper.hasFastTimeEnabled()) {
            CommonHelper.waitForSystemCurrentMillisToChange();;
        }
        event.commit();
        actualOrder.add(new TimeEvent(event.id, false));
    }

    private final static class TimeEvent implements Comparable<TimeEvent> {
        long id;
        private boolean begin;
        private Instant time;

        public TimeEvent(int id, boolean begin) {
            this.id = id;
            this.begin = begin;
        }

        public TimeEvent(int id, boolean begin, Instant time) {
            this(id, begin);
            this.time = time;
        }

        @Override
        public int compareTo(TimeEvent that) {
            return this.time.compareTo(that.time);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (begin) {
                sb.append("begin");
            } else {
                sb.append("end");
            }
            sb.append("Event");
            sb.append("(");
            sb.append(id);
            sb.append(")");
            return sb.toString();
        }

        public boolean equals(Object thatObject) {
            if (thatObject instanceof TimeEvent) {
                TimeEvent that = (TimeEvent) thatObject;
                return that.id == this.id && that.begin == this.begin;
            }
            return false;
        }
    }

    private static RecordedEvent findEvent(int id, List<RecordedEvent> events) {
        for (RecordedEvent event : events) {
            if (!event.getEventType().getName().equals(EventNames.CPUTimeStampCounter)) {
                int eventId = Events.assertField(event, "id").getValue();
                if (eventId == id) {
                    return event;
                }
            }
        }
        Asserts.fail("No event with id " + id);
        return null;
    }

    private static class MyEvent extends Event {
        int id;

        public MyEvent(int id) {
            this.id = id;
        }
    }
}
