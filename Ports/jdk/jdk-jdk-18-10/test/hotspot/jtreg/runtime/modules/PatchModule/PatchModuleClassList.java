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
 */

/**
 * @test
 * @requires vm.cds
 * @summary classes which are not useable during run time should not be included in the classlist
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          jdk.jartool/sun.tools.jar
 * @build PatchModuleMain
 * @run main PatchModuleClassList
 */

import java.nio.file.Files;
import java.nio.file.Paths;
import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.helpers.ClassFileInstaller;

public class PatchModuleClassList {
    private static final String BOOT_CLASS = "javax/naming/spi/NamingManager";
    private static final String PLATFORM_CLASS = "java/sql/ResultSet";

    public static void main(String args[]) throws Throwable {
        // Case 1. A class to be loaded by the boot class loader

        // Create a class file in the module java.naming. This class file
        // will be put in the javanaming.jar file.
        String source = "package javax.naming.spi; "                +
                        "public class NamingManager { "             +
                        "    static { "                             +
                        "        System.out.println(\"I pass!\"); " +
                        "    } "                                    +
                        "}";

        ClassFileInstaller.writeClassToDisk(BOOT_CLASS,
             InMemoryJavaCompiler.compile(BOOT_CLASS.replace('/', '.'), source, "--patch-module=java.naming"),
             System.getProperty("test.classes"));

        // Build the jar file that will be used for the module "java.naming".
        BasicJarBuilder.build("javanaming", BOOT_CLASS);
        String moduleJar = BasicJarBuilder.getTestJar("javanaming.jar");

        String classList = "javanaming.list";
        ProcessBuilder pb = ProcessTools.createTestJvm(
            "-XX:DumpLoadedClassList=" + classList,
            "--patch-module=java.naming=" + moduleJar,
            "PatchModuleMain", BOOT_CLASS.replace('/', '.'));
        OutputAnalyzer oa = new OutputAnalyzer(pb.start());
        oa.shouldContain("I pass!");
        oa.shouldHaveExitValue(0);

        // check the generated classlist file
        String content = new String(Files.readAllBytes(Paths.get(classList)));
        if (content.indexOf(BOOT_CLASS) >= 0) {
            throw new RuntimeException(BOOT_CLASS + " should not be in the classlist");
        }

        // Case 2. A class to be loaded by the platform class loader

        // Create a class file in the module java.sql. This class file
        // will be put in the javasql.jar file.
        source = "package java.sql; "                 +
                 "public class ResultSet { " +
                 "    static { "                               +
                 "        System.out.println(\"I pass too!\"); "   +
                 "    } "                                      +
                 "}";

        ClassFileInstaller.writeClassToDisk(PLATFORM_CLASS,
             InMemoryJavaCompiler.compile(PLATFORM_CLASS.replace('/', '.'), source, "--patch-module=java.sql"),
             System.getProperty("test.classes"));

        // Build the jar file that will be used for the module "java.sql".
        BasicJarBuilder.build("javasql", PLATFORM_CLASS);
        moduleJar = BasicJarBuilder.getTestJar("javasql.jar");

        classList = "javasql.list";
        pb = ProcessTools.createTestJvm(
            "-XX:DumpLoadedClassList=" + classList,
            "--patch-module=java.sql=" + moduleJar,
            "PatchModuleMain", PLATFORM_CLASS.replace('/', '.'));
        OutputAnalyzer oa2 = new OutputAnalyzer(pb.start());
        oa2.shouldContain("I pass too!");
        oa2.shouldHaveExitValue(0);

        // check the generated classlist file
        content = new String(Files.readAllBytes(Paths.get(classList)));
        if (content.indexOf(PLATFORM_CLASS) >= 0) {
            throw new RuntimeException(PLATFORM_CLASS + " should not be in the classlist");
        }

        // Case 3. A class to be loaded from the bootclasspath/a

        // Create a simple class file
        source = "public class Hello { "                         +
                 "    public static void main(String args[]) { " +
                 "        System.out.println(\"Hello\"); "       +
                 "    } "                                        +
                 "}";

        ClassFileInstaller.writeClassToDisk("Hello",
             InMemoryJavaCompiler.compile("Hello", source),
             System.getProperty("test.classes"));

        // Build hello.jar
        BasicJarBuilder.build("hello", "Hello");
        moduleJar = BasicJarBuilder.getTestJar("hello.jar");

        classList = "hello.list";
        pb = ProcessTools.createTestJvm(
            "-XX:DumpLoadedClassList=" + classList,
            "-Xbootclasspath/a:" + moduleJar,
            "Hello");
        new OutputAnalyzer(pb.start()).shouldHaveExitValue(0);

        // check the generated classlist file
        content = new String(Files.readAllBytes(Paths.get(classList)));
        if (content.indexOf("Hello") < 0) {
            throw new RuntimeException("Hello should be in the classlist");
        }
    }
}
