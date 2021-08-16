/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8246774
 * @summary Verify that annotation processing works for records
 * @library /tools/lib /tools/javac/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask JavacTestingAbstractProcessor
 * @run main JavaxLangModelForRecords
 */

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.ElementScanner14;
import javax.tools.Diagnostic.Kind;
import javax.tools.*;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.Mode;
import toolbox.Task.OutputKind;
import toolbox.TestRunner;
import toolbox.ToolBox;

public class JavaxLangModelForRecords extends TestRunner {
    protected ToolBox tb;

    JavaxLangModelForRecords() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        new JavaxLangModelForRecords().runTests();
    }

    /**
     * Run all methods annotated with @Test, and throw an exception if any
     * errors are reported..
     *
     * @throws Exception if any errors occurred
     */
    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    Path[] findJavaFiles(Path... paths) throws IOException {
        return tb.findJavaFiles(paths);
    }

    void checkOutputContains(String log, String... expect) throws Exception {
        for (String e : expect) {
            if (!log.contains(e)) {
                throw new Exception("expected output not found: " + e);
            }
        }
    }

    @Test
    public void testQualifiedClassForProcessing(Path base) throws Exception {
        Path src = base.resolve("src");
        Path r = src.resolve("R");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(r,
                "record R(int i) {}");

        List<String> expected = List.of(
                "Note: field: i",
                "Note: record component: i");

        for (Mode mode : new Mode[] {Mode.API}) {
            List<String> log = new JavacTask(tb, mode)
                    .options("-processor", QualifiedClassForProcessing.class.getName())
                    .files(findJavaFiles(src))
                    .outdir(classes)
                    .run()
                    .writeAll()
                    .getOutputLines(Task.OutputKind.DIRECT);

            if (!expected.equals(log))
                throw new AssertionError("Unexpected output: " + log);
        }
    }

    @SupportedAnnotationTypes("*")
    public static final class QualifiedClassForProcessing extends JavacTestingAbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (processingEnv.getElementUtils().getTypeElement("R") == null) {
                throw new AssertionError("No R record found.");
            }

            Messager messager = processingEnv.getMessager();

            for (TypeElement clazz : ElementFilter.typesIn(roundEnv.getRootElements())) {
                for (VariableElement field : ElementFilter.fieldsIn(clazz.getEnclosedElements())) {
                    messager.printMessage(Kind.NOTE, "field: " + field.getSimpleName());
                }
                for (RecordComponentElement rc : ElementFilter.recordComponentsIn(clazz.getEnclosedElements())) {
                    messager.printMessage(Kind.NOTE, "record component: " + rc.getSimpleName());
                }
            }

            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }
    }
}
