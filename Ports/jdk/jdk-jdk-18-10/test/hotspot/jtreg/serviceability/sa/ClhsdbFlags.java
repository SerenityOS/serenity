/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.Utils;
import jtreg.SkippedException;

/**
 * @test
 * @bug 8190198
 * @bug 8217612
 * @bug 8217845
 * @summary Test clhsdb flags command
 * @requires vm.hasSA
 * @library /test/lib
 * @run driver ClhsdbFlags
 */

public class ClhsdbFlags {

    public static void runBasicTest() throws Exception {
        System.out.println("Starting ClhsdbFlags basic test");

        LingeredApp theApp = null;
        try {
            ClhsdbLauncher test = new ClhsdbLauncher();
            theApp = LingeredApp.startApp(
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UnlockDiagnosticVMOptions",
                    "-XX:-MaxFDLimit");
            System.out.println("Started LingeredApp with pid " + theApp.getPid());

            List<String> cmds = List.of(
                    "flags", "flags -nd",
                    "flags UnlockDiagnosticVMOptions", "flags MaxFDLimit",
                    "flags MaxJavaStackTraceDepth");

            Map<String, List<String>> expStrMap = new HashMap<>();
            expStrMap.put("flags", List.of(
                    "command line", "ergonomic", "default",
                    "UnlockDiagnosticVMOptions = true",
                    "MaxFDLimit = false",
                    "MaxJavaStackTraceDepth = 1024",
                    "ConcGCThreads", "UseThreadPriorities",
                    "ShowHiddenFrames"));
            expStrMap.put("flags -nd", List.of(
                    "UnlockDiagnosticVMOptions = true",
                    "MaxFDLimit = false",
                    "InitialHeapSize",
                    "MaxHeapSize"));
            expStrMap.put("flags UnlockDiagnosticVMOptions", List.of(
                    "UnlockDiagnosticVMOptions = true"));
            expStrMap.put("flags MaxFDLimit", List.of(
                    "MaxFDLimit = false"));
            expStrMap.put("flags MaxJavaStackTraceDepth", List.of(
                    "MaxJavaStackTraceDepth = 1024"));

            test.run(theApp.getPid(), cmds, expStrMap, null);
        } catch (SkippedException se) {
            throw se;
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        } finally {
            LingeredApp.stopApp(theApp);
        }
        System.out.println("Test PASSED");
    }

    public static void runAllTypesTest() throws Exception {
        System.out.println("Starting ClhsdbFlags all types test");

        LingeredApp theApp = null;
        try {
            ClhsdbLauncher test = new ClhsdbLauncher();
            // *Prepend* options to prevent interference with flags below
            String[] vmArgs = Utils.prependTestJavaOpts(
                "-XX:+UnlockDiagnosticVMOptions",  // bool
                "-XX:ActiveProcessorCount=1",      // int
                "-XX:ParallelGCThreads=1",         // uint
                "-XX:MaxJavaStackTraceDepth=1024", // intx
                "-XX:LogEventsBufferEntries=10",   // uintx
                "-XX:HeapSizePerGCThread=32m",     // size_t
                "-XX:NativeMemoryTracking=off",    // ccstr
                "-XX:OnError='echo error'",        // ccstrlist
                "-XX:CompileThresholdScaling=1.0", // double
                "-XX:ErrorLogTimeout=120");        // uint64_t
            theApp = new LingeredApp();
            LingeredApp.startAppExactJvmOpts(theApp, vmArgs);
            System.out.println("Started LingeredApp with pid " + theApp.getPid());

            List<String> cmds = List.of("flags");

            Map<String, List<String>> expStrMap = new HashMap<>();
            expStrMap.put("flags", List.of(
                    "UnlockDiagnosticVMOptions = true",
                    "ActiveProcessorCount = 1",
                    "ParallelGCThreads = 1",
                    "MaxJavaStackTraceDepth = 1024",
                    "LogEventsBufferEntries = 10",
                    "HeapSizePerGCThread = 3",
                    "NativeMemoryTracking = \"off\"",
                    "OnError = \"'echo error'\"",
                    "CompileThresholdScaling = 1.0",
                    "ErrorLogTimeout = 120"));

            test.run(theApp.getPid(), cmds, expStrMap, null);
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        } finally {
            LingeredApp.stopApp(theApp);
        }
        System.out.println("Test PASSED");
    }

    public static void main(String[] args) throws Exception {
        runBasicTest();
        runAllTypesTest();
    }
}
