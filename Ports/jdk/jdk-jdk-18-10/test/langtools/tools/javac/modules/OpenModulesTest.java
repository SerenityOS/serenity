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
 * @summary tests for multi-module mode compilation
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.ModuleBuilder ModuleTestBase
 * @run main OpenModulesTest
 */

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.Attributes;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ClassWriter;
import com.sun.tools.classfile.Module_attribute;
import toolbox.JavacTask;
import toolbox.JavapTask;
import toolbox.Task;
import toolbox.Task.Expect;
import toolbox.Task.OutputKind;

public class OpenModulesTest extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new OpenModulesTest().runTests();
    }

    @Test
    public void testStrongModule(Path base) throws Exception {
        Path m1 = base.resolve("m1x");
        tb.writeJavaFiles(m1,
                          "module m1x { exports api1; opens api2; }",
                          "package api1; public class Api1 {}",
                          "package api2; public class Api2 {}",
                          "package impl; public class Impl {}");
        Path classes = base.resolve("classes");
        Path m1Classes = classes.resolve("m1x");
        tb.createDirectories(m1Classes);

        String log = new JavacTask(tb)
                .outdir(m1Classes)
                .files(findJavaFiles(m1))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.isEmpty())
            throw new Exception("expected output not found: " + log);

        String decompiled = new JavapTask(tb)
                .options("--system", "none", "-bootclasspath", "")
                .classes(m1Classes.resolve("module-info.class").toString())
                .run()
                .writeAll()
                .getOutput(OutputKind.DIRECT)
                .replace(System.getProperty("line.separator"), "\n")
                .replaceAll("@[^;]*;", ";");

        String expected = """
            module m1x {
              requires java.base;
              exports api1;
              opens api2;
            }""";

        if (!decompiled.contains(expected)) {
            throw new Exception("expected output not found: " + decompiled);
        }

        //compiling against a strong module read from binary:
        Path m2 = base.resolve("m2x");
        tb.writeJavaFiles(m2,
                          "module m2x { requires m1x; }",
                          "package test; public class Test { api1.Api1 a1; api2.Api2 a2; }");
        Path m2Classes = classes.resolve("m2x");
        tb.createDirectories(m2Classes);

        List<String> log2 = new JavacTask(tb)
                .options("--module-path", m1Classes.toString(),
                         "-XDrawDiagnostics")
                .outdir(m2Classes)
                .files(findJavaFiles(m2))
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected2 = Arrays.asList("Test.java:1:49: compiler.err.package.not.visible: api2, (compiler.misc.not.def.access.not.exported: api2, m1x)",
                                               "1 error");
        if (!Objects.equals(log2, expected2))
            throw new Exception("expected output not found: " + log2);
    }

    @Test
    public void testOpenModule(Path base) throws Exception {
        Path m1 = base.resolve("m1x");
        tb.writeJavaFiles(m1,
                          "open module m1x { exports api1; }",
                          "package api1; public class Api1 {}",
                          "package api2; public class Api2 {}",
                          "package impl; public class Impl {}");
        Path classes = base.resolve("classes");
        Path m1Classes = classes.resolve("m1x");
        tb.createDirectories(m1Classes);

        String log = new JavacTask(tb)
                .outdir(m1Classes)
                .files(findJavaFiles(m1))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.isEmpty())
            throw new Exception("expected output not found: " + log);

        String decompiled = new JavapTask(tb)
                .options("--system", "none", "-bootclasspath", "")
                .classes(m1Classes.resolve("module-info.class").toString())
                .run()
                .writeAll()
                .getOutput(OutputKind.DIRECT)
                .replace(System.getProperty("line.separator"), "\n")
                .replaceAll("@[^;]*;", ";");

        String expected = """
                open module m1x {
                  requires java.base;
                  exports api1;
                }""";

        if (!decompiled.contains(expected)) {
            throw new Exception("expected output not found: " + decompiled);
        }

        //compiling against a ordinary module read from binary:
        Path m2 = base.resolve("m2x");
        tb.writeJavaFiles(m2,
                          "module m2x { requires m1x; }",
                          "package test; public class Test { api1.Api1 a1; api2.Api2 a2; }");
        Path m2Classes = classes.resolve("m2x");
        tb.createDirectories(m2Classes);

        List<String> log2 = new JavacTask(tb)
                .options("--module-path", m1Classes.toString(),
                         "-XDrawDiagnostics")
                .outdir(m2Classes)
                .files(findJavaFiles(m2))
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected2 = Arrays.asList("Test.java:1:49: compiler.err.package.not.visible: api2, (compiler.misc.not.def.access.not.exported: api2, m1x)",
                                               "1 error");
        if (!Objects.equals(log2, expected2))
            throw new Exception("expected output not found: " + log2);
    }

    @Test
    public void testOpenModuleNoOpens(Path base) throws Exception {
        Path m1 = base.resolve("m1x");
        tb.writeJavaFiles(m1,
                          "open module m1x { exports api1; opens api2; }",
                          "package api1; public class Api1 {}",
                          "package api2; public class Api2 {}",
                          "package impl; public class Impl {}");
        Path classes = base.resolve("classes");
        Path m1Classes = classes.resolve("m1x");
        tb.createDirectories(m1Classes);

        List<String> log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .outdir(m1Classes)
                .files(findJavaFiles(m1))
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected = Arrays.asList("module-info.java:1:33: compiler.err.no.opens.unless.strong",
                                              "1 error");

        if (!expected.equals(log))
            throw new Exception("expected output not found: " + log);

    }

    @Test
    public void testNonZeroOpensInOpen(Path base) throws Exception {
        Path m1 = base.resolve("m1x");
        tb.writeJavaFiles(m1,
                          "module m1x { opens api; }",
                          "package api; public class Api {}");
        Path classes = base.resolve("classes");
        Path m1Classes = classes.resolve("m1x");
        tb.createDirectories(m1Classes);

        new JavacTask(tb)
            .options("-XDrawDiagnostics")
            .outdir(m1Classes)
            .files(findJavaFiles(m1))
            .run(Expect.SUCCESS)
            .writeAll();

        Path miClass = m1Classes.resolve("module-info.class");
        ClassFile cf = ClassFile.read(miClass);
        Module_attribute module = (Module_attribute) cf.attributes.map.get(Attribute.Module);
        Module_attribute newModule = new Module_attribute(module.attribute_name_index,
                                                          module.module_name,
                                                          module.module_flags | Module_attribute.ACC_OPEN,
                                                          module.module_version_index,
                                                          module.requires,
                                                          module.exports,
                                                          module.opens,
                                                          module.uses_index,
                                                          module.provides);
        Map<String, Attribute> attrs = new HashMap<>(cf.attributes.map);

        attrs.put(Attribute.Module, newModule);

        Attributes newAttributes = new Attributes(attrs);
        ClassFile newClassFile = new ClassFile(cf.magic,
                                               cf.minor_version,
                                               cf.major_version,
                                               cf.constant_pool,
                                               cf.access_flags,
                                               cf.this_class,
                                               cf.super_class,
                                               cf.interfaces,
                                               cf.fields,
                                               cf.methods,
                                               newAttributes);

        try (OutputStream out = Files.newOutputStream(miClass)) {
            new ClassWriter().write(newClassFile, out);
        }

        Path test = base.resolve("test");
        tb.writeJavaFiles(test,
                          "package impl; public class Impl extends api.Api {}");
        Path testClasses = base.resolve("test-classes");
        tb.createDirectories(testClasses);

        List<String> log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--module-path", classes.toString(),
                         "--add-modules", "m1x")
                .outdir(testClasses)
                .files(findJavaFiles(test))
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected = Arrays.asList(
                "- compiler.err.cant.access: m1x.module-info, "
                        + "(compiler.misc.bad.class.file.header: module-info.class, "
                        + "(compiler.misc.module.non.zero.opens: m1x))",
                                              "1 error");

        if (!expected.equals(log))
            throw new Exception("expected output not found: " + log);
    }

}
