/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @requires vm.cds
 * @summary different settings of --patch-module at dump time and runtime are
 *          acceptable. The class found in runtime --patch-module entry should
 *          be used.
 * @library ../..
 * @library /test/hotspot/jtreg/testlibrary
 * @library /test/lib
 * @build PatchMain
 * @run driver MismatchedPatchModule
 */

import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class MismatchedPatchModule {
    private static String moduleJar;

    public static void main(String args[]) throws Throwable {

        // Create a class file in the module java.naming. This class file
        // will be put in the javanaming.jar file.
        String source = "package javax.naming.spi; "                +
                        "public class NamingManager { "             +
                        "    static { "                             +
                        "        System.out.println(\"I pass!\"); " +
                        "    } "                                    +
                        "}";

        ClassFileInstaller.writeClassToDisk("javax/naming/spi/NamingManager",
             InMemoryJavaCompiler.compile("javax.naming.spi.NamingManager", source, "--patch-module=java.naming"),
             System.getProperty("test.classes"));

        // Build the jar file that will be used for the module "java.naming".
        JarBuilder.build("javanaming", "javax/naming/spi/NamingManager");
        moduleJar = TestCommon.getTestJar("javanaming.jar");

        // Case 1: --patch-module specified for dump time
        System.out.println("Case 1: --patch-module specified for dump time and run time");
        OutputAnalyzer output =
            TestCommon.dump(null,
                TestCommon.list("javax/naming/spi/NamingManager"),
                "--patch-module=java.naming=" + moduleJar,
                "PatchMain", "javax.naming.spi.NamingManager");
        output.shouldHaveExitValue(1)
              .shouldContain("Cannot use the following option when dumping the shared archive: --patch-module");

        // Case 2: --patch-module specified for run time but not for dump time
        System.out.println("Case 2: --patch-module specified for run time but not for dump time");
        String appJar = JarBuilder.build("PatchMain-app", "PatchMain");
        output =
            TestCommon.dump(appJar,
                TestCommon.list("javax/naming/spi/NamingManager"),
                "PatchMain", "javax.naming.spi.NamingManager");
        TestCommon.checkDump(output);

        // javax.naming.spi.NamingManager is patched at runtime
        TestCommon.run(
            "-XX:+UnlockDiagnosticVMOptions",
            "--patch-module=java.naming=" + moduleJar,
            "--patch-module=java.naming2=" + moduleJar,
            "-Xlog:class+path=info",
            "PatchMain", "javax.naming.spi.NamingManager")
            .assertSilentlyDisabledCDS(0, "I pass!");
    }
}
