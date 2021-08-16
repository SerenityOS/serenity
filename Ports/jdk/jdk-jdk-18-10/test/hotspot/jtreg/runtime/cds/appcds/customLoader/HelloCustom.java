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
 * @summary Hello World test for AppCDS custom loader support
 * @requires vm.cds
 * @requires vm.cds.custom.loaders
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile test-classes/HelloUnload.java test-classes/CustomLoadee.java
 * @build sun.hotspot.WhiteBox jdk.test.lib.classloader.ClassUnloadCommon
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar hello.jar HelloUnload
 *                 jdk.test.lib.classloader.ClassUnloadCommon
 *                 jdk.test.lib.classloader.ClassUnloadCommon$1
 *                 jdk.test.lib.classloader.ClassUnloadCommon$TestFailure
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar hello_custom.jar CustomLoadee
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar WhiteBox.jar sun.hotspot.WhiteBox
 * @run driver HelloCustom
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;
import sun.hotspot.WhiteBox;

public class HelloCustom {
    public static void main(String[] args) throws Exception {
        run();
    }
    public static void run(String... extra_runtime_args) throws Exception {
        String wbJar = ClassFileInstaller.getJarPath("WhiteBox.jar");
        String use_whitebox_jar = "-Xbootclasspath/a:" + wbJar;

        String appJar = ClassFileInstaller.getJarPath("hello.jar");
        String customJarPath = ClassFileInstaller.getJarPath("hello_custom.jar");

        // Dump the archive
        String classlist[] = new String[] {
            "HelloUnload",
            "java/lang/Object id: 1",
            "CustomLoadee id: 2 super: 1 source: " + customJarPath
        };

        OutputAnalyzer output;
        TestCommon.testDump(appJar, classlist,
                            // command-line arguments ...
                            use_whitebox_jar);

        output = TestCommon.exec(appJar,
                                 TestCommon.concat(extra_runtime_args,
                                     // command-line arguments ...
                                     use_whitebox_jar,
                                     "-XX:+UnlockDiagnosticVMOptions",
                                     "-XX:+WhiteBoxAPI",
                                     "HelloUnload", customJarPath, "true", "true"));
        TestCommon.checkExec(output);
    }
}
