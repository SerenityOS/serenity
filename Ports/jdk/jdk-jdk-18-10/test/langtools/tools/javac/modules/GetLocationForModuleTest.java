/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8162712
 * @summary StandardJavaFileManager.getModuleLocation() can't find a module
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.JavacTask toolbox.ToolBox
 * @run main GetLocationForModuleTest
 */

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import javax.tools.*;
import toolbox.JavacTask;
import toolbox.ToolBox;

public class GetLocationForModuleTest extends ModuleTestBase {
    public static void main(String... args) throws Exception {
        new GetLocationForModuleTest().run(Paths.get("."));
    }

    public void run(Path base) throws Exception {
        // Set up some trivial modules
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");
        tb.writeJavaFiles(m1, "module m1x { }");
        Path m2 = moduleSrc.resolve("m2x");
        tb.writeJavaFiles(m2, "module m2x { }");

        Path modulePath = base.resolve("module-path");
        Files.createDirectories(modulePath);
        new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString())
                .outdir(modulePath)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll();

        // Init file manager
        StandardJavaFileManager fm =
                ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null);
        fm.setLocationFromPaths(StandardLocation.MODULE_PATH, Arrays.asList(modulePath));

        // Test
        test(fm, StandardLocation.SYSTEM_MODULES, "java.base", "java.compiler");
        test(fm, StandardLocation.MODULE_PATH, "m1x", "m2x");
    }

    void test(JavaFileManager fm, JavaFileManager.Location locn, String... mods) throws IOException {
        for (String mod : mods) {
            JavaFileManager.Location modLocn = fm.getLocationForModule(locn, mod);
            if (modLocn == null) {
                error(locn.getName() + ": can't find " + mod);
            } else {
                System.err.println(locn.getName() + ": found " + mod + ": " + modLocn.getName());
            }
        }
    }
}

