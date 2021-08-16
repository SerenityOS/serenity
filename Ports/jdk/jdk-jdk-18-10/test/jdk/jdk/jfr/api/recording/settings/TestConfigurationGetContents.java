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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import jdk.jfr.Configuration;
import jdk.test.lib.Asserts;

/**
 * @test
 * @summary Verifies Configuration.getContents() for every configuration
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.settings.TestConfigurationGetContents
 */
public class TestConfigurationGetContents {

    private static final String SEP = System.getProperty("file.separator");
    private static final String JFR_DIR = System.getProperty("test.jdk")
            + SEP + "lib" + SEP + "jfr" + SEP;

    public static void main(String[] args) throws Throwable {
        List<Configuration> predefinedConfigs = Configuration.getConfigurations();

        Asserts.assertNotNull(predefinedConfigs, "List of predefined configs is null");
        Asserts.assertTrue(predefinedConfigs.size() > 0, "List of predefined configs is empty");

        for (Configuration conf : predefinedConfigs) {
            String name = conf.getName();
            System.out.println("Verifying configuration " + name);
            String fpath = JFR_DIR + name + ".jfc";
            String contents = conf.getContents();
            String fileContents = readFile(fpath);
            Asserts.assertEquals(fileContents, contents, "getContents() does not return the actual contents of the file " + fpath);
        }
    }

    private static String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded);
    }

}
