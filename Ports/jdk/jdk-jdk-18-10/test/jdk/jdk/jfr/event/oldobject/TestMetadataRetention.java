/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.time.Instant;
import java.util.List;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedClassLoader;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.internal.test.WhiteBox;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.TestClassLoader;

/**
 * @test
 * @summary The test verifies that an old object sample maintains references to "stale" metadata
 * @requires vm.hasJFR
 * @key jfr
 * @modules jdk.jfr/jdk.jfr.internal.test
 * @library /test/lib /test/jdk
 * @build jdk.jfr.event.oldobject.TestMetadataObject
 * @run main/othervm -XX:TLABSize=2k -Xmx16m jdk.jfr.event.oldobject.TestMetadataRetention
 */
public final class TestMetadataRetention {
    private final static String TEST_PACKAGE = TestMetadataRetention.class.getPackage().getName();
    private final static String TEST_CLASS_LOADER_NAME = "JFR TestClassLoader";
    private final static String TEST_CLASS_NAME = TEST_PACKAGE + ".TestMetadataObject";
    private final static String ALLOCATOR_THREAD_NAME = "TestAllocationThread";

    public static ClassLoader testClassLoader;
    public static Object leakObject;
    public static Thread allocatorThread;
    public static Class<?> testClass;

    static class ChunkRotation extends Event {
    }

    public static void main(String[] args) throws Throwable {
        WhiteBox.setWriteAllObjectSamples(true);
        while (true) {
            int failedAttempts = 0;
            try (Recording recording = new Recording()) {
                recording.enable(EventNames.OldObjectSample).withStackTrace();
                recording.enable(EventNames.ClassUnload);
                recording.start();

                // Setup metadata on the Java heap (class, stack trace and thread)
                testClassLoader = new TestClassLoader();
                testClass = testClassLoader.loadClass(TEST_CLASS_NAME);
                allocatorThread = new Thread(TestMetadataRetention::allocateLeak, ALLOCATOR_THREAD_NAME);
                allocatorThread.start();
                allocatorThread.join();

                // Clear out metadata on the heap
                testClassLoader = null;
                testClass = null;
                allocatorThread = null;

                // System.gc() will trigger class unloading if -XX:+ExplicitGCInvokesConcurrent
                // is NOT set. If this flag is set G1 will never unload classes on System.gc().
                // As far as the "jfr" key guarantees no VM flags are set from the
                // outside it should be enough with System.gc().
                System.gc();

                // Provoke a chunk rotation, which will flush out ordinary metadata.
                provokeChunkRotation();
                ChunkRotation cr = new ChunkRotation();
                cr.commit();
                recording.stop();

                List<RecordedEvent> events = Events.fromRecording(recording);
                RecordedEvent chunkRotation = findChunkRotationEvent(events);
                try {
                    // Sanity check that class was unloaded
                    Events.hasEvent(recording, EventNames.ClassUnload);
                    validateClassUnloadEvent(events);
                    // Validate that metadata for old object event has survived chunk rotation
                    Events.hasEvent(recording, EventNames.OldObjectSample);
                    validateOldObjectEvent(events, chunkRotation.getStartTime());
                } catch (Throwable t) {
                    t.printStackTrace();
                    System.out.println("Number of failed attempts " + ++failedAttempts);
                    continue;
                }
                break;
            }
        }
    }

    private static RecordedEvent findChunkRotationEvent(List<RecordedEvent> events) {
        for (RecordedEvent e : events)  {
            if (e.getEventType().getName().equals(ChunkRotation.class.getName())) {
                return e;
            }
        }
        Asserts.fail("Could not find chunk rotation event");
        return null; // Can't happen;
    }

    private static void allocateLeak() {
        try {
            leakObject = testClass.getMethod("startRecurse").invoke(null);
        } catch (Exception e) {
            System.out.println("Could not allocate memory leak!");
            e.printStackTrace();
        }
    }

    private static void provokeChunkRotation() {
        try (Recording r = new Recording()) {
            r.start();
            r.stop();
        }
    }

    private static void validateClassUnloadEvent(List<RecordedEvent> events) throws Throwable {
        for (RecordedEvent event : events) {
            if (event.getEventType().getName().equals(EventNames.ClassUnload)) {
                RecordedClass unloadedClass = event.getValue("unloadedClass");
                if (TEST_CLASS_NAME.equals(unloadedClass.getName())) {
                    RecordedClassLoader definingClassLoader = unloadedClass.getClassLoader();
                    Asserts.assertEquals(TEST_CLASS_LOADER_NAME, definingClassLoader.getName(), "Expected " + TEST_CLASS_LOADER_NAME + ", got " + definingClassLoader.getType().getName());
                    return;
                }
            }
        }
    }

    private static void validateOldObjectEvent(List<RecordedEvent> events, Instant chunkRotation) throws Throwable {
        for (RecordedEvent event : events) {
            if (event.getEventType().getName().equals(EventNames.OldObjectSample)) {
                // Only check event after the rotation
                if (!event.getStartTime().isBefore(chunkRotation)) {
                    System.out.println(event);
                    RecordedThread rt = event.getThread();
                    if (rt.getJavaName().equals(ALLOCATOR_THREAD_NAME)) {
                        RecordedStackTrace s = event.getStackTrace();
                        assertStackTrace(s, "startRecurse");
                        assertStackTrace(s, "recurse");
                        return;
                    }
                }
            }
        }

        Asserts.fail("Did not find an old object event with thread " + ALLOCATOR_THREAD_NAME);
    }

    private static void assertStackTrace(RecordedStackTrace stacktrace, final String methodName) {
        for (RecordedFrame f : stacktrace.getFrames()) {
            if (f.getMethod().getName().equals(methodName)) {
                return;
            }
        }
        Asserts.fail("Could not find class " + methodName + " in stack trace " + stacktrace);
    }
}
