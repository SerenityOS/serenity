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

import jdk.jfr.Recording;
import jdk.management.jfr.FlightRecorderMXBean;
import jdk.management.jfr.RecordingInfo;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.TestSetConfiguration
 */
public class TestSetConfiguration {
    public static void main(String[] args) throws Exception {
        FlightRecorderMXBean bean = JmxHelper.getFlighteRecorderMXBean();
        long recId = bean.newRecording();

        final String configContents =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
        "<configuration version=\"2.0\" label=\"TestName\" description='TestDesc' provider='TestProvider'>\n" +
        "  <event name=\"" + EventNames.ClassLoad +"\">\r" +
        " \t <setting name=\"enabled\" control='class-loading-enabled'>false</setting>\r\n" +
        "    <setting name=\"stackTrace\">true</setting>\t\r\n" +
        "    <setting name=\"threshold\">5 ms</setting> \n" +
        "  </event>  " +
        "  <control>  " +
        "    <flag name=\"class-loading-enabled\" label=\"Class Loading\">false</flag>\n" +
        "  </control>" +
        "</configuration>";

        Map<String, String> expectedSetting = new HashMap<>();
        expectedSetting.put(EventNames.ClassLoad + "#enabled", "false");
        expectedSetting.put(EventNames.ClassLoad + "#stackTrace", "true");
        expectedSetting.put(EventNames.ClassLoad + "#threshold", "5 ms");

        bean.setConfiguration(recId, configContents);
        RecordingInfo jmxRecording = JmxHelper.getJmxRecording(recId);
        Recording javaRecording = JmxHelper.getJavaRecording(recId);
        JmxHelper.verifyEquals(jmxRecording, javaRecording);

        Map<String, String> settings = jmxRecording.getSettings();
        for (String name : expectedSetting.keySet()) {
            String value = settings.remove(name);
            Asserts.assertNotNull(value, "No setting with name " + name);
            Asserts.assertEquals(value, expectedSetting.get(name), "Wrong setting value");
        }
        Asserts.assertTrue(settings.isEmpty(), "Extra settings found " + settings.keySet());
    }
}
