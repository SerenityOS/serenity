/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 *
 * @library /test/lib
 *
 * @run main jdk.jfr.startupargs.TestMultipleStartupRecordings
 */
public class TestMultipleStartupRecordings {

    private static final String START_FLIGHT_RECORDING = "-XX:StartFlightRecording";
    private static final String FLIGHT_RECORDER_OPTIONS = "-XX:FlightRecorderOptions";

    static class MainClass {
        public static void main(String[] args) {
        }
    }

    private static void test(ProcessBuilder pb, String... expectedOutputs) throws Exception {
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        for (String s : expectedOutputs) {
            output.shouldContain(s);
        }
    }

    private static void launchUnary(String options) throws Exception {
        String recording1 = START_FLIGHT_RECORDING + (options != null ? options : "");
        ProcessBuilder pb = ProcessTools.createTestJvm(recording1, MainClass.class.getName());
        test(pb, "Started recording 1");
    }

    private static void launchBinary(String options1, String options2) throws Exception {
        String recording1 = START_FLIGHT_RECORDING + (options1 != null ? options1 : "");
        String recording2 = START_FLIGHT_RECORDING + (options2 != null ? options2 : "");
        ProcessBuilder pb = ProcessTools.createTestJvm(recording1, recording2, MainClass.class.getName());
        test(pb, "Started recording 1", "Started recording 2");
    }

    private static void launchTernary(String options1, String options2, String options3) throws Exception {
        String recording1 = START_FLIGHT_RECORDING + (options1 != null ? options1 : "");
        String recording2 = START_FLIGHT_RECORDING + (options2 != null ? options2 : "");
        String recording3 = START_FLIGHT_RECORDING + (options3 != null ? options3 : "");
        ProcessBuilder pb = ProcessTools.createTestJvm(recording1, recording2, recording3, MainClass.class.getName());
        test(pb, "Started recording 1", "Started recording 2", "Started recording 3");
    }

    private static void testDefault() throws Exception {
        System.out.println("testDefault");
        launchUnary(null);
        launchBinary(null, null);
        launchTernary(null, null, null);
    }

    private static void testColonDelimited() throws Exception {
        launchBinary(":name=myrecording1,filename=myrecording1.jfr", ":filename=myrecording2.jfr,name=myrecording2");
    }

    private static void testMixed() throws Exception {
        launchTernary(":maxage=2d,maxsize=5GB", "=dumponexit=true,maxage=10m,", ":name=myrecording,maxage=10m,filename=myrecording.jfr,disk=false");
    }

    private static void testWithFlightRecorderOptions() throws Exception {
        System.out.println("testWithFlightRecorderOptions");
        String flightRecorderOptions = FLIGHT_RECORDER_OPTIONS + "=maxchunksize=8m";
        String recording1 = START_FLIGHT_RECORDING + "=filename=recording1.jfr";
        String recording2 = START_FLIGHT_RECORDING + "=name=myrecording,filename=recording2.jfr";
        ProcessBuilder pb = ProcessTools.createTestJvm(flightRecorderOptions, recording1, recording2, MainClass.class.getName());
        test(pb, "Started recording 1", "Started recording 2");
    }

    public static void main(String[] args) throws Exception {
        testDefault();
        testColonDelimited();
        testMixed();
        testWithFlightRecorderOptions();
    }
}
