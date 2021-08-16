/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8228460
 * @summary Verify --system is required rather than -bootclasspath for -source 9.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.file
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.TestRunner
 * @run main BCPOrSystemNotSpecified
 */

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.EnumSet;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.Expect;
import toolbox.TestRunner;
import toolbox.ToolBox;

import com.sun.tools.javac.file.PathFileObject;

public class BCPOrSystemNotSpecified extends TestRunner {

    private final ToolBox tb = new ToolBox();
    private final String fileSep = System.getProperty("file.separator");

    public BCPOrSystemNotSpecified() {
        super(System.err);
    }

    public static void main(String... args) throws Exception {
        new BCPOrSystemNotSpecified().runTests();
    }

    @Test
    public void testSource8(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "package test; public class Test { } ");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log;
        List<String> expected = Arrays.asList(
                "- compiler.warn.source.no.bootclasspath: 8",
                "1 warning"
        );

        log = new JavacTask(tb)
                .options("-XDrawDiagnostics", "-source", "8")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }

        Path bcp = base.resolve("bcp");

        prepareBCP(bcp);

        new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "-source", "8",
                         "-bootclasspath", bcp.toAbsolutePath().toString(),
                         "-Werror")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

    @Test
    public void testSource9(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "package test; public class Test { } ");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log;
        List<String> expected = Arrays.asList(
                "- compiler.warn.source.no.system.modules.path: 9",
                "1 warning"
        );

        log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "-source", "9")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }

        Path bcp = base.resolve("bcp");

        prepareBCP(bcp);

        log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "-source", "9",
                         "-bootclasspath", bcp.toAbsolutePath().toString())
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }

        new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "-source", "9",
                         "--system", "none",
                         "--module-path", bcp.toAbsolutePath().toString(),
                         "-Werror")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }

        new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "-source", "9",
                         "--system", System.getProperty("java.home"),
                         "-Werror")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()).toAbsolutePath() });
    }

    private void prepareBCP(Path target) throws IOException {
        try (StandardJavaFileManager jfm = ToolProvider.getSystemJavaCompiler()
                                               .getStandardFileManager(null, null, null)) {
            for (String pack : new String[] {"", "java.lang", "java.lang.annotation", "jdk.internal.javac"}) {
                JavaFileManager.Location javaBase =
                        jfm.getLocationForModule(StandardLocation.SYSTEM_MODULES,
                                                 "java.base");
                for (JavaFileObject file : jfm.list(javaBase,
                                                    pack,
                                                    EnumSet.of(JavaFileObject.Kind.CLASS),
                                                    false)) {
                    Path targetDir = target.resolve(pack.replace(".", fileSep));
                    Files.createDirectories(targetDir);
                    try (InputStream in = file.openInputStream()) {
                        String sourcePath = file.getName();
                        // Here, we should use file system separator instead of the operating system separator.
                        String fileSystemSep = jfm.asPath(file).getFileSystem().getSeparator();
                        int sepPos = sourcePath.lastIndexOf(fileSystemSep);
                        String fileName = sourcePath.substring(sepPos + 1);
                        Files.copy(in, targetDir.resolve(fileName));
                    }
                }
            }
        }
    }
}
