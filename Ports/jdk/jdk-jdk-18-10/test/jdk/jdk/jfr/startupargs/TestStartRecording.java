/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;

/**
 * @test
 * @summary Start a recording with -XX:StartFlightRecording. Dump recording with jcmd.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm -XX:StartFlightRecording:name=TestStartRecording,settings=profile jdk.jfr.startupargs.TestStartRecording
 */
public class TestStartRecording {

    public static void main(String[] args) throws Exception {
        final String recordingName = "TestStartRecording";
        OutputAnalyzer output = StartupHelper.jcmd("JFR.check");
        output.shouldContain(recordingName);

        Path path = Paths.get(".", "my.jfr");
        output = StartupHelper.jcmd("JFR.dump", "name=" + recordingName, "filename=" + path);
        Asserts.assertFalse(RecordingFile.readAllEvents(path).isEmpty(), "No events found");
    }

}
