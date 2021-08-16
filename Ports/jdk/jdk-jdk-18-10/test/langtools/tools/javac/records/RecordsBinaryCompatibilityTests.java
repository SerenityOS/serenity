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
 * @summary test binary compatibility rules for record classes
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main RecordsBinaryCompatibilityTests
 */

import java.util.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

import com.sun.tools.classfile.*;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.util.Assert;
import toolbox.TestRunner;
import toolbox.ToolBox;
import toolbox.JavaTask;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.OutputKind;

public class RecordsBinaryCompatibilityTests extends TestRunner {
    ToolBox tb;

    RecordsBinaryCompatibilityTests() {
        super(System.err);
        tb = new ToolBox();
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[]{Paths.get(m.getName())});
    }

    public static void main(String... args) throws Exception {
        RecordsBinaryCompatibilityTests t = new RecordsBinaryCompatibilityTests();
        t.runTests();
    }

    Path[] findJavaFiles(Path... paths) throws IOException {
        return tb.findJavaFiles(paths);
    }

    @Test
    public void testCompatibilityAfterAddingRecordComponent(Path base) throws Exception {
        testCompatibilityAfterModifyingRecord(
                base,
                """
                package pkg;
                public record R(String i) {}
                """,
                """
                package pkg;
                public record R(String i, String j) {}
                """,
                """
                package pkg;
                public class Client {
                    public static void main(String... args) {
                        R r = new R("Hello World!");
                        System.out.println(r.i());
                    }
                }
                """,
                true,
                NoSuchMethodError.class
        );
    }

    @Test
    public void testCompatibilityAfterDeletingRecordComponent(Path base) throws Exception {
        testCompatibilityAfterModifyingRecord(
                base,
                """
                package pkg;
                public record R(String i, String j) {
                    public R(String j) {
                        this("Hello World!", j);
                    }
                }
                """,
                """
                package pkg;
                public record R(String j) {}
                """,
                """
                package pkg;
                public class Client {
                    public static void main(String... args) {
                        R r = new R("Hi");
                        System.out.println(r.i());
                    }
                }
                """,
                true,
                NoSuchMethodError.class
        );
    }

    @Test
    public void testCompatibilityAfterChangingRecordComponent(Path base) throws Exception {
        testCompatibilityAfterModifyingRecord(
                base,
                """
                package pkg;
                public record R(String i, double j) {}
                """,
                """
                package pkg;
                public record R(String i, int j) {}
                """,
                """
                package pkg;
                public class Client {
                    public static void main(String... args) {
                        R r = new R("Hello World!", 1);
                        System.out.println(r.i());
                    }
                }
                """,
                true,
                NoSuchMethodError.class
        );
    }

    @Test
    public void testCompatibilityAfterReorderingRecordComponents(Path base) throws Exception {
        testCompatibilityAfterModifyingRecord(
                base,
                """
                package pkg;
                public record R(String i, int j) {}
                """,
                """
                package pkg;
                public record R(int j, String i) {}
                """,
                """
                package pkg;
                public class Client {
                    public static void main(String... args) {
                        R r = new R("Hello World!", 1);
                        System.out.println(r.i());
                    }
                }
                """,
                true,
                NoSuchMethodError.class
        );
    }

    @Test
    public void testCompatibilityAfterChangingRecordComponent2(Path base) throws Exception {
        testCompatibilityAfterModifyingRecord(
                base,
                """
                package pkg;
                public record R(String j) {
                    public static String i() { return "Hello World!"; }
                }
                """,
                """
                package pkg;
                public record R(String i) {}
                """,
                """
                package pkg;
                public class Client {
                    public static void main(String... args) {
                        R r = new R("Hello World!");
                        System.out.println(r.i());
                    }
                }
                """,
                true,
                IncompatibleClassChangeError.class
        );
    }

    @Test
    public void testCompatibilityAfterChangingRecordComponent3(Path base) throws Exception {
        testCompatibilityAfterModifyingRecord(
                base,
                """
                package pkg;
                public record R(String i) {
                }
                """,
                """
                package pkg;
                public record R(String j) {}
                """,
                """
                package pkg;
                public class Client {
                    public static void main(String... args) {
                        R r = new R("Hello World!");
                        System.out.println(r.i());
                    }
                }
                """,
                true,
                NoSuchMethodError.class
        );
    }

    /* 1- compiles the first version of the record class source code along with the client source code
     * 2- executes the client class just to make sure that it works
     * 3- compiles the second version of the record class
     * 4- executes the client class and makes sure that the VM throws the expected error or not
     *    depending on the shouldFail argument
     */
    private void testCompatibilityAfterModifyingRecord(
            Path base,
            String recordCode1,
            String recordCode2,
            String clientCode,
            boolean shouldFail,
            Class<?> expectedError) throws Exception {
        Path src = base.resolve("src");
        Path pkg = src.resolve("pkg");
        Path recordSrc = pkg.resolve("R");
        Path client = pkg.resolve("Client");

        tb.writeJavaFiles(recordSrc, recordCode1);
        tb.writeJavaFiles(client, clientCode);

        Path out = base.resolve("out");
        Files.createDirectories(out);

        new JavacTask(tb)
                .outdir(out)
                .files(findJavaFiles(pkg))
                .run();

        // let's execute to check that it's working
        String output = new JavaTask(tb)
                .classpath(out.toString())
                .classArgs("pkg.Client")
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.STDOUT);

        // let's first check that it runs wo issues
        if (!output.contains("Hello World!")) {
            throw new AssertionError("execution of Client didn't finish");
        }

        // now lets change the record class
        tb.writeJavaFiles(recordSrc, recordCode2);

        new JavacTask(tb)
                .outdir(out)
                .files(findJavaFiles(recordSrc))
                .run();

        if (shouldFail) {
            // let's now check that we get the expected error
            output = new JavaTask(tb)
                    .classpath(out.toString())
                    .classArgs("pkg.Client")
                    .run(Task.Expect.FAIL)
                    .writeAll()
                    .getOutput(Task.OutputKind.STDERR);
            if (!output.startsWith("Exception in thread \"main\" " + expectedError.getName())) {
                throw new AssertionError(expectedError.getName() + " expected");
            }
        } else {
            new JavaTask(tb)
                    .classpath(out.toString())
                    .classArgs("pkg.Client")
                    .run(Task.Expect.SUCCESS);
        }
    }
}
