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

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Load event class after recording started.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @build jdk.test.lib.jfr.SimpleEvent
 * @run main/othervm jdk.jfr.api.recording.event.TestLoadEventAfterStart
 */
public class TestLoadEventAfterStart {

    public static void main(String[] args) throws Throwable {
        Recording r = new Recording();
        r.start();

        ClassLoader classLoader = TestLoadEventAfterStart.class.getClassLoader();
        Class<? extends Event> eventClass =
            classLoader.loadClass("jdk.test.lib.jfr.SimpleEvent").asSubclass(Event.class);

        r.enable(eventClass).withThreshold(Duration.ofMillis(0)).withoutStackTrace();
        createEvent(eventClass, 1);
        r.disable(eventClass);
        createEvent(eventClass, 2);
        r.enable(eventClass).withThreshold(Duration.ofMillis(0)).withoutStackTrace();
        createEvent(eventClass, 3);

        r.stop();
        verifyEvents(r, 1, 3);
    }

    private static void verifyEvents(Recording r, int ... ids) throws Exception {
        List<Integer> eventIds = new ArrayList<>();
        for (RecordedEvent event : Events.fromRecording(r)) {
            int id = Events.assertField(event, "id").getValue();
            System.out.println("Event id:" + id);
            eventIds.add(id);
        }
        Asserts.assertEquals(eventIds.size(), ids.length, "Wrong number of events");
        for (int i = 0; i < ids.length; ++i) {
            Asserts.assertEquals(eventIds.get(i).intValue(), ids[i], "Wrong id in event");
        }
    }

    private static void createEvent(Class<? extends Event> eventClass, int id) throws Exception {
        Constructor<? extends Event> constructor = eventClass.getConstructor();
        Event event = (Event) constructor.newInstance();
        event.begin();
        eventClass.getDeclaredField("id").setInt(event, id);
        event.end();
        event.commit();
    }
}
