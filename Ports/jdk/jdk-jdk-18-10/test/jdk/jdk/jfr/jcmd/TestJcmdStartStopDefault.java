/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jcmd;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.FileHelper;
import jdk.test.lib.process.OutputAnalyzer;

/**
 * @test
 * @summary Start a recording without name.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jcmd.TestJcmdStartStopDefault
 */
public class TestJcmdStartStopDefault {

    public static void main(String[] args) throws Exception {
        Path recording = Paths.get(".","TestJcmdStartStopDefault.jfr").toAbsolutePath().normalize();

        OutputAnalyzer output = JcmdHelper.jcmd("JFR.start");
        JcmdAsserts.assertRecordingHasStarted(output);

        String name = parseRecordingName(output);
        JcmdHelper.waitUntilRunning(name);

        output = JcmdHelper.jcmd("JFR.dump",
                "name=" + name,
                "filename=" + recording);
        JcmdAsserts.assertRecordingDumpedToFile(output, recording.toFile());
        JcmdHelper.stopAndCheck(name);
        FileHelper.verifyRecording(recording.toFile());
    }

    private static String parseRecordingName(OutputAnalyzer output) {
        // Expected output:
        // Started recording recording-1. No limit (duration/maxsize/maxage) in use.
        // Use JFR.dump name=recording-1 filename=FILEPATH to copy recording data to file.

        String stdout = output.getStdout();
        Pattern p = Pattern.compile(".*Use jcmd \\d+ JFR.dump name=(\\S+).*", Pattern.DOTALL);
        Matcher m = p.matcher(stdout);
        Asserts.assertTrue(m.matches(), "Could not parse recording name");
        String name = m.group(1);
        System.out.println("Recording name=" + name);
        return name;
    }
}
