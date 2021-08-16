/*
 * Copyright (c) 2019, Google LLC. All rights reserved.
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
 * @bug 8193277
 * @summary SimpleFileObject inconsistency between getName and getShortName
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.JavacTask toolbox.TestRunner toolbox.ToolBox
 * @run main SymLinkShortNameTest
 */

import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import toolbox.JavacTask;
import toolbox.Task.Expect;
import toolbox.Task.OutputKind;
import toolbox.Task.Result;
import toolbox.TestRunner;
import toolbox.TestRunner.Test;
import toolbox.ToolBox;

public class SymLinkShortNameTest extends TestRunner {
    public static void main(String... args) throws Exception {
        new SymLinkShortNameTest().runTests(m -> new Object[] {Paths.get(m.getName())});
    }

    private final ToolBox tb = new ToolBox();

    public SymLinkShortNameTest() {
        super(System.err);
    }

    @Test
    public void testJarSymlink(Path base) throws Exception {
        Files.createDirectories(base);
        Path b = base.resolve("B.java");
        tb.writeFile(b, "class B { int f() {} }");

        Path a = base.resolve("A.java");
        try {
            Files.createSymbolicLink(a, b.getFileName());
        } catch (FileSystemException fse) {
            System.err.println("warning: test passes vacuously, sym-link could not be created");
            System.err.println(fse.getMessage());
            return;
        }

        {
            Result result =
                    new JavacTask(tb).options("-XDrawDiagnostics").files(a).run(Expect.FAIL);
            String output = result.getOutput(OutputKind.DIRECT);

            String expected = "A.java:1:20: compiler.err.missing.ret.stmt";
            if (!output.contains(expected)) {
                throw new AssertionError(
                        "expected output to contain: " + expected + "\nwas:\n" + output);
            }
        }
        {
            Result result = new JavacTask(tb).files(a).run(Expect.FAIL);
            String output = result.getOutput(OutputKind.DIRECT);

            String expected = "A.java:1: error: missing return statement";
            if (!output.contains(expected)) {
                throw new AssertionError(
                        "expected output to contain: " + expected + "\nwas:\n" + output);
            }
        }
    }
}
