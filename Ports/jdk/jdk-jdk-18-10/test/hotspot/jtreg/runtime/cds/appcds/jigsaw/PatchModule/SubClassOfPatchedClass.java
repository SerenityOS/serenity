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
 * @summary the class in the -cp is a subclass of the class in --patch-module. The
 *          patched class should be used at runtime.
 * @library ../..
 * @library /test/hotspot/jtreg/testlibrary
 * @library /test/lib
 * @build PatchMain
 * @run driver SubClassOfPatchedClass
 */

import java.io.File;
import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class SubClassOfPatchedClass {
    private static String moduleJar;
    private static String appJar;

    public static void main(String args[]) throws Throwable {

        // Create a class file in the module java.naming. This class file
        // will be put in the javanaming.jar file.
        String source = "package javax.naming; "                +
                        "public class Reference { "             +
                        "    static { "                             +
                        "        System.out.println(\"I pass!\"); " +
                        "    } "                                    +
                        "}";

        String classDir = System.getProperty("test.classes");

        ClassFileInstaller.writeClassToDisk("javax/naming/Reference",
             InMemoryJavaCompiler.compile("javax.naming.Reference", source, "--patch-module=java.naming"),
             classDir);

        // Build the jar file that will be used for the module "java.naming".
        JarBuilder.build("javanaming", "javax/naming/Reference");
        moduleJar = TestCommon.getTestJar("javanaming.jar");

        String source2 = "package mypackage; "                +
                        "public class MyReference extends javax.naming.Reference { "             +
                        "    static { "                             +
                        "        System.out.println(\"MyReference!\"); " +
                        "    } "                                    +
                        "    public MyReference(String mystring) { " +
                        "        super(mystring); " +
                        "    } " +
                        "}";
        ClassFileInstaller.writeClassToDisk("mypackage/MyReference",
             InMemoryJavaCompiler.compile("mypackage.MyReference", source2),
             classDir);

        JarBuilder.build("myjavanaming", "mypackage/MyReference");
        appJar = TestCommon.getTestJar("myjavanaming.jar");

        System.out.println("Test dumping with --patch-module");
        OutputAnalyzer output =
            TestCommon.dump(appJar,
                TestCommon.list("javax/naming/Reference", "mypackage/MyReference"),
                "--patch-module=java.naming=" + moduleJar,
                "-Xlog:class+load",
                "PatchMain", "javax.naming.Reference", "mypackage.MyReference");
        output.shouldHaveExitValue(1)
              .shouldContain("Cannot use the following option when dumping the shared archive: --patch-module");

        String classPath = appJar + File.pathSeparator + classDir;
        System.out.println("classPath: " + classPath);
        TestCommon.run(
            "-XX:+UnlockDiagnosticVMOptions",
            "-cp", classPath,
            "--patch-module=java.naming=" + moduleJar,
            "-Xlog:class+load",
            "PatchMain", "javax.naming.Reference", "mypackage.MyReference")
            .assertSilentlyDisabledCDS(0, "MyReference source: file:", "I pass!");
    }
}
