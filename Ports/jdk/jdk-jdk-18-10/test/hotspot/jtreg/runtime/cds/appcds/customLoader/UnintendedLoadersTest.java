/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @summary Make sure classes intended for custom loaders cannot be loaded by BOOT/EXT/APP loaders
 * @requires vm.cds
 * @requires vm.cds.custom.loaders
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile test-classes/UnintendedLoaders.java test-classes/CustomLoadee.java
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver UnintendedLoadersTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import sun.hotspot.WhiteBox;

public class UnintendedLoadersTest {
    public static void main(String[] args) throws Exception {
        String wbJar = JarBuilder.build(true, "WhiteBox", "sun/hotspot/WhiteBox");
        String use_whitebox_jar = "-Xbootclasspath/a:" + wbJar;

        String appJar = JarBuilder.build("UnintendedLoaders_app", "UnintendedLoaders");
        String customJarPath = JarBuilder.build("UnintendedLoaders_custom", "CustomLoadee");

        // Dump the archive
        String classlist[] = new String[] {
            "UnintendedLoadersTest",
            "java/lang/Object id: 1",

            // Without "loader:" keyword.
            "CustomLoadee id: 2 super: 1 source: " + customJarPath,
        };

        OutputAnalyzer output;
        TestCommon.testDump(appJar, classlist,
                            // command-line arguments ...
                            use_whitebox_jar);

        output = TestCommon.exec(appJar,
                                 // command-line arguments ...
                                 use_whitebox_jar,
                                 "-XX:+UnlockDiagnosticVMOptions",
                                 "-XX:+WhiteBoxAPI",
                                 "UnintendedLoaders");
        TestCommon.checkExec(output);
    }
}

