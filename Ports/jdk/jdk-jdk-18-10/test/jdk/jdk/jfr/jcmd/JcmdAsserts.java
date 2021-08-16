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

import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.process.OutputAnalyzer;



public class JcmdAsserts {

    private static final String NEW_LINE = "\n";

    public static void assertJfrNotUsed(OutputAnalyzer output) {
        output.shouldMatch("Flight Recorder has not been used");
    }

    public static void assertJfrUsed(OutputAnalyzer output) {
        output.shouldMatch("Flight Recorder has been used");
    }

    public static void assertRecordingDumpedToFile(OutputAnalyzer output, File recording) {
        output.shouldContain("Dumped recording");
        output.shouldContain(recording.getAbsolutePath());
    }

    public static void assertNotAbleToWriteToFile(OutputAnalyzer output) {
        output.shouldContain("Could not start recording, not able to write to file");
    }

    public static void assertFileNotFoundException(OutputAnalyzer output, String name) {
        output.shouldMatch("Could not write recording \"" + name + "\" to file.*");
    }

//    public static void assertNotAbleToSetFilename(OutputAnalyzer output) {
//        output.shouldContain(
//                "Filename can only be set for a recording with a duration, " +
//                "or if dumponexit=true");
//    }

    public static void assertNotAbleToFindSettingsFile(OutputAnalyzer output) {
        output.shouldContain("Could not parse setting");
    }

    public static void assertNoRecordingsAvailable(OutputAnalyzer output) {
        output.shouldContain("No available recordings");
    }

    public static void assertRecordingNotExist(OutputAnalyzer output, String name) {
        output.shouldContain("Could not find " + name);
    }

    public static void assertRecordingNotRunning(OutputAnalyzer output, String name) {
        output.shouldNotMatch(".*" + name + ".*running");
    }

    public static void assertRecordingIsRunning(OutputAnalyzer output, String name) {
        output.shouldMatch(".*" + name + ".*running");
    }

    public static void assertRecordingHasStarted(OutputAnalyzer output) {
        output.shouldContain("Started recording");
    }

    public static void assertCouldNotStartDefaultRecordingWithName(OutputAnalyzer output) {
        output.shouldContain(
                "It's not possible to set custom name for the defaultrecording");
    }

    public static void assertCouldNotStartDefaultRecording(OutputAnalyzer output) {
        output.shouldContain(
                "The only option that can be combined with defaultrecording is settings");
    }

    public static void assertRecordingIsUnstarted(OutputAnalyzer output,
            String name, String duration) {
        output.stdoutShouldMatch("^Recording \\d+: name=" + name
                + " duration=" + duration + " .*\\W{1}unstarted\\W{1}");
    }

    public static void assertRecordingIsStopped(OutputAnalyzer output, String name) {
        output.stdoutShouldMatch("^Recording \\d+: name=" + name
                + " .*\\W{1}stopped\\W{1}");
    }

    public static void assertRecordingIsStopped(OutputAnalyzer output, String name, String duration) {
        output.stdoutShouldMatch("^Recording \\d+: name=" + name
                + " duration=" + duration + " .*\\W{1}stopped\\W{1}");
    }

    public static void assertStartTimeGreaterOrEqualThanMBeanValue(String name,
            long actualStartTime) throws Exception {
        Recording recording = findRecording(name);
        Asserts.assertNotNull(recording.getStartTime(), "Start time is not set");
        Asserts.assertGreaterThanOrEqual(actualStartTime, recording.getStartTime().toEpochMilli());
    }

    public static void assertDelayAtLeast1s(OutputAnalyzer output) {
        output.shouldContain("Could not start recording, delay must be at least 1 second.");
    }

    public static void assertRecordingIsScheduled(OutputAnalyzer output, String name, String delay) {
        output.stdoutShouldMatch(
                "^\\s*Recording\\s+" + name + "\\s+scheduled to start in " + delay);
    }

    public static void assertMaxSizeEqualsMBeanValue(String name, long maxSize) throws Exception {
        Recording recording = findRecording(name);
        Asserts.assertEquals(maxSize, recording.getMaxSize());
    }

    private static Recording findRecording(String name) {
                for(Recording r : FlightRecorder.getFlightRecorder().getRecordings()) {
                        if (r.getName().equals(name)) {
                                return r;
                        }
                }
                throw new AssertionError("Could not find recording named " + name);
        }

        public static void assertMaxAgeEqualsMBeanValue(String name, long maxAge)
            throws Exception {
        Recording recording = findRecording(name);
        Asserts.assertNotNull(recording, "No recording found");
        Asserts.assertEquals(maxAge, recording.getMaxAge().toMillis());
    }

    public static void assertDurationEqualsMBeanValue(String name,
            long duration) throws Exception {
        Recording recording = findRecording(name);
        Asserts.assertNotNull(recording, "No recording found");
        Asserts.assertEquals(duration, recording.getDuration().toMillis());
    }

    public static void assertDurationAtLeast1s(OutputAnalyzer output) {
        output.shouldContain("Could not start recording, duration must be at least 1 second.");
    }

    public static void assertStoppedRecording(OutputAnalyzer output, String name) {
        output.shouldContain("Stopped recording \"" + name + "\"");
    }

    public static void assertStoppedAndWrittenTo(OutputAnalyzer output, String name, File file) {
        output.shouldMatch("^Stopped recording \"" + name + "\"" + ".*written to:");
        output.shouldContain(file.getAbsolutePath());
    }

    public static void assertStoppedDefaultRecording(OutputAnalyzer output) {
        output.shouldContain("Stopped recording 0");
    }

    public static void assertThreadSleepThresholdIsSet(OutputAnalyzer output) throws Exception {
        output.stdoutShouldMatch("\\s+\\W{1}" + EventNames.ThreadSleep + "\\W{1}" +
                NEW_LINE + ".*threshold=1 ms.*");
    }

    public static void assertMonitorWaitThresholdIsSet(OutputAnalyzer output) throws Exception {
        output.stdoutShouldMatch("\\s+\\W{1}" + EventNames.JavaMonitorWait + "\\W{1}" +
                NEW_LINE + ".*threshold=1 ms.*");
    }

}
