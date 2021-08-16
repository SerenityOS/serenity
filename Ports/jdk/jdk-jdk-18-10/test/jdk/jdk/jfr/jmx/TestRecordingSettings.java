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

package jdk.jfr.jmx;

import java.util.HashMap;
import java.util.Map;

import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.management.jfr.FlightRecorderMXBean;
import jdk.test.lib.Asserts;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.TestRecordingSettings
 */
public class TestRecordingSettings {
    public static void main(String[] args) throws Exception {
        Map<String, String> settings = new HashMap<>();
        settings.put("java.exception_throw#enabled", "false");
        settings.put("java.exception_throw#threshold", "2 s");
        settings.put("java.exception_throw#thread", "true");
        settings.put("java.exception_throw#stackTrace", "false");
        settings.put("os.information#enabled", "true");
        settings.put("os.information#period", "400 ms");

        FlightRecorderMXBean bean = JmxHelper.getFlighteRecorderMXBean();
        long recId = bean.newRecording();
        bean.setRecordingSettings(recId, settings);

        // Verify that JMX input and output settings are equal.
        JmxHelper.verifyMapEquals(settings, JmxHelper.getFlighteRecorderMXBean().getRecordingSettings(recId));

        // Verify that settings from Java API is correct.
        Recording recording = null;
        for (Recording r :  FlightRecorder.getFlightRecorder().getRecordings()) {
            if (r.getId() == recId) {
                recording = r;
                break;
            }
        }
        Asserts.assertNotNull(recording, "No Recording with id " + recId);
        JmxHelper.verifyMapEquals(settings, recording.getSettings());

        bean.startRecording(recId);
        bean.stopRecording(recId);
        bean.closeRecording(recId);
    }

}
