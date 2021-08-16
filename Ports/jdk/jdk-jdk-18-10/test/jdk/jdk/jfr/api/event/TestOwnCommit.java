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

import java.time.Instant;
import java.util.Iterator;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Use custom event that reuse method names begin, end and commit.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.event.TestOwnCommit
 */

public class TestOwnCommit {

    public static void main(String[] args) throws Throwable {
       Recording r = new Recording();
        r.enable(MyEvent.class);

        r.start();

        MyEvent event = new MyEvent();
        event.begin();
        event.begin = 10;
        event.duration = Instant.now();
        MyEvent.startTime = 20;
        event.shouldCommit = "shouldCommit";
        MyEvent.set = 30;
        event.end();
        event.commit();

        // Verify that our methods have not been removed by transformation.
        int id = 0;
        event.begin(++id);
        Asserts.assertEquals(id, staticTestValue, "EventWithBegin failed to set value");
        event.end(++id);
        Asserts.assertEquals(id, staticTestValue, "EventWithEnd failed to set value");
        event.commit(++id);
        Asserts.assertEquals(id, staticTestValue, "EventWithCommit failed to set value");
        event.shouldCommit(++id);
        Asserts.assertEquals(id, staticTestValue, "EventWithShouldCommit failed to set value");
        event.set(++id);
        Asserts.assertEquals(id, staticTestValue, "EventWithSet failed to set value");

        r.stop();

        Iterator<RecordedEvent> it = Events.fromRecording(r).iterator();
        Asserts.assertTrue(it.hasNext(), "No events");
        RecordedEvent recordedEvent = it.next();
        Asserts.assertTrue(Events.isEventType(recordedEvent, MyEvent.class.getName()));
        Events.assertField(recordedEvent, "begin").equal(10L);
        Events.assertField(recordedEvent, "shouldCommit").equal("shouldCommit");
        Events.assertField(recordedEvent, "startTime");
        Events.assertField(recordedEvent, "duration");
        Asserts.assertNull(recordedEvent.getEventType().getField("set")); // static not supported
        Asserts.assertFalse(it.hasNext(), "More than 1 event");

        r.close();
    }

    private static int staticTestValue;

    @SuppressWarnings("unused")
    static class MyEvent extends Event {
        public long begin;
        private Instant duration;
        private static int startTime;
        protected String shouldCommit;
        public static int set;

        public void begin(int testValue) {
            staticTestValue = testValue;
        }

        public void end(int testValue) {
            staticTestValue = testValue;
        }

        public void commit(int testValue) {
            staticTestValue = testValue;
        }

        public void shouldCommit(int testValue) {
            staticTestValue = testValue;
        }

        public void set(int testValue) {
            staticTestValue = testValue;
        }
    }

}
