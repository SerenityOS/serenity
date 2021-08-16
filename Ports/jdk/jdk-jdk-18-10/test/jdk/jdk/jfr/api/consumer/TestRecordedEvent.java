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

import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedClassLoader;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Verifies the methods of the RecordedEvent
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.TestRecordedEvent
 */
public class TestRecordedEvent {

    static class MyClass {
    }

    static class TestEvent extends Event {
        @Description("MyField")
        Class<?> clzField = String.class;
        int intField;
        String stringField = "myString";
        Class<?> myClass = MyClass.class;
    }

    public static void main(String[] args) throws Throwable {
        try (Recording r = new Recording()) {
            r.start();
            TestEvent t = new TestEvent();
            t.commit();
            r.stop();

            List<RecordedEvent> events = Events.fromRecording(r);
            Events.hasEvents(events);
            Asserts.assertEquals(events.size(), 1);
            RecordedEvent event = events.get(0);

            List<ValueDescriptor> descriptors = event.getFields();

            System.out.println("Descriptors");
            for (ValueDescriptor descriptor : descriptors) {
                System.out.println(descriptor.getName());
                System.out.println(descriptor.getTypeName());
            }
            System.out.println("Descriptors end");

            Object recordedClass = event.getValue("clzField");
            Asserts.assertTrue(recordedClass instanceof RecordedClass, "Expected Recorded Class got " + recordedClass);

            Object recordedInt = event.getValue("intField");
            Asserts.assertTrue(recordedInt instanceof Integer);

            Object recordedString = event.getValue("stringField");
            System.out.println("recordedString class: " + recordedString.getClass());
            Asserts.assertTrue(recordedString instanceof String);

            Object myClass = event.getValue("myClass");
            Asserts.assertTrue(myClass instanceof RecordedClass, "Expected Recorded Class got " + recordedClass);

            RecordedClass myRecClass = (RecordedClass) myClass;
            Asserts.assertEquals(MyClass.class.getName(), myRecClass.getName(), "Got " + myRecClass.getName());

            Object recordedClassLoader = myRecClass.getValue("classLoader");
            Asserts.assertTrue(recordedClassLoader instanceof RecordedClassLoader, "Expected Recorded ClassLoader got " + recordedClassLoader);

            RecordedClassLoader myRecClassLoader = (RecordedClassLoader) recordedClassLoader;
            ClassLoader cl = MyClass.class.getClassLoader();
            Asserts.assertEquals(cl.getClass().getName(), myRecClassLoader.getType().getName(), "Expected Recorded ClassLoader type to equal loader type");

            Asserts.assertNotNull(myRecClass.getModifiers());
        }
    }
}
