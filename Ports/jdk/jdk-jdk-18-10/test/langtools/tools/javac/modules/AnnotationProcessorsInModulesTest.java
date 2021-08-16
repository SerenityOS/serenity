/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Verify that annotation processors inside modules works
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main AnnotationProcessorsInModulesTest
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import toolbox.JavacTask;
import toolbox.Task;

public class AnnotationProcessorsInModulesTest extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new AnnotationProcessorsInModulesTest().runTests();
    }

    private static final String annotationProcessorModule1 =
            """
                module anno_proc1x {
                    requires java.compiler;

                    provides javax.annotation.processing.Processor
                      with mypkg1.MyProcessor1;
                }""";

    private static final String annotationProcessorModule2 =
            """
                module anno_proc2x {
                    requires java.compiler;

                    provides javax.annotation.processing.Processor
                      with mypkg2.MyProcessor2;
                }""";

    private static final String annotationProcessor1 =
            """
                package mypkg1;

                import javax.annotation.processing.AbstractProcessor;
                import javax.annotation.processing.RoundEnvironment;
                import javax.annotation.processing.SupportedAnnotationTypes;
                import javax.lang.model.SourceVersion;
                import javax.lang.model.element.*;

                import java.util.*;

                @SupportedAnnotationTypes("*")
                public final class MyProcessor1 extends AbstractProcessor {

                    @Override
                    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
                        return false;
                    }

                    @Override
                    public SourceVersion getSupportedSourceVersion() {
                        System.out.println("the annotation processor 1 is working!");
                        return SourceVersion.latest();
                    }
                }""";

    private static final String annotationProcessor2 =
            """
                package mypkg2;

                import javax.annotation.processing.AbstractProcessor;
                import javax.annotation.processing.RoundEnvironment;
                import javax.annotation.processing.SupportedAnnotationTypes;
                import javax.lang.model.SourceVersion;
                import javax.lang.model.element.*;

                import java.util.*;

                @SupportedAnnotationTypes("*")
                public final class MyProcessor2 extends AbstractProcessor {

                    @Override
                    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
                        return false;
                    }

                    @Override
                    public SourceVersion getSupportedSourceVersion() {
                        System.out.println("the annotation processor 2 is working!");
                        return SourceVersion.latest();
                    }
                }""";

    private static final String testClass = "class Test{}";

    void initialization(Path base) throws Exception {
        moduleSrc = base.resolve("anno_proc_src");
        Path anno_proc1 = moduleSrc.resolve("anno_proc1x");
        Path anno_proc2 = moduleSrc.resolve("anno_proc2x");

        processorCompiledModules = base.resolve("mods");

        Files.createDirectories(processorCompiledModules);

        tb.writeJavaFiles(
                anno_proc1,
                annotationProcessorModule1,
                annotationProcessor1);

        tb.writeJavaFiles(
                anno_proc2,
                annotationProcessorModule2,
                annotationProcessor2);

        String log = new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString())
                .outdir(processorCompiledModules)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.isEmpty()) {
            throw new AssertionError("Unexpected output: " + log);
        }

        classes = base.resolve("classes");
        Files.createDirectories(classes);
    }

    Path processorCompiledModules;
    Path moduleSrc;
    Path classes;

    @Test
    public void testUseOnlyOneProcessor(Path base) throws Exception {
        initialization(base);
        String log = new JavacTask(tb)
                .options("--processor-module-path", processorCompiledModules.toString(),
                        "-processor", "mypkg2.MyProcessor2")
                .outdir(classes)
                .sources(testClass)
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.STDOUT);
        if (!log.trim().equals("the annotation processor 2 is working!")) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

    @Test
    public void testAnnotationProcessorExecutionOrder(Path base) throws Exception {
        initialization(base);
        List<String> log = new JavacTask(tb)
                .options("--processor-module-path", processorCompiledModules.toString(),
                        "-processor", "mypkg1.MyProcessor1,mypkg2.MyProcessor2")
                .outdir(classes)
                .sources(testClass)
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.STDOUT);
        if (!log.equals(Arrays.asList("the annotation processor 1 is working!",
                                      "the annotation processor 2 is working!"))) {
            throw new AssertionError("Unexpected output: " + log);
        }

        log = new JavacTask(tb)
                .options("--processor-module-path", processorCompiledModules.toString(),
                        "-processor", "mypkg2.MyProcessor2,mypkg1.MyProcessor1")
                .outdir(classes)
                .sources(testClass)
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.STDOUT);
        if (!log.equals(Arrays.asList("the annotation processor 2 is working!",
                                      "the annotation processor 1 is working!"))) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

    @Test
    public void testErrorOutputIfOneProcessorNameIsIncorrect(Path base) throws Exception {
        initialization(base);
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--processor-module-path", processorCompiledModules.toString(),
                         "-processor", "mypkg2.MyProcessor2,noPackage.noProcessor,mypkg1.MyProcessor1")
                .outdir(classes)
                .sources(testClass)
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.STDOUT, Task.OutputKind.DIRECT).toString();
        if (!log.trim().equals("[the annotation processor 2 is working!, - compiler.err.proc.processor.not.found: noPackage.noProcessor, 1 error]")) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

    @Test
    public void testOptionsExclusion(Path base) throws Exception {
        initialization(base);
        List<String> log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                        "--processor-module-path", processorCompiledModules.toString(),
                        "--processor-path", processorCompiledModules.toString())
                .outdir(classes)
                .sources(testClass)
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        if (!log.equals(Arrays.asList("- compiler.err.processorpath.no.processormodulepath"))) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }
}
