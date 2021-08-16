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

/*
 * @test
 * @summary Ensure that a package whose module has not been defined to the boot loader
 *          is correctly located with -Xbootclasspath/a
 * @requires !(os.family == "windows")
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver XbootcpVisibility
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.helpers.ClassFileInstaller;

public class XbootcpVisibility {

    public static void main(String[] args) throws Throwable {

        String Vis1_B_src =
                "package p2;" +
                "public class Vis1_B { public void m() { System.out.println(\"In B's m()\"); } }";

        ClassFileInstaller.writeClassToDisk("p2/Vis1_B",
            InMemoryJavaCompiler.compile("p2.Vis1_B", Vis1_B_src), System.getProperty("test.classes"));
        ClassFileInstaller.writeClassToDisk("p2/Vis1_B", "mods1");

        String Vis1_C_src =
                "package p2;" +
                "public class Vis1_C { public void m() { System.out.println(\"In C's m()\"); } }";

        ClassFileInstaller.writeClassToDisk("p2/Vis1_C",
            InMemoryJavaCompiler.compile("p2.Vis1_C", Vis1_C_src), System.getProperty("test.classes"));
        ClassFileInstaller.writeClassToDisk("p2/Vis1_C", "mods1");

        String Vis1_A_src =
            "public class Vis1_A {" +
            "    public static void main(String args[]) throws Exception {" +
            // Try loading a class within a named package
            // in the unnamed module.
            // Ensure the class can still be loaded successfully by the
            // boot loader since it is located on -Xbootclasspath/a.
            "        try {" +
            "            p2.Vis1_B b = new p2.Vis1_B();" +
            "            if (b.getClass().getClassLoader() != null) {" +
            "              throw new RuntimeException(\"XbootcpVisibility FAILED - class B " +
                                                      "should be loaded by boot class loader\\n\");" +
            "            }" +
            "            b.m();" +
            // Now that the package p2 has been recorded in the
            // unnamed module within the boot loader's PackageEntryTable,
            // ensure that a different class within the same package
            // can be located on -Xbootclasspath/a as well.
            "            p2.Vis1_C c = new p2.Vis1_C();" +
            "            if (c.getClass().getClassLoader() != null) {" +
            "              throw new RuntimeException(\"XbootcpVisibility FAILED - class C " +
                                                       "should be loaded by boot class loader\\n\");" +
            "            }" +
            "            c.m();" +
            "        } catch (Exception e) {" +
            "            System.out.println(e);" +
            "            throw new RuntimeException(\"XbootcpVisibility FAILED - " +
                                                     "test should not throw exception\\n\");" +
            "        }" +
            "        System.out.println(\"XbootcpVisibility PASSED\\n\");" +
            "    }" +
            "}";

        ClassFileInstaller.writeClassToDisk("Vis1_A",
                InMemoryJavaCompiler.compile("Vis1_A", Vis1_A_src), System.getProperty("test.classes"));

        // Make sure the classes are actually being loaded from mods1
        Files.delete(Paths.get(System.getProperty("test.classes") +  File.separator +
                                                               "p2" + File.separator + "Vis1_B.class"));
        Files.delete(Paths.get(System.getProperty("test.classes") +  File.separator +
                                                               "p2" + File.separator + "Vis1_C.class"));

        new OutputAnalyzer(ProcessTools.createJavaProcessBuilder(
                "-Xbootclasspath/a:nonexistent.jar",
                "-Xbootclasspath/a:mods1",
                "Vis1_A")
            .start()).shouldHaveExitValue(0);
    }
}
