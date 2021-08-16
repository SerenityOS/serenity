/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.test.lib.jfr.EventNames;


/**
 * @test
 * @summary Start a recording with or without path-to-gc-roots
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @key jfr
 *
 * @run main/othervm jdk.jfr.jcmd.TestJcmdStartPathToGCRoots
 */
public class TestJcmdStartPathToGCRoots {

    public static void main(String[] args) throws Exception {

        JcmdHelper.jcmd("JFR.start", "path-to-gc-roots=true");
        assertCutoff("infinity", "Expected cutoff to be '0 ns' wuth -XX:StartFlightRecording:path-to-gc-roots=true");
        closeRecording();

        JcmdHelper.jcmd("JFR.start", "path-to-gc-roots=false");
        assertCutoff("0 ns", "Expected cutoff to be '0 ns' with -XX:StartFlightRecording:path-to-gc-roots=false");
        closeRecording();

        JcmdHelper.jcmd("JFR.start");
        assertCutoff("0 ns", "Expected cutoff to be '0 ns' with -XX:StartFlightRecording:");
        closeRecording();
    }

    private static void assertCutoff(String expected, String errorMessage) throws Exception {
        List<Recording> recordings = FlightRecorder.getFlightRecorder().getRecordings();
        if (recordings.isEmpty()) {
            throw new Exception("Expected recording to be started");
        }
        if (recordings.size() != 1) {
            throw new Exception("Expected only one recording");
        }

        String settingName = EventNames.OldObjectSample + "#" + "cutoff";
        Recording r = recordings.get(0);
        String cutoff = r.getSettings().get(settingName);
        System.out.println(settingName + "=" + cutoff);
        if (!expected.equals(cutoff)) {
            throw new Exception(errorMessage);
        }
        r.close();
    }

    private static void closeRecording() {
        for (Recording r : FlightRecorder.getFlightRecorder().getRecordings()) {
            r.close();
        }
    }

}
