/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.jvm;


import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import jdk.jfr.AnnotationElement;
import jdk.jfr.Configuration;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.EventFactory;
import jdk.jfr.EventSettings;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.FlightRecorderListener;
import jdk.jfr.FlightRecorderPermission;
import jdk.jfr.Label;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import jdk.jfr.SettingControl;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordedThreadGroup;
import jdk.jfr.consumer.RecordingFile;
import jdk.jfr.consumer.RecordingStream;
import jdk.management.jfr.ConfigurationInfo;
import jdk.management.jfr.EventTypeInfo;
import jdk.management.jfr.FlightRecorderMXBean;
import jdk.management.jfr.RecordingInfo;
import jdk.management.jfr.SettingDescriptorInfo;
import jdk.test.lib.Utils;

/**
 * @test TestUnsupportedVM
 * @key jfr
 * @requires vm.hasJFR
 *
 * @modules jdk.jfr
 *          jdk.management.jfr
 *
 * @library /test/lib
 * @run main/othervm -Dprepare-recording=true jdk.jfr.jvm.TestUnsupportedVM
 * @run main/othervm -Djfr.unsupported.vm=true jdk.jfr.jvm.TestUnsupportedVM
 */
public class TestUnsupportedVM {

    private static Path RECORDING_FILE = Paths.get("recording.jfr");
    private static Class<?> [] APIClasses = {
            AnnotationElement.class,
            Configuration.class,
            ConfigurationInfo.class,
            Event.class,
            EventFactory.class,
            EventSettings.class,
            EventType.class,
            EventTypeInfo.class,
            FlightRecorder.class,
            FlightRecorderPermission.class,
            FlightRecorderListener.class,
            FlightRecorderMXBean.class,
            RecordedClass.class,
            RecordedEvent.class,
            RecordedFrame.class,
            RecordedMethod.class,
            RecordedObject.class,
            RecordedStackTrace.class,
            RecordedThread.class,
            RecordedThreadGroup.class,
            Recording.class,
            RecordingFile.class,
            RecordingInfo.class,
            RecordingState.class,
            SettingControl.class,
            SettingDescriptorInfo.class,
            ValueDescriptor.class,
            EventStream.class,
            RecordingStream.class
       };

    @Label("My Event")
    @Description("My fine event")
    static class MyEvent extends Event {
        int myValue;
    }

    public static void main(String... args) throws Exception {
        if (Boolean.getBoolean("prepare-recording")) {
            Recording r = new Recording(Configuration.getConfiguration("default"));
            r.start();
            r.stop();
            r.dump(RECORDING_FILE);
            r.close();
            return;
        }

        System.out.println("jfr.unsupported.vm=" + System.getProperty("jfr.unsupported.vm"));
        // Class FlightRecorder
        if (FlightRecorder.isAvailable()) {
            throw new AssertionError("JFR should not be available on an unsupported VM");
        }

        if (FlightRecorder.isInitialized()) {
            throw new AssertionError("JFR should not be initialized on an unsupported VM");
        }

        assertIllegalStateException(() -> FlightRecorder.getFlightRecorder());
        assertIllegalStateException(() -> new RecordingStream());
        assertSwallow(() -> FlightRecorder.addListener(new FlightRecorderListener() {}));
        assertSwallow(() -> FlightRecorder.removeListener(new FlightRecorderListener() {}));
        assertSwallow(() -> FlightRecorder.register(MyEvent.class));
        assertSwallow(() -> FlightRecorder.unregister(MyEvent.class));
        assertSwallow(() -> FlightRecorder.addPeriodicEvent(MyEvent.class, new Runnable() { public void run() {} }));
        assertSwallow(() -> FlightRecorder.removePeriodicEvent(new Runnable() { public void run() {} }));

        // Class Configuration
        if (!Configuration.getConfigurations().isEmpty()) {
            throw new AssertionError("Configuration files should not exist on an unsupported VM");
        }
        Path jfcFile = Utils.createTempFile("empty", ".jfr");
        assertIOException(() -> Configuration.getConfiguration("default"));
        assertIOException(() -> Configuration.create(jfcFile));
        assertIOException(() -> Configuration.create(new FileReader(jfcFile.toFile())));

        // Class EventType
        assertInternalError(() -> EventType.getEventType(MyEvent.class));

        // Class EventFactory
        assertInternalError(() -> EventFactory.create(new ArrayList<>(), new ArrayList<>()));

        // Create a static event
        MyEvent myEvent = new MyEvent();
        myEvent.begin();
        myEvent.end();
        myEvent.shouldCommit();
        myEvent.commit();

        // Trigger class initialization failure
        for (Class<?> c : APIClasses) {
            assertNoClassInitFailure(c);
        }

        // jdk.jfr.consumer.*
        // Only run this part of tests if we are on VM
        // that can produce a recording file
        if (Files.exists(RECORDING_FILE)) {
            boolean firstFileEvent = true;
            for(RecordedEvent re : RecordingFile.readAllEvents(RECORDING_FILE)) {
                // Print one event
                if (firstFileEvent) {
                    System.out.println(re);
                    firstFileEvent = false;
                }
            }
            AtomicBoolean firstStreamEvent = new AtomicBoolean(true);
            try (EventStream es = EventStream.openFile(RECORDING_FILE)) {
                es.onEvent(e -> {
                    // Print one event
                    if (firstStreamEvent.get()) {
                        try {
                            System.out.println(e);
                            firstStreamEvent.set(false);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                });
                es.start();
                if (firstStreamEvent.get()) {
                    throw new AssertionError("Didn't print streaming event");
                }
            }

            try (EventStream es = EventStream.openRepository()) {
                es.onEvent(e -> {
                    System.out.println(e);
                });
                es.startAsync();
                es.awaitTermination(Duration.ofMillis(10));
            }
        }
    }

    private static void assertNoClassInitFailure(Class<?> clazz) {
        try {
            Class.forName(clazz.getName(), true, clazz.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Could not find public API class on unsupported VM");
        }
    }

    private static void assertInternalError(Runnable r) {
        try {
            r.run();
        } catch (InternalError e) {
           // OK, as expected
            return;
        }
        throw new AssertionError("Expected InternalError on an unsupported JVM");
    }

    private static void assertIOException(Callable<?> c) {
        try {
            c.call();
        } catch (Exception e) {
            if (e.getClass() == IOException.class) {
                return;
            }
        }
        throw new AssertionError("Expected IOException on an unsupported JVM");
    }

    private static void assertIllegalStateException(Runnable r) throws Exception {
        try {
            r.run();
        } catch (IllegalStateException ise) {
            if (!ise.getMessage().equals("Flight Recorder is not supported on this VM")) {
                throw new AssertionError("Expected 'Flight Recorder is not supported on this VM'");
            }
        }
    }

    private static void assertSwallow(Runnable r) throws Exception {
        try {
            r.run();
        } catch (Exception e) {
            throw new AssertionError("Unexpected exception '" + e.getMessage() + " on an unspported VM");
        }
    }
}
