/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jmx.info;


import java.nio.file.Paths;
import java.time.Duration;

import jdk.jfr.jmx.JmxHelper;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import jdk.management.jfr.FlightRecorderMXBean;
import jdk.management.jfr.RecordingInfo;
import jdk.test.lib.jfr.CommonHelper;

/**
 * @test
 * @key jfr
 * @summary Test for RecordingInfo
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.info.TestRecordingInfo
 */
public class TestRecordingInfo {
    public static void main(String[] args) throws Throwable {
        Recording recording = new Recording(Configuration.getConfiguration("profile"));
        recording.setDestination(Paths.get(".", "my.jfr"));
        recording.setDumpOnExit(true);
        recording.setDuration(Duration.ofSeconds(60));
        recording.setMaxAge(Duration.ofHours(1));
        recording.setMaxSize(123456789);
        recording.setName("myName");
        recording.enable("java.exception_throw").with("threashold", "2 s");
        recording.setToDisk(true);

        recording.start();
        CommonHelper.verifyRecordingState(recording, RecordingState.RUNNING); // Wait until running

        FlightRecorderMXBean bean = JmxHelper.getFlighteRecorderMXBean();
        RecordingInfo info = JmxHelper.verifyExists(recording.getId(), bean.getRecordings());

        System.out.println(JmxHelper.asString(recording));
        System.out.println(JmxHelper.asString(info));
        JmxHelper.verifyEquals(info, recording);

        recording.stop();
        recording.close();
    }

}
