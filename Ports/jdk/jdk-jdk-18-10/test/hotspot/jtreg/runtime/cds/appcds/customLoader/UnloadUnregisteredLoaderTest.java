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
 * @summary Test the behavior when shared classes loaded by custom loaders are
 *          unloaded.
 * @requires vm.cds
 * @requires vm.cds.custom.loaders
 * @requires vm.opt.final.ClassUnloading
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @build sun.hotspot.WhiteBox jdk.test.lib.classloader.ClassUnloadCommon
 * @compile test-classes/UnloadUnregisteredLoader.java test-classes/CustomLoadee.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *                 jdk.test.lib.classloader.ClassUnloadCommon
 *                 jdk.test.lib.classloader.ClassUnloadCommon$1
 *                 jdk.test.lib.classloader.ClassUnloadCommon$TestFailure
 * @run driver UnloadUnregisteredLoaderTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import sun.hotspot.WhiteBox;

public class UnloadUnregisteredLoaderTest {
    public static void main(String[] args) throws Exception {
        String appJar1 = JarBuilder.build("UnloadUnregisteredLoader_app1", "UnloadUnregisteredLoader");
        String appJar2 = JarBuilder.build(true, "UnloadUnregisteredLoader_app2",
                                          "jdk/test/lib/classloader/ClassUnloadCommon",
                                          "jdk/test/lib/classloader/ClassUnloadCommon$1",
                                          "jdk/test/lib/classloader/ClassUnloadCommon$TestFailure");
        String customJarPath = JarBuilder.build("UnloadUnregisteredLoader_custom", "CustomLoadee");
        String wbJar = JarBuilder.build(true, "WhiteBox", "sun/hotspot/WhiteBox");
        String use_whitebox_jar = "-Xbootclasspath/a:" + wbJar;

        String classpath = TestCommon.concatPaths(appJar1, appJar2);
        String classlist[] = new String[] {
            "UnloadUnregisteredLoader",
            "jdk/test/lib/classloader/ClassUnloadCommon",
            "jdk/test/lib/classloader/ClassUnloadCommon$1",
            "jdk/test/lib/classloader/ClassUnloadCommon$TestFailure",
            "java/lang/Object id: 1",
            "CustomLoadee id: 2 super: 1 source: " + customJarPath,
        };

        OutputAnalyzer output;
        TestCommon.testDump(classpath, classlist,
                            // command-line arguments ...
                            use_whitebox_jar);

        output = TestCommon.exec(classpath,
                                 // command-line arguments ...
                                 use_whitebox_jar,
                                 "-XX:+UnlockDiagnosticVMOptions",
                                 "-XX:+WhiteBoxAPI",
                                 "UnloadUnregisteredLoader",
                                 customJarPath);
        TestCommon.checkExec(output);
    }
}
