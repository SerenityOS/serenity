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

import java.util.List;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.TestFieldAccess
 */
public class TestFieldAccess {

    private static class MyEvent extends Event {
        byte byteField = 42;
        char charField = 'X';
        String stringField = "Hello";
        int intField = 4711;
        long longField = 4712L;
        short shortField = (short)67;
        double doubleField = Double.NaN;
        float floatField = Float.MIN_VALUE;
        boolean booleanField = false;
        Thread threadField = Thread.currentThread();
        Class<?> classField = MyEvent.class;
    }

    public static void main(String[] args) throws Throwable {
        try (Recording r = new Recording()) {
            r.enable(MyEvent.class);
            r.start();
            MyEvent myEvent = new MyEvent();
            myEvent.commit();
            r.stop();
            List<RecordedEvent> events = Events.fromRecording(r);
            Events.hasEvents(events);
            RecordedEvent event = events.get(0);
            testHasField(event);
            testGetField(event, myEvent);
        }
    }

    private static void testGetField(RecordedEvent event, MyEvent myEvent) {
        char charField = event.getValue("charField");
        Asserts.assertEquals(charField, myEvent.charField);

        byte byteField = event.getValue("byteField");
        Asserts.assertEquals(byteField, myEvent.byteField);

        String stringField = event.getValue("stringField");
        Asserts.assertEquals(stringField, myEvent.stringField);

        int intField = event.getValue("intField");
        Asserts.assertEquals(intField, myEvent.intField);

        long longField = event.getValue("longField");
        Asserts.assertEquals(longField, myEvent.longField);

        short shortField = event.getValue("shortField");
        Asserts.assertEquals(shortField, myEvent.shortField);

        double doubleField = event.getValue("doubleField");
        Asserts.assertEquals(doubleField, myEvent.doubleField);

        float floatField = event.getValue("floatField");
        Asserts.assertEquals(floatField, myEvent.floatField);

        boolean booleanField = event.getValue("booleanField");
        Asserts.assertEquals(booleanField, myEvent.booleanField);

        RecordedThread threadField = event.getValue("eventThread");
        Asserts.assertEquals(threadField.getJavaName(), myEvent.threadField.getName());
        String threadGroupName = event.getValue("eventThread.group.name");
        Asserts.assertEquals(threadField.getThreadGroup().getName(), threadGroupName);

        RecordedClass  classField = event.getValue("classField");
        Asserts.assertEquals(classField.getName(), myEvent.classField.getName());
        String className = event.getValue("classField.name");
        Asserts.assertEquals(classField.getName(), className.replace("/", "."));

        try {
            event.getValue("doesnotexist");
        } catch (IllegalArgumentException iae) {
            // as expected
        }

        try {
            event.getValue("classField.doesnotexist");
        } catch (IllegalArgumentException iae) {
            // as expected
        }

        try {
            event.getValue(null);
        } catch (NullPointerException npe) {
            // as expected
        }
    }

    private static void testHasField(RecordedEvent event) {
        System.out.println(event);
        Asserts.assertTrue(event.hasField("charField"));
        Asserts.assertTrue(event.hasField("byteField"));
        Asserts.assertTrue(event.hasField("stringField"));
        Asserts.assertTrue(event.hasField("intField"));
        Asserts.assertTrue(event.hasField("longField"));
        Asserts.assertTrue(event.hasField("shortField"));
        Asserts.assertTrue(event.hasField("doubleField"));
        Asserts.assertTrue(event.hasField("floatField"));
        Asserts.assertTrue(event.hasField("threadField"));
        Asserts.assertTrue(event.hasField("classField"));
        Asserts.assertTrue(event.hasField("classField.name"));
        Asserts.assertTrue(event.hasField("eventThread"));
        Asserts.assertTrue(event.hasField("eventThread.group.name"));
        Asserts.assertTrue(event.hasField("startTime"));
        Asserts.assertTrue(event.hasField("stackTrace"));
        Asserts.assertTrue(event.hasField("duration"));
        Asserts.assertFalse(event.hasField("doesNotExist"));
        Asserts.assertFalse(event.hasField("classField.doesNotExist"));
        Asserts.assertFalse(event.hasField(""));
        try {
            event.hasField(null);
        } catch (NullPointerException npe) {
            // as expected
        }
    }
}
