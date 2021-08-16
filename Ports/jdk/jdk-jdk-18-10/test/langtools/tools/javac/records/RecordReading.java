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
 * @summary test the records can be read by javac properly
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main RecordReading
 */


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import toolbox.TestRunner;
import toolbox.ToolBox;
import toolbox.JavacTask;
import toolbox.Task;

public class RecordReading extends TestRunner {
    ToolBox tb;

    RecordReading() {
        super(System.err);
        tb = new ToolBox();
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[]{Paths.get(m.getName())});
    }

    public static void main(String... args) throws Exception {
        RecordReading t = new RecordReading();
        t.runTests();
    }

    Path[] findJavaFiles(Path... paths) throws IOException {
        return tb.findJavaFiles(paths);
    }

    @Test
    public void testRecordClassFileReading(Path base) throws Exception {
        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
                           """
                           public record R(int i, @A long j, java.util.List<String> l) {}
                           """,
                           """
                           public @interface A {}
                           """);

        Path out = base.resolve("out");
        Files.createDirectories(out);

        new JavacTask(tb)
                .outdir(out)
                .files(findJavaFiles(src))
                .run();

        //read the class file back, to verify javac's ClassReader
        //reads the Record attribute properly:
        String output = new JavacTask(tb)
                .options("-Xprint")
                .classpath(out.toString())
                .classes("R")
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.STDOUT)
                .replaceAll("\\R", "\n");

        String expected =
                """
                \n\
                public record R(int i, @A long j, java.util.List<java.lang.String> l) {
                  private final int i;
                  @A
                  private final long j;
                  private final java.util.List<java.lang.String> l;
                \n\
                  public R(int i,
                    @A long j,
                    java.util.List<java.lang.String> l);
                \n\
                  public final java.lang.String toString();
                \n\
                  public final int hashCode();
                \n\
                  public final boolean equals(java.lang.Object arg0);
                \n\
                  public int i();
                \n\
                  @A
                  public long j();
                \n\
                  public java.util.List<java.lang.String> l();
                }
                """;
        if (!Objects.equals(expected, output)) {
            throw new AssertionError("Unexpected output: " + output);
        }
    }

}
