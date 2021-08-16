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

import jdk.jfr.Recording;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main jdk.jfr.startupargs.TestStartName
 */
public class TestStartName {

    public static class TestName {
        public static void main(String[] args) throws Exception {
            Recording r = StartupHelper.getRecording(args[0]);
            Asserts.assertNotNull(r);
        }
    }

    private static void testName(String recordingName, boolean validName) throws Exception {
        ProcessBuilder pb = ProcessTools.createTestJvm(
            "-XX:StartFlightRecording:name=" + recordingName, TestName.class.getName(), recordingName);
        OutputAnalyzer out = ProcessTools.executeProcess(pb);

        if (validName) {
            out.shouldHaveExitValue(0);
        } else {
            out.shouldHaveExitValue(1);
            out.shouldContain("Name of recording can't be numeric");
        }
    }

    public static void main(String[] args) throws Exception {
        testName("12345a", true);
        testName("--12345", true);
        testName("[()]", true);

        // numeric names should not be accepted
        testName("100", false);
        testName("-327", false);
        testName("+511", false);
    }
}
