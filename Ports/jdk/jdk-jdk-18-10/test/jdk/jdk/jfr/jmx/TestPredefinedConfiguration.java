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

import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Configuration;
import jdk.management.jfr.FlightRecorderMXBean;
import jdk.management.jfr.RecordingInfo;
import jdk.test.lib.Asserts;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.TestPredefinedConfiguration
 */
public class TestPredefinedConfiguration {
    public static void main(String[] args) throws Exception {
        FlightRecorderMXBean bean = JmxHelper.getFlighteRecorderMXBean();

        long recId = bean.newRecording();
        List<String> configNames = new ArrayList<>();
        for (Configuration config : Configuration.getConfigurations()) {
            System.out.println("name=" + config.getName());
            configNames.add(config.getName());
            bean.setPredefinedConfiguration(recId, config.getName());

            RecordingInfo jmxRecording = JmxHelper.getJmxRecording(recId);
            JmxHelper.verifyMapEquals(jmxRecording.getSettings(), config.getSettings());
            JmxHelper.verifyMapEquals(bean.getRecordingSettings(recId), config.getSettings());
        }
        Asserts.assertTrue(configNames.contains("default"), "Missing config 'default'");
        Asserts.assertTrue(configNames.contains("profile"), "Missing config 'profile'");
    }
}
