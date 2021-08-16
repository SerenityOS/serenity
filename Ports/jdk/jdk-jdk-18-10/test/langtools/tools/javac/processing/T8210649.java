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

/**
 * @test
 * @bug 8210649
 * @summary Check that diagnostics can be printed even after the compilation
 *          stopped.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.TestRunner toolbox.ToolBox T8210649
 * @run main T8210649
 */

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;
import javax.tools.DiagnosticListener;
import toolbox.JavacTask;
import toolbox.TestRunner;
import toolbox.TestRunner.Test;
import toolbox.ToolBox;

public class T8210649 extends TestRunner {

    public static void main(String... args) throws Exception {
        new T8210649().runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    private final ToolBox tb = new ToolBox();

    public T8210649() {
        super(System.err);
    }

    @Test
    public void testErrorsAfter(Path outerBase) throws Exception {
        Path libSrc = outerBase.resolve("libsrc");
        tb.writeJavaFiles(libSrc,
                          "package lib;\n" +
                          "@lib2.Helper(1)\n" +
                          "public @interface Lib {\n" +
                          "}",
                          "package lib2;\n" +
                          "public @interface Helper {\n" +
                          "    public int value() default 0;\n" +
                          "}");
        Path libClasses = outerBase.resolve("libclasses");
        Files.createDirectories(libClasses);
        new JavacTask(tb)
                .outdir(libClasses.toString())
                .files(tb.findJavaFiles(libSrc))
                .run()
                .writeAll();
        Files.delete(libClasses.resolve("lib2").resolve("Helper.class"));
        Path src = outerBase.resolve("src");
        tb.writeJavaFiles(src,
                          "package t;\n" +
                          "import lib.Lib;\n" +
                          "public class T {\n" +
                          "  Undefined<String> undef() {}\n" +
                          "}");
        Path classes = outerBase.resolve("classes");
        Files.createDirectories(classes);
        Path tTarget = classes.resolve("t").resolve("T.class");
        Files.createDirectories(tTarget.getParent());
        try (OutputStream in = Files.newOutputStream(tTarget)) {}
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager sjfm = compiler.getStandardFileManager(null, null, null)) {
            Consumer<DiagnosticListener<JavaFileObject>> runCompiler = dl -> {
                try {
                    List<String> options = List.of("-processor", "T8210649$P",
                                                   "-processorpath", System.getProperty("test.classes"),
                                                   "-classpath", libClasses.toString() + ":" + classes.toString(),
                                                   "-d", classes.toString());
                    ToolProvider.getSystemJavaCompiler()
                                .getTask(null, null, dl, options, null, sjfm.getJavaFileObjects(tb.findJavaFiles(src)))
                                .call();
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            };

            List<String> expected = new ArrayList<>();
            runCompiler.accept(d -> expected.add(d.getMessage(null)));

            DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<>();
            runCompiler.accept(dc);

            List<String> actual = dc.getDiagnostics()
                                    .stream()
                                    .map(d -> d.getMessage(null))
                                    .collect(Collectors.toList());
            if (!expected.equals(actual)) {
                throw new IllegalStateException("Unexpected output: " + actual);
            }
        }
    }

    @SupportedAnnotationTypes("*")
    public static class P extends AbstractProcessor {
        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }
    }

}
