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

package jdk.jfr.api.consumer;

import java.util.HashMap;
import java.util.List;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Sanity checks that RecordedEvent#toString returns something valid
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.TestToString
 */
public class TestToString {

    public static void main(String[] args) throws Throwable {

        try (Recording recording = new Recording()) {
            recording.start();
            HelloWorldEvent event = new HelloWorldEvent();
            event.message = "hello, world";
            event.integer = 4711;
            event.floatValue = 9898;
            event.doubleValue = 313;
            event.clazz = HashMap.class;
            event.characater = '&';
            event.byteValue = (byte) 123;
            event.longValue = 1234567890L;
            event.shortValue = 64;
            event.booleanValue = false;
            event.commit();
            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            Events.hasEvents(events);
            RecordedEvent e = events.get(0);
            String toString = e.toString();
            System.out.println(toString);
            Asserts.assertTrue(toString.contains("hello, world"), "Missing String field value in RecordedEvent#toSting()");
            Asserts.assertTrue(toString.contains("4711"), "Missing integer fields value in RecordedEvent#toSting()");
            Asserts.assertTrue(toString.contains("313"), "Missing double value in RecordedEvent#toSting()");
            Asserts.assertTrue(toString.contains("HashMap"), "Missing class value in RecordedEvent#toSting()");
            Asserts.assertTrue(toString.contains("1234567890"), "Missing long value in RecordedEvent#toSting()");
            Asserts.assertTrue(toString.contains("&"), "Missing char value in RecordedEvent#toSting()");
            Asserts.assertTrue(toString.contains("123"), "Missing byte value in RecordedEvent#toString()");
            Asserts.assertTrue(toString.contains("64"), "Missing short value in RecordedEvent#toString()");
            Asserts.assertTrue(toString.contains("false"), "Missing boolean value in RecordedEvent#toString()");
            Asserts.assertTrue(toString.contains("HelloWorldEvent"), "Missing class name in RecordedEvent#toString()");
        }
    }

    static class HelloWorldEvent extends Event {
        public boolean booleanValue;
        public long longValue;
        public int shortValue;
        public byte byteValue;
        public int doubleValue;
        public char characater;
        public Thread mainThread;
        public Class<?> clazz;
        public int floatValue;
        public int integer;
        public String message;
    }
}
