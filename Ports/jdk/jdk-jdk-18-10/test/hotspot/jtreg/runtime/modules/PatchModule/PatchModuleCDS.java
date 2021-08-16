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
 */

/**
 * @test
 * @requires vm.cds
 * @summary test that --patch-module works with CDS
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          jdk.jartool/sun.tools.jar
 * @build PatchModuleMain
 * @run driver PatchModuleCDS
 */

import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.helpers.ClassFileInstaller;

public class PatchModuleCDS {

    public static void main(String args[]) throws Throwable {

        // Case 1: Test that --patch-module and -Xshare:dump are compatible
        String filename = "patch_module.jsa";
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:SharedArchiveFile=" + filename,
            "-Xshare:dump",
            "--patch-module=java.naming=no/such/directory",
            "-Xlog:class+path=info",
            "-version");
        new OutputAnalyzer(pb.start())
            // --patch-module is not supported during CDS dumping
            .shouldContain("Cannot use the following option when dumping the shared archive: --patch-module");

        // Case 2: Test that directory in --patch-module is supported for CDS dumping
        // Create a class file in the module java.base.
        String source = "package javax.naming.spi; "                +
                        "public class NamingManager { "             +
                        "    static { "                             +
                        "        System.out.println(\"I pass!\"); " +
                        "    } "                                    +
                        "}";

        ClassFileInstaller.writeClassToDisk("javax/naming/spi/NamingManager",
             InMemoryJavaCompiler.compile("javax.naming.spi.NamingManager", source, "--patch-module=java.naming"),
             System.getProperty("test.classes"));

        pb = ProcessTools.createJavaProcessBuilder(
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:SharedArchiveFile=" + filename,
            "-Xshare:dump",
            "--patch-module=java.naming=" + System.getProperty("test.classes"),
            "-Xlog:class+path=info",
            "-version");
        new OutputAnalyzer(pb.start())
            // --patch-module is not supported during CDS dumping
            .shouldContain("Cannot use the following option when dumping the shared archive: --patch-module");

        // Case 3a: Test CDS dumping with jar file in --patch-module
        BasicJarBuilder.build("javanaming", "javax/naming/spi/NamingManager");
        String moduleJar = BasicJarBuilder.getTestJar("javanaming.jar");
        pb = ProcessTools.createJavaProcessBuilder(
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:SharedArchiveFile=" + filename,
            "-Xshare:dump",
            "--patch-module=java.naming=" + moduleJar,
            "-Xlog:class+load",
            "-Xlog:class+path=info",
            "PatchModuleMain", "javax.naming.spi.NamingManager");
        new OutputAnalyzer(pb.start())
            // --patch-module is not supported during CDS dumping
            .shouldContain("Cannot use the following option when dumping the shared archive: --patch-module");

        // Case 3b: Test CDS run with jar file in --patch-module
        pb = ProcessTools.createJavaProcessBuilder(
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:SharedArchiveFile=" + filename,
            "-Xshare:auto",
            "--patch-module=java.naming=" + moduleJar,
            "-Xlog:class+load",
            "-Xlog:class+path=info",
            "PatchModuleMain", "javax.naming.spi.NamingManager");
        new OutputAnalyzer(pb.start())
            .shouldContain("I pass!")
            .shouldHaveExitValue(0);
    }
}
