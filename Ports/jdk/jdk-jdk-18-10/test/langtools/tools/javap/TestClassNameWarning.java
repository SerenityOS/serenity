/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8170708
 * @summary javap -m <module> cannot read a module-info.class
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.jdeps/com.sun.tools.classfile
 *      jdk.jdeps/com.sun.tools.javap
 * @build toolbox.JavacTask toolbox.JavapTask toolbox.ToolBox toolbox.TestRunner
 * @run main TestClassNameWarning
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ClassWriter;

import toolbox.JavacTask;
import toolbox.JavapTask;
import toolbox.Task;
import toolbox.TestRunner;
import toolbox.ToolBox;

public class TestClassNameWarning extends TestRunner {
    public static void main(String... args) throws Exception {
        new TestClassNameWarning().runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    private ToolBox tb = new ToolBox();

    TestClassNameWarning() {
        super(System.err);
    }

    /**
     * Baseline test for normal classes.
     */
    @Test
    public void testStandardClass(Path base) throws Exception {
        Path src = base.resolve("src");
        Path classes = Files.createDirectories(base.resolve("classes"));
        tb.writeJavaFiles(src, "class A { }");

        new JavacTask(tb)
                .outdir(classes.toString())
                .files(tb.findJavaFiles(src))
                .run()
                .writeAll();

        List<String> log = new JavapTask(tb)
                .classpath(classes.toString())
                .classes("A")
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        checkOutput(log, false, "^(Warning|Error)");
        checkOutput(log, true, "class A");
    }

    /**
     * Test that module-info can be used to name the .class file
     * for a module declaration (i.e. ACC_MODULE, this_class == 0)
     * This is the primary test case for the bug as reported.
     */
    @Test
    public void testStandardModuleInfo(Path base) throws Exception {
        Path src = base.resolve("src");
        Path classes = Files.createDirectories(base.resolve("classes"));
        tb.writeJavaFiles(src, "module m { }");

        new JavacTask(tb)
                .outdir(classes.toString())
                .files(tb.findJavaFiles(src))
                .run()
                .writeAll();

        List<String> log = new JavapTask(tb)
                .options("--module-path", classes.toString(),
                        "--module", "m")
                .classes("module-info")
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        checkOutput(log, false, "^(Warning|Error)");
        checkOutput(log, true, "module m");
    }

    /**
     * Test module-info can still be used to find a weirdly named
     * class equivalent to "class module-info { }" if that were legal in JLS.
     * Such a class file would arguably be legal in certain selected contexts.
     */
    @Test
    public void testLegacyModuleInfo(Path base) throws Exception {
        Path src = base.resolve("src");
        Path classes = Files.createDirectories(base.resolve("classes"));
        tb.writeJavaFiles(src, "class module_info { }");

        new JavacTask(tb)
                .outdir(classes.toString())
                .files(tb.findJavaFiles(src))
                .run()
                .writeAll();

        byte[] bytes = Files.readAllBytes(classes.resolve("module_info.class"));
        byte[] searchBytes = "module_info".getBytes("UTF-8");
        byte[] replaceBytes = "module-info".getBytes("UTF-8");
        for (int i = 0; i < bytes.length - searchBytes.length; i++) {
            if (Arrays.equals(bytes, i, i + searchBytes.length,
                                searchBytes, 0, searchBytes.length)) {
                System.arraycopy(replaceBytes, 0, bytes, i, replaceBytes.length);
            }
        }
        Files.write(classes.resolve("module-info.class"), bytes);

        List<String> log = new JavapTask(tb)
                .classpath(classes.toString())
                .options("-bootclasspath", "") // hide all system classes
                .classes("module-info")
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        checkOutput(log, false, "^(Warning|Error)");
        checkOutput(log, true, "class module-info");
    }

    /**
     * Test an invalid class, with this_class == 0.
     */
    @Test
    public void testNoNameClass(Path base) throws Exception {
        Path src = base.resolve("src");
        Path classes = Files.createDirectories(base.resolve("classes"));
        tb.writeJavaFiles(src, "class A { }");

        new JavacTask(tb)
                .outdir(classes.toString())
                .files(tb.findJavaFiles(src))
                .run()
                .writeAll();

        ClassFile cf = ClassFile.read(classes.resolve("A.class"));
        ClassFile cf2 = new ClassFile(
                cf.magic, cf.minor_version, cf.major_version, cf.constant_pool,
                cf.access_flags,
                0, // this_class,
                cf.super_class, cf.interfaces, cf.fields, cf.methods, cf.attributes);
        new ClassWriter().write(cf2, Files.newOutputStream(classes.resolve("Z.class")));

        List<String> log = new JavapTask(tb)
                .classpath(classes.toString())
                .classes("Z")
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        checkOutput(log, true, "Warning:.*Z.class does not contain class Z");
    }

    /**
     * Test a class with unexpected contents.
     * This is the arguably the most common negative case.
     */
    @Test
    public void testWrongNameClass(Path base) throws Exception {
        Path src = base.resolve("src");
        Path classes = Files.createDirectories(base.resolve("classes"));
        tb.writeJavaFiles(src, "class A { }");

        new JavacTask(tb)
                .outdir(classes.toString())
                .files(tb.findJavaFiles(src))
                .run()
                .writeAll();

        Files.move(classes.resolve("A.class"), classes.resolve("B.class"));

        List<String> log = new JavapTask(tb)
                .classpath(classes.toString())
                .classes("B")
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        checkOutput(log, true, "Warning:.*B.class does not contain class B");
    }

    /**
     * Check that the output does, or does not, contain lines matching a regex.
     */
    private void checkOutput(List<String> log, boolean expect, String regex) {
        Pattern p = Pattern.compile(regex);
        List<String> matches = log.stream()
                .filter(line -> p.matcher(line).find())
                .collect(Collectors.toList());
        if (expect) {
            if (matches.isEmpty()) {
                error("expected output not found: " + regex);
            }
        } else {
            if (!matches.isEmpty()) {
                error("unexpected output found: " + matches);
            }
        }
    }
}

