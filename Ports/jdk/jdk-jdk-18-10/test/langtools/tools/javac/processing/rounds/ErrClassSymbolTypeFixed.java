/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Red Hat, Inc.
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
 * @bug 8256809
 * @summary Verify that erroneous symbols have their type fixed between rounds
 * @library /tools/lib /tools/javac/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.JarTask toolbox.TestRunner
 *      JavacTestingAbstractProcessor
 * @run main ErrClassSymbolTypeFixed
 */

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import toolbox.JavacTask;
import toolbox.JarTask;
import toolbox.Task;
import toolbox.TestRunner;
import toolbox.ToolBox;

public class ErrClassSymbolTypeFixed extends TestRunner {

    private static final String A_JAVA = "package t1; public @interface A {}";
    private static final String B_JAVA = "package t2; public class B {}";
    private static final String C_JAVA = "package t3; import t2.B; public class C extends B {}";
    private static final String D_JAVA = "import t1.A; import t3.C; import t2.B; @A public class D {}";

    private ToolBox tb;

    public ErrClassSymbolTypeFixed() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        new ErrClassSymbolTypeFixed().runTests();
    }

    @Test
    public void testErrorFixed() throws Exception {
        Path base = Paths.get(".");
        Path src1 = base.resolve("src1");
        Path src2 = base.resolve("src2");
        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        /*
         *  Create and compile the following:
         *  A: annotation type
         *  B: basic class
         *  C: subclass of B
         */
        tb.writeJavaFiles(src1, A_JAVA, B_JAVA, C_JAVA);

        new JavacTask(tb)
        .outdir(classes)
        .files(tb.findJavaFiles(src1))
        .run()
        .writeAll()
        .getOutput(Task.OutputKind.DIRECT);

        // Create a Jar containing only C to include later in the classpath
        Path jarPath = base.resolve("test.jar");
        new JarTask(tb, jarPath)
        .baseDir(classes)
        .files("t3")
        .run()
        .writeAll()
        .getOutput(Task.OutputKind.DIRECT);

        // Delete B from the classpath
        Files.delete(classes.resolve("t2").resolve("B.class"));

        /*
         *  Write and compile D, which has the following properties:
         *  - is annotated with A, causing our processor to regenerate B
         *  - imports C before B
         */
        tb.writeJavaFiles(src2, D_JAVA);

        // If the erroneous ClassSymbol is not reset between rounds,
        // a NullPointerException will occur later during flow analysis.
        new JavacTask(tb)
        .classpath(classes, jarPath)
        .options("-processor", ErrClassSymbolProcessor.class.getName(),
                "--processor-path", System.getProperty("test.class.path"))
        .outdir(classes)
        .files(tb.findJavaFiles(src2))
        .run()
        .writeAll()
        .getOutput(Task.OutputKind.DIRECT);
    }

    @SupportedAnnotationTypes("t1.A")
    public static class ErrClassSymbolProcessor extends JavacTestingAbstractProcessor {

        @Override
        public final boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            for (TypeElement te : ElementFilter.typesIn(roundEnv.getRootElements())) {
                createFile(te);
            }
            return true;
        }

        private void createFile(TypeElement te) {
            // Generate B.java when the processor reads the @A annotation on D
            if ("D".equals(te.getSimpleName().toString())) {
                try {
                    JavaFileObject fo = processingEnv.getFiler().createSourceFile("B");
                    try (Writer out = fo.openWriter()) {
                        out.write(B_JAVA);
                    }
                } catch (IOException e) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "problem writing file: " + e);
                }
            }
        }
    }

}
