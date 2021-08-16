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
 *
 */

/**
 * @test
 * @bug 8145221
 * @summary After creating an AppCDS archive, run the test with the JFR profiler
 *          enabled, and keep calling a method in the archive in a tight loop.
 *          This is to test the safe handling of trampoline functions by the
 *          profiler.
 * @requires vm.hasJFR & vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile test-classes/MyThread.java
 * @compile test-classes/TestWithProfilerHelper.java
 * @run driver TestWithProfiler
 */

import jdk.test.lib.BuildHelper;
import jdk.test.lib.process.OutputAnalyzer;

public class TestWithProfiler {
    public static void main(String[] args) throws Exception {
        JarBuilder.build("myThread", "MyThread", "TestWithProfilerHelper");
        String appJar = TestCommon.getTestJar("myThread.jar");
        OutputAnalyzer output = TestCommon.dump(appJar,
            TestCommon.list("MyThread", "TestWithProfilerHelper"));
        TestCommon.checkDump(output);
        output = TestCommon.exec(appJar,
            "-XX:+UnlockDiagnosticVMOptions",
            "-Xint",
            "-XX:StartFlightRecording:duration=15s,filename=myrecording.jfr,settings=profile,dumponexit=true",
            "TestWithProfilerHelper");
        TestCommon.checkExec(output);
    }
}
