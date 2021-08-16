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
 * @summary Check that during dumping, the classes for BOOT/EXT/APP loaders are segregated from the
 *          custom loader classes.
 * @requires vm.cds
 * @requires vm.cds.custom.loaders
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile test-classes/LoaderSegregation.java
 *          test-classes/CustomLoadee.java test-classes/CustomLoadee2.java
 *          test-classes/CustomInterface2_ia.java test-classes/CustomInterface2_ib.java
 *          test-classes/CustomLoadee3.java test-classes/CustomLoadee3Child.java
 *          test-classes/OnlyBuiltin.java
 *          test-classes/OnlyUnregistered.java
 *          ../test-classes/Util.java
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver LoaderSegregationTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import sun.hotspot.WhiteBox;

/**
 * See "Handling of the classes in the AppCDS archive" at the top of
 * systemDicrionatyShared.hpp.
 *
 * This test ensure that the 2 types of archived classes (BUILTIN and UNREGISTERED)
 * are segregated at both dump-time and run time:
 *
 * [A] An archived BUILTIN class cannot be a subclass of a non-BUILTIN class.
 * [B] An archived BUILTIN class cannot implement a non-BUILTIN interface.
 * [C] BUILTIN and UNREGISTERED classes can be loaded only by their corresponding
 *     type of loaders.
 *
 */
public class LoaderSegregationTest {
    public static void main(String[] args) throws Exception {
        String wbJar = JarBuilder.build(true, "WhiteBox", "sun/hotspot/WhiteBox");
        String use_whitebox_jar = "-Xbootclasspath/a:" + wbJar;

        String appJar = JarBuilder.build("LoaderSegregation_app", "LoaderSegregation", "LoaderSegregation$1",
                                         "CustomLoadee", "CustomLoadee2", "CustomLoadee3Child", "CustomInterface2_ia",
                                         "OnlyBuiltin", "Util");

        String app2Jar = JarBuilder.build("LoaderSegregation_app2", "CustomLoadee3", "CustomInterface2_ib");

        String customJarPath = JarBuilder.build("LoaderSegregation_custom", "CustomLoadee",
                                                "CustomLoadee2", "CustomInterface2_ia", "CustomInterface2_ib",
                                                "CustomLoadee3", "CustomLoadee3Child",
                                                "OnlyBuiltin", "OnlyUnregistered");

        // Dump the archive
        String classlist[] = new String[] {
            "LoaderSegregation",
            "java/lang/Object id: 1",

            // These are the UNREGISTERED classes: they have "source:"
            // but they don't have "loader:".
            "CustomLoadee id: 2 super: 1 source: " + customJarPath,

            "CustomInterface2_ia id: 3 super: 1 source: " + customJarPath,
            "CustomInterface2_ib id: 4 super: 1 source: " + customJarPath,
            "CustomLoadee2 id: 5 super: 1 interfaces: 3 4 source: " + customJarPath,

            "CustomLoadee3 id: 6 super: 1 source: " + customJarPath,
            "CustomLoadee3Child id: 7 super: 6 source: " + customJarPath,

            // At dump time, the following BUILTIN classes are loaded after the UNREGISTERED
            // classes from above. However, at dump time, they cannot use the UNREGISTERED classes are their
            // super or interface.
            "CustomLoadee",          // can be loaded at dump time
            "CustomLoadee2",         // cannot be loaded at dump time (interface missing)
            "CustomLoadee3Child",    // cannot be loaded at dump time (super missing)

            // Check that BUILTIN and UNREGISTERED classes can be loaded only by their
            // corresponding type of loaders.
            "OnlyBuiltin",
            "OnlyUnregistered id: 9 super: 1 source: " + customJarPath,
        };

        OutputAnalyzer output;
        TestCommon.testDump(appJar, classlist,
                            // command-line arguments ...
                            use_whitebox_jar);

        output = TestCommon.exec(TestCommon.concatPaths(appJar, app2Jar),
                                 // command-line arguments ...
                                 use_whitebox_jar,
                                 "-XX:+UnlockDiagnosticVMOptions",
                                 "-XX:+WhiteBoxAPI",
                                 "LoaderSegregation", customJarPath);
        TestCommon.checkExec(output);
    }
}
