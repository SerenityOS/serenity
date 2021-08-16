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

package jdk.jfr.jcmd;

import java.io.File;

import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import jdk.test.lib.process.OutputAnalyzer;

/**
 * @test
 * @summary The test verifies that recording can be started with options delay|duration|maxage|maxsize
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm -XX:FlightRecorderOptions:maxchunksize=2097152 jdk.jfr.jcmd.TestJcmdStartWithOptions
 */
public class TestJcmdStartWithOptions {

    private static final String DIR = System.getProperty("test.src", ".");
    private static final File SETTINGS = new File(DIR, "jcmd-testsettings3.jfc");

    public static void main(String[] args) throws Exception {
        testRecordingNotStartedTooEarly();
        testDelayLessThan1s();
        testDuration();
        testDurationLessThan1s();
        testMaxAge();
        testMaxSize();
    }

    static void testRecordingNotStartedTooEarly() throws Exception {
        String name = "testRecordingNotStartedTooEarly";
        long delay = 2 * 1000;
        OutputAnalyzer output = JcmdHelper.jcmd("JFR.start",
                "name=" + name,
                "delay=" + delay + "ms");
        JcmdAsserts.assertRecordingIsScheduled(output, "1", "2 s");
        for (Recording r : FlightRecorder.getFlightRecorder().getRecordings()) {
            if (name.equals(r.getName())) {
                while(r.getState() != RecordingState.RUNNING) {
                    Thread.sleep(10);
                }
                long currentTime = System.currentTimeMillis();
                long afterActualStart = currentTime + delay;
                JcmdAsserts.assertStartTimeGreaterOrEqualThanMBeanValue(name, afterActualStart);
                JcmdHelper.stopAndCheck(name);
                return;
            }
        }
        throw new Exception("Could not find recording with name " + name);
    }

    private static void testDelayLessThan1s() throws Exception {
        String name = "testDelayLessThan1s";
        OutputAnalyzer output = JcmdHelper.jcmd("JFR.start",
                "name=" + name,
                "delay=10ms");
        JcmdAsserts.assertDelayAtLeast1s(output);
        output = JcmdHelper.jcmd("JFR.check");
        JcmdAsserts.assertNoRecordingsAvailable(output);
    }

    private static void testDuration() throws Exception {
        String name = "testDuration";
        long duration = 3600 * 1000;
        String durationS = String.valueOf(duration / 1000) + "s" ;
        OutputAnalyzer output = JcmdHelper.jcmd("JFR.start",
                "name=" + name,
                "duration=" + durationS);
        JcmdAsserts.assertRecordingHasStarted(output);
        JcmdHelper.waitUntilRunning(name);
        JcmdAsserts.assertDurationEqualsMBeanValue(name, duration);
        JcmdHelper.stopAndCheck(name);
    }

    private static void testDurationLessThan1s() throws Exception {
        String name = "testDurationLessThan1s";
        OutputAnalyzer output = JcmdHelper.jcmd("JFR.start",
                "name=" + name,
                "duration=10ms");
        JcmdAsserts.assertDurationAtLeast1s(output);
        JcmdHelper.checkAndAssertNoRecordingsAvailable();
    }

    /**
     * Check the maxage is the same as MBean value
     */
    private static void testMaxAge() throws Exception {
        String name = "testMaxAge";
        long maxAge = 2 * 1000;
        OutputAnalyzer output = JcmdHelper.jcmd("JFR.start",
                "name=" + name,
                "settings=" + SETTINGS.getAbsolutePath(),
                "maxage=2s");
        JcmdAsserts.assertRecordingHasStarted(output);
        JcmdHelper.waitUntilRunning(name);
        JcmdAsserts.assertMaxAgeEqualsMBeanValue(name, maxAge);
        JcmdHelper.stopAndCheck(name);
    }

    /**
     * Check the maxsize is the same as MBean value
     */
    private static void testMaxSize() throws Exception {
        String name = "testMaxSize";
        long maxSize = 2 * 1024 * 1024;
        OutputAnalyzer output = JcmdHelper.jcmd("JFR.start",
                "name=" + name,
                "settings=" + SETTINGS.getAbsolutePath(),
                "maxsize=" + maxSize);
        JcmdAsserts.assertRecordingHasStarted(output);
        JcmdHelper.waitUntilRunning(name);
        JcmdAsserts.assertMaxSizeEqualsMBeanValue(name, maxSize);
        JcmdHelper.stopAndCheck(name);
    }

}
