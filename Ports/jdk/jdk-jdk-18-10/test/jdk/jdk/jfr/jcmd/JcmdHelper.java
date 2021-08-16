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

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;

import jdk.test.lib.Asserts;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.PidJcmdExecutor;
import jdk.test.lib.process.OutputAnalyzer;

public class JcmdHelper {

    // Wait until recording's state became running
    public static void waitUntilRunning(String name) throws Exception {
        long timeoutAt = System.currentTimeMillis() + 10000;
        while (true) {
            OutputAnalyzer output = jcmdCheck(name, false);
            try {
                // The expected output can look like this:
                // Recording 1: name=1 (running)
                output.shouldMatch("^Recording \\d+: name=" + name
                        + " .*\\W{1}running\\W{1}");
                return;
            } catch (RuntimeException e) {
                if (System.currentTimeMillis() > timeoutAt) {
                    Asserts.fail("Recording not started: " + name);
                }
                Thread.sleep(100);
            }
        }
    }

    public static void stopAndCheck(String name) throws Exception {
        jcmd("JFR.stop", "name=\"" + name + "\"");
        assertRecordingNotRunning(name);
    }

    public static void stopWriteToFileAndCheck(String name, File file) throws Exception {
        OutputAnalyzer output = jcmd("JFR.stop",
                "name=\"" + name + "\"",
                "filename=\"" + file.getAbsolutePath() + "\"");
        JcmdAsserts.assertStoppedAndWrittenTo(output, name, file);
        assertRecordingNotRunning(name);
    }

    public static void stopCompressAndCheck(String name, File file) throws Exception {
        OutputAnalyzer output = jcmd("JFR.stop",
                "name=\"" + name + "\"",
                "compress=true",
                "filename=\"" + file.getAbsolutePath() + "\"");
        JcmdAsserts.assertStoppedAndWrittenTo(output, name, file);
        checkAndAssertNoRecordingsAvailable();
    }

    public static void stopDefaultRecordingAndCheck() throws Exception {
        OutputAnalyzer output = jcmd("JFR.stop", "recording=0");
        JcmdAsserts.assertStoppedDefaultRecording(output);
        checkAndAssertNoRecordingsAvailable();
    }

    public static void checkAndAssertNoRecordingsAvailable() throws Exception {
        OutputAnalyzer output = jcmd("JFR.check");
        JcmdAsserts.assertNoRecordingsAvailable(output);
    }

    public static void assertRecordingNotExist(String name) throws Exception {
        OutputAnalyzer output = jcmdCheck(name, false);
        JcmdAsserts.assertRecordingNotExist(output, name);
    }

    public static void assertRecordingNotRunning(String name) throws Exception {
        OutputAnalyzer output = jcmdCheck(name, false);
        JcmdAsserts.assertRecordingNotRunning(output, name);
    }

    public static void assertRecordingIsRunning(String name) throws Exception {
        OutputAnalyzer output = jcmdCheck(name, false);
        JcmdAsserts.assertRecordingIsRunning(output, name);
    }

    public static OutputAnalyzer jcmd(int expectedExitValue, String... args) {
        String argsString = Arrays.stream(args).collect(Collectors.joining(" "));
        CommandExecutor executor = new PidJcmdExecutor();
        OutputAnalyzer oa = executor.execute(argsString);
        oa.shouldHaveExitValue(expectedExitValue);
        return oa;
    }

    public static OutputAnalyzer jcmd(String... args) {
        return jcmd(0, args);
    }


    public static OutputAnalyzer jcmdCheck(String recordingName, boolean verbose) {
        return jcmd("JFR.check", "name=" + recordingName, "verbose=" + verbose);
    }

    public static String readFilename(OutputAnalyzer output) throws Exception {
        Iterator<String> it = output.asLines().iterator();
        while (it.hasNext()) {
            String line = it.next();
            if (line.contains("written to")) {
                line = it.next(); // blank line
                return it.next();
            }
        }
        throw new Exception("Could not find filename of dumped recording.");
    }
}
