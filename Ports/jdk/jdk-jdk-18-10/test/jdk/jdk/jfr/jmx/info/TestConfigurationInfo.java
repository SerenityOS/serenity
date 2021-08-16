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


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.jfr.jmx.JmxHelper;

import jdk.jfr.Configuration;
import jdk.management.jfr.ConfigurationInfo;
import jdk.test.lib.Asserts;

/**
 * @test
 * @key jfr
 * @summary Test for ConfigurationInfo. Compare infos from java API and jmx API.
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.info.TestConfigurationInfo
 */
public class TestConfigurationInfo {
    public static void main(String[] args) throws Throwable {
        List<ConfigurationInfo> configInfos = JmxHelper.getFlighteRecorderMXBean().getConfigurations();
        Asserts.assertFalse(configInfos.isEmpty(), "No ConfigurationInfos found");

        Map<String, Configuration> configs = new HashMap<>();
        for (Configuration config : Configuration.getConfigurations()) {
            configs.put(config.getName(), config);
        }
        Asserts.assertFalse(configs.isEmpty(), "No Configurations found");

        for (ConfigurationInfo configInfo : configInfos) {
            final String key = configInfo.getName();
            Configuration config = configs.remove(key);
            Asserts.assertNotNull(config, "No Configuration for name " + key);

            System.out.println("getDescription:" + configInfo.getDescription());
            System.out.println("getLabel:" + configInfo.getLabel());
            System.out.println("getName:" + configInfo.getName());
            System.out.println("getProvider:" + configInfo.getProvider());

            Asserts.assertEquals(configInfo.getContents(), config.getContents(), "Wrong contents");
            Asserts.assertEquals(configInfo.getDescription(), config.getDescription(), "Wrong description");
            Asserts.assertEquals(configInfo.getLabel(), config.getLabel(), "Wrong label");
            Asserts.assertEquals(configInfo.getName(), config.getName(), "Wrong name");
            Asserts.assertEquals(configInfo.getProvider(), config.getProvider(), "Wrong provider");

            verifySettingsEqual(config, configInfo);
        }

        // Verify that all EventTypes have been matched.
        if (!configs.isEmpty()) {
            for (String name : configs.keySet()) {
                System.out.println("Found extra Configuration with name " + name);
            }
            Asserts.fail("Found extra Configuration");
        }
    }

    private static void verifySettingsEqual(Configuration config, ConfigurationInfo configInfo) {
        Map<String, String> javaSettings = config.getSettings();
        Map<String, String> jmxSettings = configInfo.getSettings();

        Asserts.assertFalse(javaSettings.isEmpty(), "No Settings found in java apa");
        Asserts.assertFalse(jmxSettings.isEmpty(), "No Settings found in jmx api");

        for (String name : jmxSettings.keySet().toArray(new String[0])) {
            System.out.printf("%s: jmx=%s, java=%s%n", name, jmxSettings.get(name), javaSettings.get(name));
            Asserts.assertNotNull(javaSettings.get(name), "No java setting for " + name);
            Asserts.assertEquals(jmxSettings.get(name), javaSettings.get(name), "Wrong value for setting");
            javaSettings.remove(name);
        }

        // Verify that all Settings have been matched.
        if (!javaSettings.isEmpty()) {
            for (String name : javaSettings.keySet()) {
                System.out.println("Found extra Settings name " + name);
            }
            Asserts.fail("Found extra Setting in java api");
        }
    }

}
