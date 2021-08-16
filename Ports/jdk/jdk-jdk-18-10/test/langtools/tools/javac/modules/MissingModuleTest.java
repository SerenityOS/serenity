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
 * @bug 8172240
 * @summary javac should not need the transitive closure to compile a module
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.code
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.compiler/com.sun.tools.javac.processing
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.ModuleBuilder ModuleTestBase
 * @run main MissingModuleTest
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.Expect;

public class MissingModuleTest extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new MissingModuleTest().runTests();
    }

    @Test
    public void testMissingNotNeeded(Path base) throws Exception {
        doTest(base, "m1x", new String[0]);
    }

    @Test
    public void testMissingNeededTransitive(Path base) throws Exception {
        doTest(base, "m2x", "- compiler.err.module.not.found: m2x", "1 error");
    }

    @Test
    public void testMissingNeededDirect(Path base) throws Exception {
        doTest(base, "m3x", "module-info.java:1:24: compiler.err.module.not.found: m3x", "1 error");
    }

    @Test
    public void testMultipleErrors(Path base) throws Exception {
        doTest(base, "m4x", "module-info.java:1:38: compiler.err.module.not.found: m4x", "1 error");
    }

    private void doTest(Path base, String toDelete, String... errors) throws Exception {
        //note: avoiding use of java.base, as that gets special handling on some places:
        Path srcMP = base.resolve("src-mp");
        Path m1x = srcMP.resolve("m1x");
        tb.writeJavaFiles(m1x,
                          "module m1x { exports api1; }",
                          "package api1; public interface Api { }");
        Path m2x = srcMP.resolve("m2x");
        tb.writeJavaFiles(m2x,
                          "module m2x { requires m1x; exports api2; }",
                          "package api2; public interface Api { }");
        Path m3x = srcMP.resolve("m3x");
        tb.writeJavaFiles(m3x,
                          "module m3x { requires transitive m2x; }");
        Path m4x = srcMP.resolve("m4x");
        tb.writeJavaFiles(m4x,
                          "module m4x { requires transitive m2x; }");
        Path m5x = srcMP.resolve("m5x");
        tb.writeJavaFiles(m5x,
                          "module m5x { requires transitive m4x; }");
        Path classesMP = base.resolve("classes-mp");
        tb.createDirectories(classesMP);

        new JavacTask(tb)
            .options("--module-source-path", srcMP.toString())
            .outdir(classesMP)
            .files(findJavaFiles(srcMP))
            .run()
            .writeAll();

        tb.cleanDirectory(classesMP.resolve(toDelete));
        Files.delete(classesMP.resolve(toDelete));

        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "module test { requires m3x; requires m4x; requires m5x; } ");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log = new JavacTask(tb)
                .options("--module-path", classesMP.toString(),
                         "-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(errors.length > 0 ? Expect.FAIL : Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (errors.length == 0) {
            errors = new String[] {""};
        }

        if (!log.equals(Arrays.asList(errors)))
            throw new Exception("expected output not found: " + log);
    }

}
