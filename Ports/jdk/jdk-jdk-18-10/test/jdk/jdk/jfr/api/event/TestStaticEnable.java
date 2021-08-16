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

import java.time.Duration;
import java.util.List;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Enable an event from a static function in the event.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.event.TestStaticEnable
 */
public class TestStaticEnable {

    public static void main(String[] args) throws Exception {
        Recording r = new Recording();
        MyEvent.enable(r, true);
        r.start();
        MyEvent.create("Hello", 1);
        r.stop();

        List<RecordedEvent> events = Events.fromRecording(r);
        Events.hasEvents(events);
        for (RecordedEvent event : events) {
            System.out.println("Event:" + event);
            Events.assertField(event, "msg").equal("Hello");
            Events.assertField(event, "id").equal(1L);
        }
        r.close();
    }


    public static class MyEvent extends Event {
        public String msg;
        long id;

        public static void enable(Recording r, boolean isEnabled) {
            if (isEnabled) {
                r.enable(MyEvent.class).withThreshold(Duration.ofMillis(0)).withoutStackTrace();
            } else {
                r.disable(MyEvent.class);
            }
        }

        public static void create(String msg, long id) {
            MyEvent event = new MyEvent();
            event.msg = msg;
            event.id = id;
            event.begin();
            event.end();
            event.commit();
        }
    }

}
