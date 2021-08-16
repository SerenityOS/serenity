/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8160181 8176572
 * @summary Add lint warning for digits in module names
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.code
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main PoorChoiceForModuleNameTest
 */


import java.nio.file.Path;

import toolbox.JavacTask;
import toolbox.Task;

public class PoorChoiceForModuleNameTest extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new PoorChoiceForModuleNameTest().runTests();
    }

    @Test
    public void testDigitsInModuleNames(Path base) throws Exception {

        Path src = base.resolve("src");

        // Nitpickable module name
        Path src_m1 = src.resolve("mango19");
        tb.writeJavaFiles(src_m1, "module mango19 { }");

        // Acceptable module name.
        Path src_m2 = src.resolve("mango20kg");
        tb.writeJavaFiles(src_m2, "module mango20kg { }");

        // Nitpickable, but should not due to annotation.
        Path src_m3 = src.resolve("mango100");
        tb.writeJavaFiles(src_m3, "@SuppressWarnings(\"module\") module mango100 { }");

        // Check that there is no warning at use site.
        Path src_m4 = src.resolve("mangouser");
        tb.writeJavaFiles(src_m4, "module mangouser { requires mango19; }");

        // Check that we warn about component names ending in digit also
        Path src_m5 = src.resolve("mango1000.mangofruit.mangomodule");
        tb.writeJavaFiles(src_m5, "module mango1000.mangofruit.mangomodule { }");

        // Check that we warn about component names ending in digit also
        Path src_m6 = src.resolve("mangofruit.mango1000.mangomodule");
        tb.writeJavaFiles(src_m6, "module mangofruit.mango1000.mangomodule { }");

        // Check that we warn about component names ending in digit also
        Path src_m7 = src.resolve("mangomodule.mangofruit.mango1000");
        tb.writeJavaFiles(src_m7, "module mangomodule.mangofruit.mango1000 { }");

        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "-Xlint:module",
                         "-Werror",
                         "--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:8: compiler.warn.poor.choice.for.module.name: mango19") ||
            !log.contains("module-info.java:1:8: compiler.warn.poor.choice.for.module.name: mango1000") ||
            !log.contains("module-info.java:1:18: compiler.warn.poor.choice.for.module.name: mango1000") ||
            !log.contains("module-info.java:1:30: compiler.warn.poor.choice.for.module.name: mango1000") ||
            !log.contains("- compiler.err.warnings.and.werror") ||
            !log.contains("1 error") ||
            !log.contains("4 warning"))
            throw new Exception("expected output not found: " + log);
    }
}
