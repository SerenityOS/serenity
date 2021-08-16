/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.runtime;

import java.time.Duration;
import java.util.*;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @bug 8242263
 * @requires vm.hasJFR
 * @key jfr
 * @library /test/lib
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:DiagnoseSyncOnValueBasedClasses=2 jdk.jfr.event.runtime.TestSyncOnValueBasedClassEvent
 */
public class TestSyncOnValueBasedClassEvent {
    static final String EVENT_NAME = EventNames.SyncOnValueBasedClass;
    static String[] classesWanted = {"java/lang/Character", "java/lang/Boolean", "java/lang/Byte", "java/lang/Short",
                                     "java/lang/Integer", "java/lang/Long", "java/lang/Float", "java/lang/Double",
                                     "java/time/Duration", "java/util/OptionalInt", "java/lang/Runtime$Version"};
    static List<Object> testObjects = new ArrayList<Object>();
    static Integer counter = 0;

    private static void initTestObjects() {
        testObjects.add(Character.valueOf('H'));
        testObjects.add(Boolean.valueOf(true));
        testObjects.add(Byte.valueOf((byte)0x40));
        testObjects.add(Short.valueOf((short)0x4000));
        testObjects.add(Integer.valueOf(0x40000000));
        testObjects.add(Long.valueOf(0x4000000000000000L));
        testObjects.add(Float.valueOf(1.20f));
        testObjects.add(Double.valueOf(1.2345));
        testObjects.add(Duration.ofMillis(5));
        testObjects.add(OptionalInt.of(10));
        testObjects.add(Runtime.version());
    }

    public static void main(String[] args) throws Throwable {
        initTestObjects();
        Recording recording = new Recording();
        recording.enable(EVENT_NAME).withThreshold(Duration.ofMillis(0));
        recording.start();
        for (Object obj : testObjects) {
            synchronized (obj) {
                counter++;
            }
        }
        recording.stop();

        List<String> classesFound = new ArrayList<String>();
        List<RecordedEvent> events = Events.fromRecording(recording);
        Events.hasEvents(events);
        for (RecordedEvent event : Events.fromRecording(recording)) {
            String className = Events.assertField(event, "valueBasedClass.name").notEmpty().getValue();
            RecordedThread jt = event.getThread();
            if (Thread.currentThread().getName().equals(jt.getJavaName())) {
                classesFound.add(className);
            }
        }
        for (String classWanted : classesWanted) {
            if (!classesFound.contains(classWanted)) {
                throw new AssertionError("No matching event SyncOnValueBasedClass with \"valueBasedClass=" + classWanted + "\" and current thread as caller");
            }
        }
        if (classesFound.size() != classesWanted.length) {
            throw new AssertionError("Invalid number of SyncOnValueBasedClass events for current thread");
        }
    }
}
