/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.List;
import java.util.Set;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.SimpleEvent;


/**
 * @test
 * @summary Simple test for RecordedFrame APIs
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm -Xint  -XX:+UseInterpreter -Dinterpreted=true  jdk.jfr.api.consumer.TestRecordedFrame
 * @run main/othervm -Xcomp -XX:-UseInterpreter -Dinterpreted=false jdk.jfr.api.consumer.TestRecordedFrame
 */
public final class TestRecordedFrame {

    public static void main(String[] args) throws IOException {
        doSomething(); // Makes BCI for method larger than 0
        test(); // Records the line number and BCI for the main method/frame
    }

    static void doSomething() {
        // Don't actually do anything: on platforms that do not support
        // patching (AArch64) -Xcomp is very sensitive to class load
        // order and calling other methods here might result in main()
        // being deoptimized, failing the test below.
    }

    static void test() throws IOException {
        try (Recording recording = new Recording()) {
            recording.start();

            SimpleEvent ev = new SimpleEvent();
            ev.commit();
            recording.stop();

            List<RecordedEvent> recordedEvents = Events.fromRecording(recording);
            Events.hasEvents(recordedEvents);
            RecordedEvent recordedEvent = recordedEvents.get(0);

            RecordedStackTrace stacktrace = recordedEvent.getStackTrace();
            List<RecordedFrame> frames = stacktrace.getFrames();
            for (RecordedFrame frame : frames) {
                // All frames are java frames
                Asserts.assertTrue(frame.isJavaFrame());
                // Verify the main() method frame
                RecordedMethod method = frame.getMethod();
                if (method.getName().equals("main")) {
                    // Frame type
                    String type = frame.getType();
                    System.out.println("type: " + type);
                    Set<String> types = Set.of("Interpreted", "JIT compiled", "Inlined");
                    Asserts.assertTrue(types.contains(type));
                    // Line number
                    Asserts.assertEquals(getLineNumber("main"), frame.getLineNumber());
                    // Interpreted
                    boolean isInterpreted = "Interpreted".equals(type);
                    boolean expectedInterpreted = "true".equals(System.getProperty("interpreted"));
                    Asserts.assertEquals(isInterpreted, expectedInterpreted);
                    // BCI
                    int bci = frame.getBytecodeIndex();
                    System.out.println("bci: " + bci);
                    Asserts.assertGreaterThan(bci, 0);
                }
            }
        }
    }

    /**
     * Returns line number of a method on the stack
     */
    private static int getLineNumber(String methodName) {
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            if (methodName.equals(ste.getMethodName())) {
                return ste.getLineNumber();
            }
        }
        throw new RuntimeException("Unexpected error: could not analyze stacktrace");
    }
}
