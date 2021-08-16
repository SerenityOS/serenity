/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8208269
 * @summary Verify module-infos are read from the correct place in multi-release jars
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.javadoc/jdk.javadoc.internal.api
 *          jdk.javadoc/jdk.javadoc.internal.tool
 * @build toolbox.JavacTask toolbox.JavadocTask toolbox.TestRunner toolbox.ToolBox
 * @run main TestMultiRelease
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.JavadocTask;
import toolbox.Task.Expect;
import toolbox.TestRunner;
import toolbox.ToolBox;

public class TestMultiRelease extends TestRunner {

    public static void main(String... args) throws Exception {
        TestMultiRelease t = new TestMultiRelease();
        t.runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    private final ToolBox tb = new ToolBox();

    TestMultiRelease() throws IOException {
        super(System.err);
    }

    @Test
    public void testMultiReleaseModule(Path base) throws Exception {
        Files.createDirectory(base);

        Path module = base.resolve("module");
        Path moduleSrc = module.resolve("src");
        Path moduleCls = module.resolve("classes");

        tb.writeJavaFiles(moduleSrc, "module test.module { }");

        Files.createDirectories(moduleCls);

        new JavacTask(tb)
                .outdir(moduleCls)
                .files(tb.findJavaFiles(moduleSrc))
                .run()
                .writeAll();

        Path moduleJarDir = module.resolve("jar-dir");
        Path versions = moduleJarDir.resolve("META-INF/versions/10");

        Files.createDirectories(versions);

        Files.copy(moduleCls.resolve("module-info.class"),
                   versions.resolve("module-info.class"));

        Path moduleJar = module.resolve("module.jar");

        new JarTask(tb, moduleJar)
                .baseDir(moduleJarDir)
                .files(Arrays.stream(tb.findFiles("class", moduleJarDir))
                             .map(p -> moduleJarDir.relativize(p).toString())
                             .toArray(s -> new String[s]))
                .manifest("Multi-Release: true\n\n")
                .run();

        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
                          "module m { requires test.module; exports api; }",
                          "package api; public class Api { }");
        Path out = base.resolve("out");
        Files.createDirectory(out);

        JavadocTask task = new JavadocTask(tb);
        task.outdir(out)
            .options("--module-path", moduleJar.toString(),
                     "-source", "10")
            .files(tb.findJavaFiles(src))
            .run(Expect.SUCCESS);

        task.outdir(out)
            .options("--module-path", moduleJar.toString(),
                     "-source", "9")
            .files(tb.findJavaFiles(src))
            .run(Expect.FAIL);
    }
}
