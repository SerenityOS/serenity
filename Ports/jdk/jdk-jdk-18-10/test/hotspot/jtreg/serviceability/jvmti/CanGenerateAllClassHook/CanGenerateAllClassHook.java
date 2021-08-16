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

/**
 * @test
 * @bug 8161605
 * @summary Tests that jvmtiEnv::GetPotentialCapabilities reports
 *          can_generate_all_class_hook_events capability with CDS (-Xshare:on)
 *          at ONLOAD and LIVE phases
 * @requires vm.jvmti
 * @requires vm.cds
 * @requires vm.flagless
 * @library /test/lib
 * @compile CanGenerateAllClassHook.java
 * @run main/othervm/native CanGenerateAllClassHook
 */

import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import java.io.File;
import java.io.IOException;

/*
 * The simplest way to test is to use system classes.jsa,
 * but we cannot rely on tested JRE/JDK has it.
 * So the test runs 2 java processes -
 * 1st to generate custom shared archive file:
 *     java -XX:+UnlockDiagnosticVMOptions -XX:SharedArchiveFile=<jsa_file> -Xshare:dump
 * and 2nd to perform the actual testing using generated shared archive:
 *     java -XX:+UnlockDiagnosticVMOptions -XX:SharedArchiveFile=<jsa_file> -Xshare:on
  *         -agentlib:<agent> CanGenerateAllClassHook
 */
public class CanGenerateAllClassHook {

    private static final String agentLib = "CanGenerateAllClassHook";

    private static native int getClassHookAvail();
    private static native int getOnLoadClassHookAvail();

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            // this is master run

            final File jsaFile = File.createTempFile(agentLib, ".jsa");
            jsaFile.deleteOnExit();
            final String jsaPath = jsaFile.getAbsolutePath();

            log("generating CDS archive...");
            execJava(
                        "-XX:+UnlockDiagnosticVMOptions",
                        "-XX:SharedArchiveFile=" + jsaPath,
                        "-Xshare:dump")
                    .shouldHaveExitValue(0);
            log("CDS generation completed.");

            OutputAnalyzer output = execJava(
                    "-XX:+UnlockDiagnosticVMOptions",
                    "-XX:SharedArchiveFile=" + jsaPath,
                    "-Xshare:on",
                    "-agentlib:" + agentLib,
                    // copy java.library.path
                    "-Djava.library.path=" + System.getProperty("java.library.path"),
                    // specify "-showversion" to ensure the test runs in shared mode
                    "-showversion",
                    // class to run
                    CanGenerateAllClassHook.class.getCanonicalName(),
                    // and arg
                    "test");
            // Xshare:on can cause intermittent failure
            // checkExec handles this.
            CDSTestUtils.checkExec(output);

            log("Test PASSED.");
        } else {
            // this is test run
            try {
                System.loadLibrary(agentLib);
            } catch (UnsatisfiedLinkError ex) {
                System.err.println("Failed to load " + agentLib + " lib");
                System.err.println("java.library.path: " + System.getProperty("java.library.path"));
                throw ex;
            }

            final int onLoadValue = getOnLoadClassHookAvail();
            final int liveValue = getClassHookAvail();
            // Possible values returned:
            // 1 - the capability is supported;
            // 0 - the capability is not supported;
            // -1 - error occured.

            log("can_generate_all_class_hook_events value capability:");
            log("ONLOAD phase: " + (onLoadValue < 0 ? "Failed to read" : onLoadValue));
            log("LIVE phase: " + (liveValue < 0 ? "Failed to read" : liveValue));
            if (onLoadValue != 1 || liveValue != 1) {
                throw new RuntimeException("The can_generate_all_class_hook_events capability "
                        + " is expected to be available in both ONLOAD and LIVE phases");
            }
        }
    }

    private static void log(String msg) {
        System.out.println(msg);
        System.out.flush();
    }

    private static OutputAnalyzer execJava(String... args) throws IOException {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(args);

        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        log("[STDERR]\n" + output.getStderr());
        log("[STDOUT]\n" + output.getStdout());

        return output;
    }

}
