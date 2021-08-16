/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8231599
 * @summary Verify javac does not crash on preview classfiles from the future
            Java versions.
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.jvm
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main TooNewMajorVersionTest
 */

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.TestRunner;
import toolbox.ToolBox;

import com.sun.tools.javac.jvm.ClassFile.Version;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class TooNewMajorVersionTest extends TestRunner {

    protected ToolBox tb;

    TooNewMajorVersionTest() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        TooNewMajorVersionTest t = new TooNewMajorVersionTest();
        t.runTests();
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    @Test
    public void brokenMajorVersionWithPreview(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, """
                               class C {
                                   private Object o = null;
                                   private boolean b = o instanceof String s;
                               }
                               """);
        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        for (int upgrade = 1; upgrade < 3; upgrade++) {
            new JavacTask(tb)
                    .outdir(classes)
                    .options("-XDforcePreview",
                             "--enable-preview",
                             "--release", String.valueOf(Runtime.version().feature()))
                    .files(tb.findJavaFiles(src))
                    .run()
                    .writeAll();

            Path classfile = classes.resolve("C.class");
            int wrongMajor;

            try (RandomAccessFile f = new RandomAccessFile(classfile.toFile(), "rw")) {
                f.readInt();
                short minor = f.readShort();
                if (minor != (-1)) {
                    throw new AssertionError("Unexpected minor version: " + minor);
                }
                long point = f.getFilePointer();
                short major = f.readShort();
                f.seek(point);
                f.writeShort(wrongMajor = major + upgrade);
            }

            Path test = base.resolve("test");
            tb.writeJavaFiles(test, "class Test extends C {}");
            Path testClasses = base.resolve("classes");

            Files.createDirectories(testClasses);

            for (String extraOption : new String[] {"-XDignored", "--enable-preview"}) {
                List<String> log = new JavacTask(tb)
                        .outdir(testClasses)
                        .options("-XDrawDiagnostics",
                                 "-classpath", classes.toString(),
                                 "--release", String.valueOf(Runtime.version().feature()),
                                 extraOption)
                        .files(tb.findJavaFiles(test))
                        .run(Task.Expect.FAIL)
                        .writeAll()
                        .getOutputLines(Task.OutputKind.DIRECT);
                List<String> expected = List.of(
                        "Test.java:1:20: compiler.err.cant.access: C, (compiler.misc.bad.class.file.header: C.class, (compiler.misc.wrong.version: " + wrongMajor + ", 65535, " + Version.MAX().major + ", 0))",
                        "1 error"
                );
                if (!log.equals(expected))
                    throw new Exception("expected output not found" + log);
            }
        }
    }
}
