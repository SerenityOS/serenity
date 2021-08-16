/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jdk.internal.misc.Unsafe;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;


/**
 * @test
 * @summary Test Shutdown event
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @modules jdk.jfr
 *          java.base/jdk.internal.misc
 * @run main/othervm jdk.jfr.event.runtime.TestShutdownEvent
 */
public class TestShutdownEvent {
    private static ShutdownEventSubTest subTests[] = {
             new TestLastNonDaemon(),
             new TestSystemExit(),
             new TestVMCrash(),
             new TestUnhandledException(),
             new TestRuntimeHalt(),
             new TestSig("TERM"),
             new TestSig("HUP"),
             new TestSig("INT")
    };

    public static void main(String[] args) throws Throwable {
        for (int i = 0; i < subTests.length; ++i) {
            int attempts = subTests[i].attempts();
            if (attempts == 0) {
                System.out.println("Skipping non-applicable test: " + i);
            }
            for (int j = 0; j < attempts -1; j++) {
                try {
                    runSubtest(i);
                    return;
                } catch (Exception e) {
                    System.out.println("Failed: " + e.getMessage());
                    System.out.println();
                    System.out.println("Retry " + i + 1);
                } catch (OutOfMemoryError | StackOverflowError e) {
                    System.out.println("Error");
                    // Can happen when parsing corrupt file. Abort test.
                    return;
                }
            }
            runSubtest(i);
        }
    }

    private static void runSubtest(int subTestIndex) throws Exception {
        ProcessBuilder pb = ProcessTools.createTestJvm(
                                "-Xlog:jfr=debug",
                                "-XX:-CreateCoredumpOnCrash",
                                "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
                                "-XX:StartFlightRecording:filename=./dumped.jfr,dumponexit=true,settings=default",
                                "jdk.jfr.event.runtime.TestShutdownEvent$TestMain",
                                String.valueOf(subTestIndex));
        OutputAnalyzer output = ProcessTools.executeProcess(pb);
        System.out.println(output.getOutput());
        int exitCode = output.getExitValue();
        System.out.println("Exit code: " + exitCode);

        String recordingName = output.firstMatch("JFR recording file will be written. Location: (.*.jfr)", 1);
        if (recordingName == null) {
            recordingName = "./dumped.jfr";
        }

        List<RecordedEvent> events = RecordingFile.readAllEvents(Paths.get(recordingName));
        List<RecordedEvent> filteredEvents = events.stream()
            .filter(e -> e.getEventType().getName().equals(EventNames.Shutdown))
            .sorted(Comparator.comparing(RecordedEvent::getStartTime))
            .collect(Collectors.toList());

        Asserts.assertEquals(filteredEvents.size(), 1);
        RecordedEvent event = filteredEvents.get(0);
        subTests[subTestIndex].verifyEvents(event, exitCode);
    }

    @SuppressWarnings("unused")
    private static class TestMain {
        public static void main(String[] args) throws Exception {
            ShutdownEventSubTest subTest = subTests[Integer.parseInt(args[0])];
            System.out.println("Running subtest " + args[0] + " (" + subTest.getClass().getName() + ")");
            subTest.runTest();
        }
    }

    private interface ShutdownEventSubTest {
        default int attempts() {
            return 1;
        }
        void runTest();
        void verifyEvents(RecordedEvent event, int exitCode);
    }

    // Basic stack trace validation, checking that the runTest method is part of the stack
    static void validateStackTrace(RecordedStackTrace stackTrace) {
        List<RecordedFrame> frames = stackTrace.getFrames();
        Asserts.assertFalse(frames.isEmpty());
        Asserts.assertTrue(frames.stream()
                           .anyMatch(t -> t.getMethod().getName().equals("runTest")));
    }


    // =========================================================================
    private static class TestLastNonDaemon implements ShutdownEventSubTest {
        @Override
        public void runTest() {
            // Do nothing - this is the default exit reason
        }

        @Override
        public void verifyEvents(RecordedEvent event, int exitCode) {
            Events.assertField(event, "reason").equal("No remaining non-daemon Java threads");
        }
    }

    private static class TestSystemExit implements ShutdownEventSubTest {
        @Override
        public void runTest() {
            System.out.println("Running System.exit");
            System.exit(42);
        }

        @Override
        public void verifyEvents(RecordedEvent event, int exitCode) {
            Events.assertField(event, "reason").equal("Shutdown requested from Java");
            validateStackTrace(event.getStackTrace());
        }
    }

    private static class TestVMCrash implements ShutdownEventSubTest {

        @Override
        public void runTest() {
            System.out.println("Attempting to crash");
            Unsafe.getUnsafe().putInt(0L, 0);
        }

        @Override
        public void verifyEvents(RecordedEvent event, int exitCode) {
            Events.assertField(event, "reason").equal("VM Error");
            // for now avoid validating the stack trace, in case of compiled code
            // the vframeStream based solution will not work in this special VMCrash case
            // see 8219082 for details (running the crashed VM with -Xint would solve the issue too)
            //validateStackTrace(event.getStackTrace());
        }

        @Override
        public int attempts() {
            return 3;
        }
    }

    private static class TestUnhandledException implements ShutdownEventSubTest {
        @Override
        public void runTest() {
            throw new RuntimeException("Unhandled");
        }

        @Override
        public void verifyEvents(RecordedEvent event, int exitCode) {
            Events.assertField(event, "reason").equal("No remaining non-daemon Java threads");
        }
    }

    private static class TestRuntimeHalt implements ShutdownEventSubTest {
        @Override
        public void runTest() {
            System.out.println("Running Runtime.getRuntime.halt");
            Runtime.getRuntime().halt(17);
        }

        @Override
        public void verifyEvents(RecordedEvent event, int exitCode) {
            Events.assertField(event, "reason").equal("Shutdown requested from Java");
            validateStackTrace(event.getStackTrace());
        }
    }

    private static class TestSig implements ShutdownEventSubTest {

        private final String signalName;

        @Override
        public int  attempts() {
            if (Platform.isWindows()) {
                return 0;
            }
            return 1;
        }

        public TestSig(String signalName) {
            this.signalName = signalName;
        }

        @Override
        public void runTest() {
            System.out.println("Sending SIG" + signalName + " to process " + ProcessHandle.current().pid());
            try {
                Runtime.getRuntime().exec("kill -" + signalName + " " + ProcessHandle.current().pid()).waitFor();
                Thread.sleep(60_1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Process survived the SIG" + signalName + " signal!");
        }

        @Override
        public void verifyEvents(RecordedEvent event, int exitCode) {
            if (exitCode == 0) {
                System.out.println("Process exited normally with exit code 0, skipping the verification");
                return;
            }
            Events.assertField(event, "reason").equal("Shutdown requested from Java");
            Events.assertEventThread(event);
            Asserts.assertEquals(event.getThread().getJavaName(), "SIG" + signalName + " handler");
        }
    }
}
