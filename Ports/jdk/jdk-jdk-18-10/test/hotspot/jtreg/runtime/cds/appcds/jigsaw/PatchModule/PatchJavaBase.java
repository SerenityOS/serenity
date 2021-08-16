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
 * @summary sharing is disabled if java.base is patch at runtime
 * @library ../..
 * @library /test/hotspot/jtreg/testlibrary
 * @library /test/lib
 * @build PatchMain
 * @run driver PatchJavaBase
 */

import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class PatchJavaBase {
    private static String moduleJar;

    public static void main(String args[]) throws Throwable {

        String source = "package java.lang; "                       +
                        "public class NewClass { "                  +
                        "    static { "                             +
                        "        System.out.println(\"I pass!\"); " +
                        "    } "                                    +
                        "}";

        ClassFileInstaller.writeClassToDisk("java/lang/NewClass",
             InMemoryJavaCompiler.compile("java.lang.NewClass", source, "--patch-module=java.base"),
             System.getProperty("test.classes"));

        JarBuilder.build("javabase", "java/lang/NewClass");
        moduleJar = TestCommon.getTestJar("javabase.jar");

        System.out.println("Test dumping with --patch-module");
        String runError = "Unable to use shared archive: CDS is disabled when java.base module is patched";
        String dumpingError = "Cannot use the following option when dumping the shared archive: --patch-module";
        String errMsg;
        if (TestCommon.isDynamicArchive()) {
            errMsg = runError;
        } else {
            errMsg = dumpingError;
        }
        OutputAnalyzer output =
            TestCommon.dump(null, null,
                "--patch-module=java.base=" + moduleJar,
                "PatchMain", "java.lang.NewClass");
        output.shouldHaveExitValue(1)
              .shouldContain(errMsg);

        TestCommon.run(
            "-XX:+UnlockDiagnosticVMOptions",
            "--patch-module=java.base=" + moduleJar,
            "PatchMain", "java.lang.NewClass")
          .assertAbnormalExit(runError);
    }
}
