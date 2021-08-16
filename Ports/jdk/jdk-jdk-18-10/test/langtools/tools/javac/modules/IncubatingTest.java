/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8171177
 * @summary Verify that ModuleResolution attribute flags are honored.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask toolbox.JavapTask ModuleTestBase
 * @run main IncubatingTest
 */

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.Attributes;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ClassWriter;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Utf8_info;
import com.sun.tools.classfile.ConstantPool.CPInfo;
import com.sun.tools.classfile.ModuleResolution_attribute;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.Expect;

public class IncubatingTest extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new IncubatingTest().runTests();
    }

    @Test
    public void testDoNotResolve(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "module jdk.i { exports api; }",
                          "package api; public class Api { }");
        Path classes = base.resolve("classes");
        Files.deleteIfExists(classes);
        Path iClasses = classes.resolve("jdk.i");
        tb.createDirectories(iClasses);

        new JavacTask(tb)
                .outdir(iClasses)
                .files(findJavaFiles(src))
                .run()
                .writeAll();

        copyJavaBase(classes);

        Path jdkIModuleInfo = iClasses.resolve("module-info.class");
        addModuleResolutionAttribute(jdkIModuleInfo, ModuleResolution_attribute.DO_NOT_RESOLVE_BY_DEFAULT);

        Path testSrc = base.resolve("test-src");
        tb.writeJavaFiles(testSrc,
                          "class T { api.Api api; }");
        Path testClasses = base.resolve("test-classes");
        tb.createDirectories(testClasses);

        List<String> log;
        List<String> expected;

        log = new JavacTask(tb)
                .options("--system", "none",
                         "--upgrade-module-path", classes.toString(),
                         "-XDrawDiagnostics")
                .outdir(testClasses)
                .files(findJavaFiles(testSrc))
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        expected = Arrays.asList(
                "T.java:1:11: compiler.err.package.not.visible: api, (compiler.misc.not.def.access.does.not.read.from.unnamed: api, jdk.i)",
                "1 error"
        );

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }

        log = new JavacTask(tb)
                .options("--system", "none",
                         "--upgrade-module-path", classes.toString(),
                         "--add-modules", "ALL-SYSTEM",
                         "-XDrawDiagnostics")
                .outdir(testClasses)
                .files(findJavaFiles(testSrc))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        expected = Arrays.asList("");

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }

        new JavacTask(tb)
                .options("--system", "none",
                         "--upgrade-module-path", classes.toString(),
                         "--add-modules", "jdk.i")
                .outdir(testClasses)
                .files(findJavaFiles(testSrc))
                .run()
                .writeAll();

        Path testModuleSrc = base.resolve("test-module-src");
        tb.writeJavaFiles(testModuleSrc,
                          "module test { requires jdk.i; }", //explicit requires of an incubating module
                          "class T { api.Api api; }");
        Path testModuleClasses = base.resolve("test-module-classes");
        tb.createDirectories(testModuleClasses);

        new JavacTask(tb)
                .options("--system", "none",
                         "--upgrade-module-path", classes.toString())
                .outdir(testModuleClasses)
                .files(findJavaFiles(testModuleSrc))
                .run()
                .writeAll();
    }

    @Test
    public void testIncubating(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "module jdk.i { exports api; }",
                          "package api; public class Api { }");
        Path classes = base.resolve("classes");
        Files.deleteIfExists(classes);
        Path iClasses = classes.resolve("jdk.i");
        tb.createDirectories(iClasses);

        new JavacTask(tb)
                .outdir(iClasses)
                .files(findJavaFiles(src))
                .run()
                .writeAll();

        Path jdkIModuleInfo = iClasses.resolve("module-info.class");
        addModuleResolutionAttribute(jdkIModuleInfo, ModuleResolution_attribute.WARN_INCUBATING);

        Path testSrc = base.resolve("test-src");
        tb.writeJavaFiles(testSrc,
                          "class T { api.Api api; }");
        Path testClasses = base.resolve("test-classes");
        tb.createDirectories(testClasses);

        List<String> log;
        List<String> expected;

        log = new JavacTask(tb)
                .options("--module-path", classes.toString(),
                         "--add-modules", "jdk.i",
                         "-XDrawDiagnostics",
                         "-Werror")
                .outdir(testClasses)
                .files(findJavaFiles(testSrc))
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        expected = Arrays.asList(
                "- compiler.warn.incubating.modules: jdk.i",
                "- compiler.err.warnings.and.werror",
                "1 error",
                "1 warning"
        );

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }

        Path testModuleSrc = base.resolve("test-module-src");
        tb.writeJavaFiles(testModuleSrc,
                          "module test { requires jdk.i; }", //explicit requires of an incubating module
                          "class T { api.Api api; }");
        Path testModuleClasses = base.resolve("test-module-classes");
        tb.createDirectories(testModuleClasses);

        log = new JavacTask(tb)
                .options("--module-path", classes.toString(),
                         "-XDrawDiagnostics",
                         "-Werror")
                .outdir(testModuleClasses)
                .files(findJavaFiles(testModuleSrc))
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        expected = Arrays.asList(
                "- compiler.warn.incubating.modules: jdk.i",
                "- compiler.err.warnings.and.werror",
                "1 error",
                "1 warning"
        );

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

    private void copyJavaBase(Path targetDir) throws IOException {
        FileSystem jrt = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path javaBase = jrt.getPath("modules", "java.base");

        if (!Files.exists(javaBase)) {
            throw new AssertionError("No java.base?");
        }

        Path javaBaseClasses = targetDir.resolve("java.base");

        for (Path clazz : tb.findFiles("class", javaBase)) {
            Path target = javaBaseClasses.resolve(javaBase.relativize(clazz).toString());
            Files.createDirectories(target.getParent());
            Files.copy(clazz, target);
        }
    }

    private void addModuleResolutionAttribute(Path classfile, int resolution_flags) throws Exception {
        ClassFile cf = ClassFile.read(classfile);
        Attributes attrs = cf.attributes;
        List<CPInfo> cpData = new ArrayList<>();
        cpData.add(null);
        for (CPInfo info : cf.constant_pool.entries()) {
            cpData.add(info);
            if (info.size() == 2)
                cpData.add(null);
        }
        cpData.add(new CONSTANT_Utf8_info(Attribute.ModuleResolution));
        ConstantPool newCP = new ConstantPool(cpData.toArray(new CPInfo[0]));
        ModuleResolution_attribute res = new ModuleResolution_attribute(newCP, resolution_flags);
        Map<String, Attribute> newAttributeMap = new HashMap<>(attrs.map);
        newAttributeMap.put(Attribute.ModuleResolution, res);
        Attributes newAttrs = new Attributes(newAttributeMap);
        ClassFile newCF = new ClassFile(cf.magic,
                                        cf.minor_version,
                                        cf.major_version,
                                        newCP,
                                        cf.access_flags,
                                        cf.this_class,
                                        cf.super_class,
                                        cf.interfaces,
                                        cf.fields,
                                        cf.methods,
                                        newAttrs);
        try (OutputStream out = Files.newOutputStream(classfile)) {
            new ClassWriter().write(newCF, out);
        }
    }
}
