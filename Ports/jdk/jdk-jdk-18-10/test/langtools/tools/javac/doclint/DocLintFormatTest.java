/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8172474 8247957
 * @summary javac should enable doclint checking for HTML 5
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main DocLintFormatTest
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class DocLintFormatTest {
    public static void main(String... args) throws Exception {
        new DocLintFormatTest().run();
    }

    private ToolBox tb = new ToolBox();
    private Path src = Paths.get("src");
    private Path classes = Paths.get("classes");

    void run() throws Exception {
        Files.createDirectories(classes);

        tb.writeJavaFiles(src,
            //        1         2
            //2345678901234567890
            "/** This is an <tt>HTML 4</tt> comment. */ public class Test4 { }",
            "/** This is an <mark>HTML 5</mark> comment. */ public class Test5 { }"
        );

        test(src.resolve("Test4.java"),
                "Test4.java:1:16: compiler.err.proc.messager: tag not supported in HTML5: tt");
        test(src.resolve("Test5.java"));

        if (errors > 0) {
            throw new Exception(errors + " errors occurred");
        }
    }

    void test(Path file, String... expect) {
        System.err.println("Test: " + file);
        List<String> output = new JavacTask(tb)
                  .outdir(classes)
                  .options("-XDrawDiagnostics", "-Xdoclint")
                  .files(file)
                  .run(expect.length == 0 ? Task.Expect.SUCCESS : Task.Expect.FAIL)
                  .writeAll()
                  .getOutputLines(Task.OutputKind.DIRECT);

        if (expect.length == 0) {
            if (!(output.size() == 1 && output.get(0).isEmpty())) {
                error("All output unexpected.");
            }
        } else {
            for (String e : expect) {
                if (!output.contains(e)) {
                    error("expected output not found: " + e);
                }
            }
        }
    }

    void error(String message) {
        System.err.println("Error: " + message);
        errors++;
    }

    private int errors = 0;
}

