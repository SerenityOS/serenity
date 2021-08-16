/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.api.recording.settings;

import java.util.List;

import jdk.jfr.Configuration;
import jdk.test.lib.Asserts;

/**
 * @test
 * @summary Verifies that there is the default config and that it has
 *          the expected parameters
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.settings.TestGetConfigurations
 */
public class TestGetConfigurations {

    private static final String DEFAULT_CONFIG_NAME = "default";
    private static final String DEFAULT_CONFIG_LABEL = "Continuous";
    private static final String DEFAULT_CONFIG_DESCRIPTION = "Low overhead configuration safe for continuous use in production environments, typically less than 1 % overhead.";
    private static final String DEFAULT_CONFIG_PROVIDER = "Oracle";

    private static final String PROFILE_CONFIG_NAME = "profile";
    private static final String PROFILE_CONFIG_LABEL = "Profiling";
    private static final String PROFILE_CONFIG_DESCRIPTION = "Low overhead configuration for profiling, typically around 2 % overhead.";
    private static final String PROFILE_CONFIG_PROVIDER = "Oracle";

    public static void main(String[] args) throws Throwable {
        List<Configuration> predefinedConfigs = Configuration.getConfigurations();
        Asserts.assertNotNull(predefinedConfigs, "List of predefined configs is null");
        Asserts.assertEquals(predefinedConfigs.size(), 2, "Expected exactly two predefined configurations");

        Configuration defaultConfig = findConfigByName(predefinedConfigs, DEFAULT_CONFIG_NAME);
        Asserts.assertNotNull(defaultConfig, "Config '" + DEFAULT_CONFIG_NAME + "' not found");
        Asserts.assertEquals(defaultConfig.getLabel(), DEFAULT_CONFIG_LABEL);
        Asserts.assertEquals(defaultConfig.getDescription(), DEFAULT_CONFIG_DESCRIPTION);
        Asserts.assertEquals(defaultConfig.getProvider(), DEFAULT_CONFIG_PROVIDER);

        Configuration profileConfig = findConfigByName(predefinedConfigs, PROFILE_CONFIG_NAME);
        Asserts.assertNotNull(profileConfig, "Config '" + PROFILE_CONFIG_NAME + "' not found");
        Asserts.assertEquals(profileConfig.getLabel(), PROFILE_CONFIG_LABEL);
        Asserts.assertEquals(profileConfig.getDescription(), PROFILE_CONFIG_DESCRIPTION);
        Asserts.assertEquals(profileConfig.getProvider(), PROFILE_CONFIG_PROVIDER);
    }

    private static Configuration findConfigByName(List<Configuration> configs, String name) {
        for (Configuration config : configs) {
            if (name.equals(config.getName())) {
                return config;
            }
        }
        return null;
    }

}
