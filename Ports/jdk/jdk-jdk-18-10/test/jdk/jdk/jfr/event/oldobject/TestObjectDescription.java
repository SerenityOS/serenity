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

package jdk.jfr.event.oldobject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.internal.test.WhiteBox;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @requires vm.gc != "Z"
 * @library /test/lib /test/jdk
 * @modules jdk.jfr/jdk.jfr.internal.test
 * @run main/othervm -XX:TLABSize=2k jdk.jfr.event.oldobject.TestObjectDescription
 */
public class TestObjectDescription {

    private static final int OBJECT_DESCRIPTION_MAX_SIZE = 100;
    private static final String CLASS_NAME = TestClassLoader.class.getName() + "$TestClass";
    public static List<?> leaks;

    public final static class MyThreadGroup extends ThreadGroup {
        public final static String NAME = "My Thread Group";

        public MyThreadGroup(String name) {
            super(name);
        }

        // Allocate array to trigger sampling code path for interpreter / c1
        byte[] bytes = new byte[10];
    }

    public final static class MyThread extends Thread {
        public final static String NAME = "My Thread";

        public MyThread() {
            super(NAME);
        }

        // Allocate array to trigger sampling code path for interpreter / c1
        byte[] bytes = new byte[10];
    }

    public static void main(String[] args) throws Exception {
        WhiteBox.setWriteAllObjectSamples(true);

        testThreadGroupName();
        testThreadName();
        testClassName();
        testSize();
        testEllipsis();
    }

    private static void testThreadName() throws Exception {
        asseertObjectDescription(() -> {
            List<MyThread> threads = new ArrayList<>(OldObjects.MIN_SIZE);
            for (int i = 0; i < OldObjects.MIN_SIZE; i++) {
                threads.add(new MyThread());
            }
            return threads;
        }, "Thread Name: " + MyThread.NAME);
    }

    private static void testThreadGroupName() throws Exception {
        asseertObjectDescription(() -> {
            List<MyThreadGroup> groups = new ArrayList<>(OldObjects.MIN_SIZE);
            for (int i = 0; i < OldObjects.MIN_SIZE; i++) {
                groups.add(new MyThreadGroup("My Thread Group"));
            }
            return groups;
        }, "Thread Group: " + "My Thread Group");
    }

    private static void testClassName() throws Exception {
        asseertObjectDescription(() -> {
            TestClassLoader testClassLoader = new TestClassLoader();
            List<Object> classObjects = new ArrayList<>(OldObjects.MIN_SIZE);
            for (Class<?> clazz : testClassLoader.loadClasses(OldObjects.MIN_SIZE / 20)) {
                // Allocate array to trigger sampling code path for interpreter / c1
                for (int i = 0; i < 20; i++) {
                    Object classArray = Array.newInstance(clazz, 20);
                    Array.set(classArray, i, clazz.newInstance());
                    classObjects.add(classArray);
                }
            }
            return classObjects;
        }, "Class Name: " + CLASS_NAME);
    }

    private static void testSize() throws Exception {
        asseertObjectDescription(() -> {
            List<Object> arrayLists = new ArrayList<>(OldObjects.MIN_SIZE);
            for (int i = 0; i < OldObjects.MIN_SIZE; i++) {
                List<Object> arrayList = new ArrayList<>();
                arrayList.add(new Object());
                arrayList.add(new Object());
                arrayList.add(new Object());
                arrayLists.add(arrayList);
            }
            return arrayLists;
        }, "Size: 3");
    }

    private static void testEllipsis() throws Exception {
        asseertObjectDescription(() -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 2 * OBJECT_DESCRIPTION_MAX_SIZE; i++) {
                sb.append("x");
            }
            String threadName = sb.toString();
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < OldObjects.MIN_SIZE; i++) {
                threads.add(new Thread(threadName));
            }
            return threads;
        }, "xxx...");
    }

    private static void asseertObjectDescription(Callable<List<?>> callable, String text) throws Exception {
        int iteration = 1;
        while (true) {
            try (Recording recording = new Recording()) {
                System.out.println("Iteration: " + iteration);
                recording.enable(EventNames.OldObjectSample).withoutStackTrace().with("cutoff", "infinity");
                recording.start();
                leaks = null;
                System.gc();
                leaks = callable.call();

                recording.stop();

                List<RecordedEvent> events = Events.fromRecording(recording);
                Set<String> objectDescriptions = extractObjectDecriptions(events);
                for (String s : objectDescriptions) {
                    if (s.contains(text)) {
                        printDescriptions(objectDescriptions);
                        return;
                    }
                }
                System.out.println("Could not find object description containing text \"" + text + "\"");
                printDescriptions(objectDescriptions);
                System.out.println();
                iteration++;
            }
        }
    }

    private static void printDescriptions(Set<String> objectDescriptions) {
        System.out.println("Found descriptions:");
        for (String t : objectDescriptions) {
            System.out.println(t);
        }
    }

    private static Set<String> extractObjectDecriptions(List<RecordedEvent> events) {
        Set<String> objectDescriptions = new HashSet<>();
        for (RecordedEvent e : events) {
            objectDescriptions.addAll(extractObjectDescriptions(e.getValue("object")));
        }
        return objectDescriptions;
    }

    private static Set<String> extractObjectDescriptions(RecordedObject o) {
        Set<Long> visited = new HashSet<>();
        Set<String> descriptions = new HashSet<>();
        while (o != null) {
            Long memoryAddress = o.getValue("address");
            if (visited.contains(memoryAddress)) {
                return descriptions;
            }
            visited.add(memoryAddress);
            String od = o.getValue("description");
            if (od != null) {
                descriptions.add(od);
            }
            RecordedObject referrer = o.getValue("referrer");
            o = referrer != null ? referrer.getValue("object") : null;
        }
        return descriptions;
    }
}
