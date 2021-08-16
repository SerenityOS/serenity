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

/**
 * @test
 * @bug 8157032
 * @key jfr
 * @summary verify that jfr can not be used when JVM is executed only with java.base
 * @requires vm.hasJFR & !vm.graal.enabled
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @run driver jdk.jfr.jvm.TestJfrJavaBase
 */

package jdk.jfr.jvm;


import jdk.test.lib.dcmd.PidJcmdExecutor;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestJfrJavaBase {

    private static void checkOutput(OutputAnalyzer output) {
        output.shouldContain("Module jdk.jfr not found.");
        output.shouldContain("Flight Recorder can not be enabled.");
    }

    public static void main(String[] args) throws Exception {
        OutputAnalyzer output;
        if (args.length == 0) {
            output = ProcessTools.executeProcess(ProcessTools.createJavaProcessBuilder(
                "-Dtest.jdk=" + System.getProperty("test.jdk"),
                "--limit-modules", "java.base", "-cp", System.getProperty("java.class.path"),
                TestJfrJavaBase.class.getName(), "runtest"));
            output.shouldHaveExitValue(0);
        } else {
            output = ProcessTools.executeTestJava("-XX:StartFlightRecording:dumponexit=true",
                "--limit-modules", "java.base", "-version");
            checkOutput(output);
            output.shouldHaveExitValue(1);

            // Verify that JFR.start jcmd command reports an error when jdk.jfr module is not available
            output = new PidJcmdExecutor().execute("JFR.start");
            checkOutput(output);
            output.shouldHaveExitValue(0);
        }
    }
}
