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
import jdk.test.lib.Asserts;

/**
 * @test
 * @key jfr
 * @summary Verify exception when setting invalid settings.
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.TestRecordingSettingsInvalid
 */
public class TestRecordingSettingsInvalid {
    public static void main(String[] args) throws Exception {
        Map<String, String> settings = new HashMap<>();
        settings.put(null, "true");
        settings.put("java.exception_throw#stackTrace", null);
        settings.put("java.exception_throw#threshold", "not-a-number");
        settings.put("os.information#period", "4 x");

        // TODO: No exception for these settings. Not sure how much validation can be done on settings.
        //settings.put("java.exception_throw#enabled", "maybe");
        //settings.put("os.information#period", "-4 s");
        //settings.put("java.exception_throw#thread", "");
        //settings.put("", "true");
        //settings.put("os.information#what", "4 ms");
        //settings.put("#", "4 what");
        //settings.put("java.exception_throw#", "true");
        //settings.put("java.exception_throwenabled", "false");

        FlightRecorderMXBean bean = JmxHelper.getFlighteRecorderMXBean();

        for (String key : settings.keySet()) {
            System.out.printf("settings: %s=%s%n", key, settings.get(key));
            Map<String, String> temp = new HashMap<String, String>();
            temp.put(key, settings.get(key));
            long recId = -1;
            try {
                recId = bean.newRecording();
                bean.setRecordingSettings(recId, temp);
                bean.startRecording(recId);
                bean.stopRecording(recId);
                Asserts.fail("Missing exception");
            } catch (Exception e) {
                System.out.println("Got expected exception: " + e.getMessage());
            } finally {
                bean.closeRecording(recId);
            }
        }
    }

}
