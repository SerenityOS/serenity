/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.startupargs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.SimpleEvent;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.startupargs.TestRetransformUsingLog
 */
public class TestRetransformUsingLog {

    private static final String FILE_READ_FORCED_CLASS_LOAD = "Adding forced instrumentation for event type " + EventNames.FileRead + " during initial class load";
    private static final String SIMPLE_EVENT_FORCED_CLASS_LOAD = "Adding forced instrumentation for event type jdk.test.lib.jfr.SimpleEvent during initial class load";
    private static final String SIMPLE_EVENT_UNFORCED_CLASS_LOAD = "Adding instrumentation for event type jdk.test.lib.jfr.SimpleEvent during initial class load";

    public static class TestApp {
        public static void main(String[] args) throws Exception {
            SimpleEvent event = new SimpleEvent();
            event.commit();
        }
    }

    public static void main(String[] args) throws Exception {
        testRecordingRetransFormFalse();
        testRecordingRetransFormTrue();
        testNoRecordingRetransFormFalse();
        testNoRecordingRetransFormTrue();
    }

    private static void testRecordingRetransFormFalse() throws Exception {
        startApp(true, false, out -> {
            out.shouldContain(FILE_READ_FORCED_CLASS_LOAD);
            out.shouldContain(SIMPLE_EVENT_FORCED_CLASS_LOAD);
        });
    }

    private static void testRecordingRetransFormTrue() throws Exception {
        startApp(true, true, out -> {
            out.shouldContain(FILE_READ_FORCED_CLASS_LOAD);
            out.shouldContain(SIMPLE_EVENT_UNFORCED_CLASS_LOAD);
        });
    }

    private static void testNoRecordingRetransFormFalse() throws Exception {
        startApp(false, false, out -> {
            out.shouldNotContain(FILE_READ_FORCED_CLASS_LOAD);
            out.shouldContain(SIMPLE_EVENT_FORCED_CLASS_LOAD);
        });
    }

    private static void testNoRecordingRetransFormTrue() throws Exception {
        startApp(false, true, out -> {
            out.shouldNotContain(FILE_READ_FORCED_CLASS_LOAD);
            out.shouldNotContain(SIMPLE_EVENT_FORCED_CLASS_LOAD);
            out.shouldNotContain(SIMPLE_EVENT_UNFORCED_CLASS_LOAD);
        });
    }

    private static void startApp(boolean recording, boolean retransform, Consumer<OutputAnalyzer> verifier) throws Exception {
        List<String> args = new ArrayList<>();
        args.add("-Xlog:jfr+system");
        args.add("-XX:FlightRecorderOptions:retransform=" + retransform);
        if (recording) {
            args.add("-XX:StartFlightRecording");
        }
        args.add(TestApp.class.getName());
        System.out.println();
        System.out.println("Starting test app:");
        System.out.print("java ");
        for (String arg : args) {
            System.out.print(arg + " ");
        }
        System.out.println();
        System.out.println();
        ProcessBuilder pb = ProcessTools.createTestJvm(args);
        OutputAnalyzer out = ProcessTools.executeProcess(pb);
        System.out.println(out.getOutput());
        verifier.accept(out);
    }
}
