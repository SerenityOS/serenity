/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8175346
 * @summary Test patch module options
 * @modules
 *      jdk.javadoc/jdk.javadoc.internal.api
 *      jdk.javadoc/jdk.javadoc.internal.tool
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @library /tools/lib
 * @build toolbox.ToolBox toolbox.TestRunner
 * @run main PatchModules
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import toolbox.*;

public class PatchModules extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new PatchModules().runTests();
    }

    // Case A.1, m2 augmenting m1
    @Test
    public void testPatchModuleOption(Path base) throws Exception {
        Path src = base.resolve("src");
        Path modulePath = base.resolve("modules");
        Path patchPath = base.resolve("patch");

        ModuleBuilder mb1 = new ModuleBuilder(tb, "m1");
        mb1.comment("Module on module path.")
                .exports("pkg1")
                .classes("package pkg1; /** Class A */ public class A { }")
                .build(modulePath);

        tb.writeJavaFiles(patchPath, "package pkg1; /** Class A */ public class A { public static int k; }");
        new JavacTask(tb)
                .files(patchPath.resolve("pkg1/A.java"))
                .run();

        ModuleBuilder mb2 = new ModuleBuilder(tb, "m2");
        mb2.comment("The second module.")
                .exports("pkg2")
                .requires("m1")
                .classes("package pkg2; /** Class B */ public class B { /** Field f */ public int f = pkg1.A.k; }")
                .write(src);
        execTask("--module-source-path", src.toString(),
                "--patch-module", "m1=" + patchPath.toString(),
                "--module-path", modulePath.toString(),
                "--module", "m2");
        checkModulesSpecified("m2");
        checkPackagesIncluded("pkg2");
        checkMembersSelected("pkg2.B.f");
    }

    // Case A.2: use package, source form of m1 augmenting binary form of m1
    @Test
    public void testPatchModuleWithPackage(Path base)  throws Exception {
        Path modulePath = base.resolve("modules");
        Path moduleSrcPath = base.resolve("modulesSrc");

        Path mpath = Paths.get(moduleSrcPath.toString(),  "m1");

        ModuleBuilder mb1 = new ModuleBuilder(tb, "m1");
        mb1.comment("Module m1.")
                .exports("pkg1")
                .classes("package pkg1; /** Class A */ public class A { }")
                .classes("package pkg1.pkg2; /** Class B */ public class B { }")
                .build(modulePath);

        execTask("-hasDocComments",
                "--module-path", modulePath.toString(),
                "--add-modules", "m1",
                "--patch-module", "m1=" + mpath.toString(),
                "pkg1");
        checkTypesIncluded("pkg1.A hasDocComments");
    }

     // Case A.2: use subpackages, source form of m1 augmenting binary form of m1
    @Test
    public void testPatchModuleWithSubPackages(Path base) throws Exception {
        Path modulePath = base.resolve("modules");
        Path moduleSrcPath = base.resolve("modulesSrc");

        Path mpath = Paths.get(moduleSrcPath.toString(),  "m1");

        ModuleBuilder mb1 = new ModuleBuilder(tb, "m1");
        mb1.comment("Module m1.")
                .exports("pkg1")
                .classes("package pkg1; /** Class A */ public class A { }")
                .classes("package pkg1.pkg2; /** Class B */ public class B { }")
                .build(modulePath);

        execTask("-hasDocComments",
                "--module-path", modulePath.toString(),
                "--add-modules", "m1",
                "--patch-module", "m1=" + mpath.toString(),
                "-subpackages", "pkg1");
        checkTypesIncluded("pkg1.A hasDocComments");
        checkTypesIncluded("pkg1.pkg2.B hasDocComments");
    }

    // Case B.1: (jsr166) augment and override system module
    @Test
    public void testPatchModuleModifyingSystemModule(Path base) throws Exception {
        Path patchSrc = base.resolve("patch");

        // build the patching sources
        tb.writeJavaFiles(patchSrc, """
            package java.util;
            /** Class Collection */
            public interface Collection<K> {}""");

        tb.writeJavaFiles(patchSrc, """
            package java.util;
            /** Class MyCollection */
            public interface MyCollection<K> extends Collection {}""");

        execTask("-hasDocComments", "--patch-module", "java.base=" + patchSrc.toString(),
                "java.util");

        checkPackagesSpecified("java.util");
        checkTypesIncluded("java.util.Collection hasDocComments",
                "java.util.MyCollection hasDocComments");
    }

    // Case C.1: patch a user module's sources using source path
    @Test
    public void testPatchModuleUsingSourcePath(Path base) throws Exception {
        Path src = base.resolve("src");
        Path patchSrc = base.resolve("patch");

        ModuleBuilder mb1 = new ModuleBuilder(tb, "m1");
        mb1.comment("Module m1.")
                .exports("pkg1")
                .classes("package pkg1; /** Class A */ public class A { }")
                .write(src);

        // build the patching module
        tb.writeJavaFiles(patchSrc, """
            package pkg1;
            /** Class A */ public class A extends java.util.ArrayList { }""");
        tb.writeJavaFiles(patchSrc, """
            package pkg1;
            /** Class B */ public class B { }""");

        Path m1src = Paths.get(src.toString(), "m1");

        execTask("--source-path", m1src.toString(),
                "--patch-module", "m1=" + patchSrc.toString(),
                "pkg1");

        checkPackagesSpecified("pkg1");
        checkModulesIncluded("m1");
        checkTypesIncluded("pkg1.A", "pkg1.B");
    }

    // Case C.2: patch a user module's sources using module source path
    @Test
    public void testPatchModuleWithModuleSourcePath(Path base) throws Exception {
        Path src = base.resolve("src");
        Path patchSrc = base.resolve("patch");

        ModuleBuilder mb1 = new ModuleBuilder(tb, "m1");
        mb1.comment("Module on module-source-path.")
                .exports("pkg1")
                .classes("package pkg1; /** Class A */ public class A { }")
                .write(src);

        // build the patching module
        tb.writeJavaFiles(patchSrc, """
            package pkg1;
            /** Class A */ public class A extends java.util.ArrayList { }""");
        tb.writeJavaFiles(patchSrc, """
            package pkg1;
            /** Class B */ public class B { }""");


        execTask("--module-source-path", src.toString(),
                "--add-modules", "m1",
                "--patch-module", "m1=" + patchSrc.toString(),
                "pkg1");

        checkPackagesSpecified("pkg1");
        checkModulesIncluded("m1");
        checkTypesIncluded("pkg1.A", "pkg1.B");
    }

}
