/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package jdk.test.lib.jfr;

import static jdk.test.lib.Asserts.assertEquals;
import static jdk.test.lib.Asserts.assertFalse;
import static jdk.test.lib.Asserts.assertNotNull;
import static jdk.test.lib.Asserts.assertTrue;
import static jdk.test.lib.Asserts.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import jdk.jfr.AnnotationElement;
import jdk.jfr.EventType;
import jdk.jfr.Recording;
import jdk.jfr.SettingDescriptor;
import jdk.jfr.Timespan;
import jdk.jfr.Timestamp;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordedThreadGroup;


/**
 * Helper class to verify RecordedEvent content
 */
public class Events {

    public static EventField assertField(RecordedEvent event, String name)  {
        String[] partNames = name.split("\\.");
        RecordedObject struct = event;
        try {
            for (int i=0; i<partNames.length; ++i) {
                final String partName =  partNames[i];
                final boolean isLastPart = i == partNames.length - 1;
                ValueDescriptor d = getValueDescriptor(struct, partName);
                if (isLastPart) {
                    return new EventField(struct, d);
                } else {
                    assertTrue(struct.getValue(partName) instanceof RecordedObject, "Expected '" + partName + "' to be a struct");
                    struct = struct.getValue(partName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.printf("Failed event:%n%s%n", event.toString());
        fail(String.format("Field %s not in event", name));
        return null;
    }

    private static RecordedObject getRecordedPackage(final RecordedClass rc) {
        if (rc == null) {
            throw new RuntimeException("RecordedClass must not be null!");
        }
        return rc.getValue("package");
    }

    private static RecordedObject getRecordedModule(final RecordedObject pkg) {
        if (pkg == null) {
            // null package is an unnamed module (null)
            return null;
        }

        return pkg.getValue("module");
    }
    /**
     * Validates the recored name field
     *
     * @param ro should be a Package or a Module
     * @param targetName name to match
     */
    private static boolean isMatchingTargetName(final RecordedObject ro, final String targetName) {
        if (ro == null) {
            return targetName == null;
        }

        final String recordedName = ro.getValue("name");

        if (recordedName == null) {
            return targetName == null;
        }

        return recordedName.equals(targetName);
    }

    public static void assertClassPackage(final RecordedClass rc, final String packageNameTarget) {
        final RecordedObject recordedPackage = getRecordedPackage(rc);

        if (recordedPackage == null) {
            if (packageNameTarget != null) {
                throw new RuntimeException("RecordedClass package is null!");
            }
            return;
        }
        assertTrue(isMatchingTargetName(recordedPackage, packageNameTarget), "mismatched package name! Target is " + packageNameTarget);
    }

    public static void assertClassModule(final RecordedClass rc, final String moduleNameTarget) {
        final RecordedObject recordedPackage = getRecordedPackage(rc);
        final RecordedObject recordedModule = getRecordedModule(recordedPackage);

        if (recordedModule == null) {
            if (moduleNameTarget != null) {
                throw new RuntimeException("RecordedClass module is null!");
            }
            return;
        }

       assertTrue(isMatchingTargetName(recordedModule, moduleNameTarget), "mismatched module name! Target is " + moduleNameTarget);
    }

    private static ValueDescriptor getValueDescriptor(RecordedObject struct, String name) throws Exception {
        List<ValueDescriptor> valueDescriptors = struct.getFields();
        for (ValueDescriptor d : valueDescriptors) {
            if (name.equals(d.getName())) {
                return d;
            }
        }
        System.out.printf("Failed struct:%s", struct.toString());
        fail(String.format("Field %s not in struct", name));
        return null;
    }

    public static void hasEvents(List<RecordedEvent> events) {
        assertFalse(events.isEmpty(), "No events");
    }

    public static void hasEvents(RecordingFile file) {
        assertTrue(file.hasMoreEvents(), "No events");
    }

    public static void assertEventThread(RecordedEvent event) {
        RecordedThread eventThread = event.getThread();
        if (eventThread == null) {
            System.out.printf("Failed event:%n%s%n", event.toString());
            fail("No thread in event");
        }
    }

    public static void assertJavaMethod(RecordedEvent event) {
        assertField(event, "method.name").notEmpty();
        assertField(event, "method.descriptor").notEmpty();
        assertField(event, "method.modifiers").atLeast(0);
        assertField(event, "method.hidden");
        assertField(event, "method.type.name").notEmpty();
        assertField(event, "method.type.modifiers").atLeast(0);
    }

    public static void assertEventThread(RecordedEvent event, Thread thread) {
        assertThread(event.getThread(), thread);
    }

    public static void assertEventThread(RecordedEvent event, String structName, Thread thread) {
        assertThread(assertField(event, structName).notNull().getValue(), thread);
    }

    public static void assertDuration(RecordedEvent event, String name, Duration duration) {
        assertEquals(event.getDuration(name), duration);
    }

    public static void assertInstant(RecordedEvent event, String name, Instant instant) {
        assertEquals(event.getInstant(name), instant);
    }

    public static void assertMissingValue(RecordedEvent event, String name) {
       ValueDescriptor v =  event.getEventType().getField(name);
       if (v.getAnnotation(Timespan.class) != null) {
           Duration d = event.getDuration(name);
           assertTrue(d.getSeconds() == Long.MIN_VALUE && d.getNano() == 0);
           return;
       }
       if (v.getAnnotation(Timestamp.class) != null) {
           Instant instant = event.getInstant(name);
           assertTrue(instant.equals(Instant.MIN));
           return;
       }
       if (v.getTypeName().equals("double")) {
           double d = event.getDouble(name);
           assertTrue(Double.isNaN(d) || d == Double.NEGATIVE_INFINITY);
           return;
       }
       if (v.getTypeName().equals("float")) {
           float f = event.getFloat(name);
           assertTrue(Float.isNaN(f) || f == Float.NEGATIVE_INFINITY);
           return;
       }
       if (v.getTypeName().equals("int")) {
           int i = event.getInt(name);
           assertTrue(i == Integer.MIN_VALUE);
           return;
       }
       if (v.getTypeName().equals("long")) {
           assertEquals(event.getLong(name), Long.MIN_VALUE);
           return;
       }
       Object o = event.getValue(name);
       Asserts.assertNull(o);
    }

    private static void assertThread(RecordedThread eventThread, Thread thread) {
        assertNotNull(eventThread, "Thread in event was null");
        assertEquals(eventThread.getJavaThreadId(), thread.getId(), "Wrong thread id");
        assertEquals(eventThread.getJavaName(), thread.getName(), "Wrong thread name");

        ThreadGroup threadGroup = thread.getThreadGroup();
        RecordedThreadGroup eventThreadGroup = eventThread.getThreadGroup();
        assertNotNull(eventThreadGroup, "eventThreadGroup was null");

        // Iterate and check all threadGroups
        while (eventThreadGroup != null) {
            final String groupName = eventThreadGroup.getName();
            if (threadGroup != null) {
                assertEquals(groupName, threadGroup.getName(), "Wrong threadGroup name");
                threadGroup = threadGroup.getParent();
            } else {
                assertNotNull(groupName, "threadGroup name was null");
                assertFalse(groupName.isEmpty(), "threadGroup name was empty");
            }
            eventThreadGroup = eventThreadGroup.getParent();
        }
    }

    public static boolean hasField(RecordedEvent event, String name) {
        return event.getFields().stream().map(vd -> vd.getName()).anyMatch(s -> s.equals(name));
    }

    public static boolean isEventType(RecordedEvent event, String typeName) {
        return typeName.equals(event.getEventType().getName());
    }


    /**
     * Creates a list of events from a recording.
     *
     * @param recording recording, not {@code null}
     * @return an a list, not null
     * @throws IOException if an event set could not be created due to I/O
     *         errors.
     */
    public static List<RecordedEvent> fromRecording(Recording recording) throws IOException {
        return RecordingFile.readAllEvents(makeCopy(recording));
    }

    public static RecordingFile copyTo(Recording r) throws IOException {
        return new RecordingFile(makeCopy(r));
    }

    private static Path makeCopy(Recording recording) throws IOException {
        Path p = recording.getDestination();
        if (p == null) {
            File directory = new File(".");
            // FIXME: Must come up with a way to give human-readable name
            // this will at least not clash when running parallel.
            ProcessHandle h = ProcessHandle.current();
            p = new File(directory.getAbsolutePath(), "recording-" + recording.getId() + "-pid" + h.pid() + ".jfr").toPath();
            recording.dump(p);
        }
        return p;
    }

   public static void hasAnnotation(ValueDescriptor field, Class<? extends java.lang.annotation.Annotation> annotationClass) throws Exception {
       AnnotationElement a = getAnnotation(field, annotationClass);
       if (a == null) {
           throw new Exception("Expected " + annotationClass.getSimpleName() + " on field " + field.getName());
       }
   }

   public static void assertAnnotation(ValueDescriptor field, Class<? extends java.lang.annotation.Annotation> annotationClass, String value) throws Exception {
       AnnotationElement a = getAnnotation(field, annotationClass);
       Object v = a.getValue("value");
       if (!v.equals(value)) {
           throw new Exception("Expected " + annotationClass.getSimpleName() + " on field " + field.getName() + " to have value " + value + ", but got " + v);
       }
   }

   // candidate for moving into API
   public static AnnotationElement getAnnotation(ValueDescriptor v, Class<?> clazz) throws Exception {
      for (AnnotationElement a : v.getAnnotationElements()) {
          if (a.getTypeName().equals(clazz.getName())) {
              return a;
          }
      }

      throw new Exception("Could not find annotation " + clazz.getName());
  }

   // candidate for moving into API
   public static AnnotationElement getAnnotationByName(EventType t, String name) throws Exception {
       for (AnnotationElement a : t.getAnnotationElements()) {
           if (a.getTypeName().equals(name)) {
               return a;
           }
       }
       throw new Exception("Could not find annotation '" + name + " in type " + t.getName());
   }

    // candidate for moving into API
    public static SettingDescriptor getSetting(EventType type, String name) {
        for (SettingDescriptor s : type.getSettingDescriptors()) {
            if (s.getName().equals(name)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Could not setting with name " + name);
    }

    public static void hasEvent(Recording r, String name) throws IOException {
        List<RecordedEvent> events = fromRecording(r);
        Events.hasEvents(events);
        Events.hasEvent(events, name);
    }

    public static void hasEvent(List<RecordedEvent> events, String name) throws IOException {
        if (!containsEvent(events, name)) {
            Asserts.fail("Missing event " + name  + " in recording " + events.toString());
        }
    }

    public static void hasNotEvent(List<RecordedEvent> events, String name) throws IOException {
        if (containsEvent(events, name)) {
            Asserts.fail("Rercording should not contain event " + name  + " " + events.toString());
        }
    }

    private static boolean containsEvent(List<RecordedEvent> events, String name) {
        for (RecordedEvent event : events) {
            if (event.getEventType().getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static void assertFrame(RecordedEvent event, Class<?> expectedClass, String expectedMethodName) {
        RecordedStackTrace stackTrace = event.getStackTrace();
        Asserts.assertNotNull(stackTrace, "Missing stack trace");
        for (RecordedFrame frame : stackTrace.getFrames()) {
            if (frame.isJavaFrame()) {
                RecordedMethod method = frame.getMethod();
                RecordedClass type = method.getType();
                if (expectedClass.getName().equals(type.getName())) {
                    if (expectedMethodName.equals(method.getName())) {
                        return;
                    }
                }
            }
        }
        Asserts.fail("Expected " + expectedClass.getName() + "::"+ expectedMethodName + " in stack trace");
    }
}
