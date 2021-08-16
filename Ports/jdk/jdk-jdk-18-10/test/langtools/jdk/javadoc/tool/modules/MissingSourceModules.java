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

/*
 * @test
 * @bug 8176481 8246705
 * @summary Tests behavior of the tool, when modules are present as
 *          binaries.
 * @modules
 *      jdk.javadoc/jdk.javadoc.internal.api
 *      jdk.javadoc/jdk.javadoc.internal.tool
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @library /tools/lib
 * @build toolbox.ToolBox toolbox.TestRunner
 * @run main MissingSourceModules
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import toolbox.*;

public class MissingSourceModules extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new MissingSourceModules().runTests();
    }

    @Test
    public void testExplicitBinaryModuleOnModulePath(Path base) throws Exception {
        Path modulePath = base.resolve("modules");

        ModuleBuilder ma = new ModuleBuilder(tb, "ma");
        ma.comment("Module ma.")
                .exports("pkg1")
                .classes("package pkg1; /** Class A */ public class A { }")
                .classes("package pkg1.pkg2; /** Class B */ public class B { }")
                .build(modulePath);

        execNegativeTask("--module-path", modulePath.toString(),
                "--module", "ma");
        assertMessagePresent("module ma not found");
    }

    @Test
    public void testExplicitBinaryModuleOnLegacyPaths(Path base) throws Exception {
        Path modulePath = base.resolve("modules");

        ModuleBuilder ma = new ModuleBuilder(tb, "ma");
        ma.comment("Module ma.")
                .exports("pkg1")
                .classes("package pkg1; /** Class A */ public class A { }")
                .classes("package pkg1.pkg2; /** Class B */ public class B { }")
                .build(modulePath);

        Path mPath = Paths.get(modulePath.toString(), "ma");
        execNegativeTask("--source-path", mPath.toString(),
                "--module", "ma");
        assertMessagePresent("module ma not found on source path");

        execNegativeTask("--class-path", mPath.toString(),
                "--module", "ma");
        assertMessagePresent("module ma not found");
    }

    @Test
    public void testImplicitBinaryRequiresModule(Path base) throws Exception {
        Path src = base.resolve("src");
        Path modulePath = base.resolve("modules");

        ModuleBuilder mb = new ModuleBuilder(tb, "mb");
        mb.comment("Module mb.")
                .exports("pkgb")
                .classes("package pkgb; /** Class A */ public class A { }")
                .build(modulePath);

        ModuleBuilder ma = new ModuleBuilder(tb, "ma");
        ma.comment("Module ma.")
                .exports("pkga")
                .requires("mb", modulePath)
                .classes("package pkga; /** Class A */ public class A { }")
                .write(src);

        execTask("--module-path", modulePath.toString(),
                "--module-source-path", src.toString(),
                "--expand-requires", "all",
                "--module", "ma");
        assertMessagePresent("source files for module mb not found");
    }

    @Test
    public void testImplicitBinaryTransitiveModule(Path base) throws Exception {
        Path src = base.resolve("src");
        Path modulePath = base.resolve("modules");

        ModuleBuilder mb = new ModuleBuilder(tb, "mb");
        mb.comment("Module mb.")
                .exports("pkgb")
                .classes("package pkgb; /** Class A */ public class A { }")
                .build(modulePath);

        ModuleBuilder ma = new ModuleBuilder(tb, "ma");
        ma.comment("Module ma.")
                .exports("pkga")
                .requiresTransitive("mb", modulePath)
                .classes("package pkga; /** Class A */ public class A { }")
                .write(src);

        execTask("--module-path", modulePath.toString(),
                "--module-source-path", src.toString(),
                "--expand-requires", "transitive",
                "--module", "ma");
        assertMessagePresent("source files for module mb not found");
    }
}
