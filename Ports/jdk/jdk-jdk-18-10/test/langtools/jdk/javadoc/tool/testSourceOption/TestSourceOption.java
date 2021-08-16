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
 * @bug 8187588
 * @summary -bootclasspath should work with -source 8
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.javadoc/jdk.javadoc.internal.api
 *          jdk.javadoc/jdk.javadoc.internal.tool
 * @library /tools/lib
 * @build toolbox.JavacTask toolbox.JavadocTask toolbox.TestRunner toolbox.ToolBox
 * @run main TestSourceOption
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import toolbox.JarTask;
import toolbox.JavadocTask;
import toolbox.ModuleBuilder;
import toolbox.Task;
import toolbox.Task.Expect;
import toolbox.TestRunner;
import toolbox.ToolBox;

import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

public class TestSourceOption extends TestRunner {

    public static void main(String... args) throws Exception {
        TestSourceOption t = new TestSourceOption();
        t.runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    private final ToolBox tb = new ToolBox();

    TestSourceOption() throws IOException {
        super(System.err);
    }

    @Test
    public void testSourceWithBootclasspath(Path base) throws Exception {
        Files.createDirectory(base);

        Path smallRtJar = base.resolve("small-rt.jar");
        try (JavaFileManager fm = ToolProvider.getSystemJavaCompiler()
                .getStandardFileManager(null, null, null)) {

            new JarTask(tb, smallRtJar)
                    .files(fm, StandardLocation.PLATFORM_CLASS_PATH,
                            "java.lang.**", "java.io.*", "java.util.*")
                    .run();
        }

        tb.writeJavaFiles(base, "public class C { }");
        Path out = base.resolve("out");
        Files.createDirectory(out);

        JavadocTask task = new JavadocTask(tb);
        task.outdir(out)
            .options("-bootclasspath", smallRtJar.toString(),
                     "-source", "8")
            .files(base.resolve("C.java"))
            .run(Expect.SUCCESS);
    }
}
