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

import jdk.management.jfr.FlightRecorderMXBean;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.TestRecordingSettingsMultiple
 */
public class TestRecordingSettingsMultiple {
    public static void main(String[] args) throws Exception {
        Map<String, String> settingsA = new HashMap<>();
        settingsA.put("java.exception_throw#enabled", "false");
        settingsA.put("java.exception_throw#threshold", "2 s");
        settingsA.put("java.exception_throw#thread", "true");
        settingsA.put("java.exception_throw#stackTrace", "false");
        settingsA.put("os.information#enabled", "true");
        settingsA.put("os.information#period", "400 ms");

        Map<String, String> settingsB = new HashMap<>();
        settingsB.put("vm/code_sweeper/config#enabled", "true");
        settingsB.put("vm/code_sweeper/config#period", "everyChunk");
        settingsA.put("java.exception_throw#enabled", "true");
        settingsA.put("java.exception_throw#threshold", "6 m");
        settingsB.put("os.information#enabled", "true");
        settingsB.put("os.information#period", "0 ms");

        FlightRecorderMXBean bean = JmxHelper.getFlighteRecorderMXBean();
        long recIdA = bean.newRecording();
        long recIdB = bean.newRecording();
        bean.setRecordingSettings(recIdA, settingsA);
        bean.setRecordingSettings(recIdB, settingsB);

        JmxHelper.verifyMapEquals(settingsA, bean.getRecordingSettings(recIdA));
        JmxHelper.verifyMapEquals(settingsB, bean.getRecordingSettings(recIdB));
        JmxHelper.verifyMapEquals(settingsA, JmxHelper.getJavaRecording(recIdA).getSettings());
        JmxHelper.verifyMapEquals(settingsB, JmxHelper.getJavaRecording(recIdB).getSettings());

        bean.startRecording(recIdA);
        bean.startRecording(recIdB);
        JmxHelper.verifyMapEquals(settingsA, bean.getRecordingSettings(recIdA));
        JmxHelper.verifyMapEquals(settingsB, bean.getRecordingSettings(recIdB));
        JmxHelper.verifyMapEquals(settingsA, JmxHelper.getJavaRecording(recIdA).getSettings());
        JmxHelper.verifyMapEquals(settingsB, JmxHelper.getJavaRecording(recIdB).getSettings());

        bean.stopRecording(recIdA);
        bean.stopRecording(recIdB);
        JmxHelper.verifyMapEquals(settingsA, bean.getRecordingSettings(recIdA));
        JmxHelper.verifyMapEquals(settingsB, bean.getRecordingSettings(recIdB));
        JmxHelper.verifyMapEquals(settingsA, JmxHelper.getJavaRecording(recIdA).getSettings());
        JmxHelper.verifyMapEquals(settingsB, JmxHelper.getJavaRecording(recIdB).getSettings());

        bean.closeRecording(recIdA);
        bean.closeRecording(recIdB);
    }

}
