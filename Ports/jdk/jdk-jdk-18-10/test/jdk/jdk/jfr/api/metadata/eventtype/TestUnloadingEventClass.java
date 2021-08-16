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

package jdk.jfr.api.metadata.eventtype;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingFile;
import jdk.jfr.internal.JVM;
import jdk.test.lib.Utils;

/**
 * @test
 * @key jfr
 * @summary Test that verifies event metadata is removed when an event class is unloaded.
 * @requires vm.hasJFR
 *
 * @library /test/lib
 * @modules jdk.jfr/jdk.jfr.internal
 *          java.base/jdk.internal.misc
 *
 * @run main/othervm -Xlog:class+unload -Xlog:gc jdk.jfr.api.metadata.eventtype.TestUnloadingEventClass
 */
public class TestUnloadingEventClass {

    private static final String EVENT_NAME = "jdk.jfr.api.metadata.eventtype.TestUnloadingEventClass$ToBeUnloaded";

    public static class ToBeUnloaded extends Event {
    }

    static public class MyClassLoader extends ClassLoader {
        public MyClassLoader() {
            super("MyClassLoader", null);
        }

        public final Class<?> defineClass(String name, byte[] b) {
            return super.defineClass(name, b, 0, b.length);
        }
    }

    private static final JVM jvm = JVM.getJVM();
    public static ClassLoader myClassLoader;

    public static void main(String[] args) throws Throwable {
        assertEventTypeNotAvailable();
        myClassLoader = createClassLoaderWithEventClass();

        try (Recording r0 = new Recording()) {
            r0.start();
            r0.stop();
            if (getEventType(r0, 0, EVENT_NAME) == null) {
                throw new Exception("Expected event class to have corresponding event type");
            }
        }

        try (Recording r1 = new Recording(); Recording r2 = new Recording(); Recording r3 = new Recording()) {
            r1.start();
            r2.start();
            System.out.println("Class loader with name " + myClassLoader.getName() + " is on the heap");
            unLoadEventClass();
            r3.start();

            assertEventTypeNotAvailable();
            r3.stop();
            r2.stop();
            r1.stop();

            if (getEventType(r1, 1, EVENT_NAME) == null) {
                throw new Exception("Expected event class to have corresponding event type in recording with all chunks");
            }
            if (getEventType(r2, 2, EVENT_NAME) == null) {
                throw new Exception("Expected event class to have corresponding event type in recording where event class was unloaded");
            }
            if (getEventType(r3, 3, EVENT_NAME) != null) {
                throw new Exception("Unexpected metadata found for event class tha has been unloaded.");
            }
        }
    }

    private static MyClassLoader createClassLoaderWithEventClass() throws Exception {
        String resourceName = EVENT_NAME.replace('.', '/') + ".class";
        try (InputStream is = TestUnloadingEventClass.class.getClassLoader().getResourceAsStream(resourceName)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int byteValue = 0;
            while ((byteValue = is.read(buffer, 0, buffer.length)) != -1) {
                baos.write(buffer, 0, byteValue);
            }
            baos.flush();
            MyClassLoader myClassLoader = new MyClassLoader();
            Class<?> eventClass = myClassLoader.defineClass(EVENT_NAME, baos.toByteArray());
            if (eventClass == null) {
                throw new Exception("Could not define test class");
            }
            if (eventClass.getSuperclass() != Event.class) {
                throw new Exception("Superclass should be jdk.jfr.Event");
            }
            if (eventClass.getSuperclass().getClassLoader() != null) {
                throw new Exception("Class loader of jdk.jfr.Event should be null");
            }
            if (eventClass.getClassLoader() != myClassLoader) {
                throw new Exception("Incorrect class loader for event class");
            }
            eventClass.newInstance(); // force <clinit>
            return myClassLoader;
        }
    }

    private static void unLoadEventClass() throws Exception {
        long firstCount = jvm.getUnloadedEventClassCount();
        System.out.println("Initial unloaded count: " + firstCount);
        myClassLoader = null;
        System.out.println("Cleared reference to MyClassLoader");
        long newCount = 0;
        do {
            System.out.println("GC triggered");
            System.gc();
            Thread.sleep(1000);
            newCount = jvm.getUnloadedEventClassCount();
            System.out.println("Unloaded count: " + newCount);
        } while (firstCount + 1 != newCount);
        System.out.println("Event class unloaded!");
        System.out.println("Event classes currently on the heap:");
        for (Class<?> eventClass : JVM.getJVM().getAllEventClasses()) {
            System.out.println(eventClass + " " + (eventClass.getClassLoader() != null ? eventClass.getClassLoader().getName() : null));
        }

    }

    private static void assertEventTypeNotAvailable() throws Exception {
        for (EventType type : FlightRecorder.getFlightRecorder().getEventTypes()) {
            if (type.getName().equals(EVENT_NAME)) {
                throw new Exception("Event type should not be available");
            }
        }
    }

    private static Object getEventType(Recording r, long id, String eventName) throws IOException {
        Path p = Utils.createTempFile("unloading-event-class-recording-" + id + "_", ".jfr");
        r.dump(p);
        try (RecordingFile rf = new RecordingFile(p)) {
            for (EventType et : rf.readEventTypes()) {
                if (et.getName().equals(eventName)) {
                    return et;
                }
            }

        }
        return null;
    }
}

