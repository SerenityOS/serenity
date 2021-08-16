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

import jdk.jfr.Configuration;
import jdk.management.jfr.ConfigurationInfo;
import jdk.test.lib.Asserts;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.TestConfigurationInfo
 */
public class TestConfigurationInfo {
    public static void main(String[] args) throws Exception {
        Map<String, Configuration> cc = new HashMap<>();
        for (Configuration c : Configuration.getConfigurations()) {
            cc.put(c.getName(), c);
        }
        Asserts.assertTrue(!cc.isEmpty(), "Mising configurations, can't verify ConfigurationInfo");
        for (ConfigurationInfo ci : JmxHelper.getFlighteRecorderMXBean().getConfigurations()) {
            Configuration c = cc.remove(ci.getName());
            Asserts.assertNotNull(c, "Superfluous configuration " + ci.getName());
            Asserts.assertEquals(c.getDescription(), ci.getDescription(), "Descriptions don't match");
            Asserts.assertEquals(c.getLabel(), ci.getLabel(), "Labels don't match");
            Asserts.assertEquals(c.getProvider(), ci.getProvider(), "Providers don't match");
            Asserts.assertEquals(c.getContents(), ci.getContents(), "Contents don't match");
            Asserts.assertEquals(c.getSettings(), ci.getSettings(), "Settings don't match");
        }
        Asserts.assertTrue(cc.isEmpty(), "Missing configuration in FlightRecorderMXBean, " + cc.keySet());
    }
}
