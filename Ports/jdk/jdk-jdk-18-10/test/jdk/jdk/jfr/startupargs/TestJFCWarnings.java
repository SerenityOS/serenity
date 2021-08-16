/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/**
 * @test
 * @summary Start a recording with custom settings
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @modules jdk.jfr/jdk.jfr.internal
 *
 * @run main/othervm jdk.jfr.startupargs.TestJFCWarnings
 */
public class TestJFCWarnings {

    public static void main(String... args) throws Exception {
        testUnknownOption();
        testSpellingError();
    }

    private static void testSpellingError() throws Exception {
        // One character spelling error
        launch("-XX:StartFlightRecording:disc=false",
             "Did you mean 'disk' instead of 'disc'?");
        // One missing character
        launch("-XX:StartFlightRecording:setting=my.jfc",
             "Did you mean 'settings' instead of 'setting'?");
        // One additional character
        launch("-XX:StartFlightRecording:disk=false,paths-to-gc-roots=true,name=test",
             "Did you mean 'path-to-gc-roots' instead of 'paths-to-gc-roots'?");
        // Incorrect case
        launch("-XX:StartFlightRecording:fileName=recording.jfr,disk=false",
             "Did you mean 'filename' instead of 'fileName'?");
        // Two character spelling error in option with more than 6 characters
        launch("-XX:StartFlightRecording:name=wrong,filenaim=recording.jfr",
             "Did you mean 'filename' instead of 'filenaim'?");
    }

    private static void testUnknownOption() throws Exception {
        // Unknown .jfc option
        launch("-XX:StartFlightRecording:zebra=high",
               "The .jfc option/setting 'zebra' doesn't exist.");
        // Unknown event setting
        launch("-XX:StartFlightRecording:com.example.Hello#enabled=true",
               "The .jfc option/setting 'com.example.Hello#enabled' doesn't exist.");
    }

    private static void launch(String commandLine, String expectedOutput) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(commandLine, "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain(expectedOutput);
    }

}
