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
 * @bug 8234101
 * @summary Verify that repeating annotations and its processing works for records
 * @library /tools/lib /tools/javac/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.compiler/com.sun.tools.javac.code
 *      jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask
 * @build JavacTestingAbstractProcessor
 * @run main/othervm RepeatingAnnotationsOnRecords
 */

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.ElementScanner14;
import javax.tools.Diagnostic.Kind;
import javax.tools.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;

import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.List;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.Mode;
import toolbox.Task.OutputKind;
import toolbox.TestRunner;
import toolbox.ToolBox;

public class RepeatingAnnotationsOnRecords extends TestRunner {
    protected ToolBox tb;

    RepeatingAnnotationsOnRecords() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        new RepeatingAnnotationsOnRecords().runTests();
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    Path[] findJavaFiles(Path... paths) throws IOException {
        return tb.findJavaFiles(paths);
    }

    static final String SOURCE =
            """
            import java.lang.annotation.*;
            import java.util.*;
            import javax.annotation.processing.*;
            import javax.lang.model.element.*;

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ ElementType.PARAMETER })
            @interface ParameterContainer { Parameter[] value(); }

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ ElementType.PARAMETER })
            @Repeatable(ParameterContainer.class)
            @interface Parameter {}

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ ElementType.METHOD })
            @interface MethodContainer { Method[] value(); }

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ ElementType.METHOD })
            @Repeatable(MethodContainer.class)
            @interface Method {}

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ ElementType.FIELD })
            @interface FieldContainer { Field[] value(); }

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ ElementType.FIELD })
            @Repeatable(FieldContainer.class)
            @interface Field {}

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ ElementType.RECORD_COMPONENT })
            @interface RecComponentContainer { RecComponent[] value(); }

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ ElementType.RECORD_COMPONENT })
            @Repeatable(RecComponentContainer.class)
            @interface RecComponent {}

            @Retention(RetentionPolicy.RUNTIME)
            @interface AllContainer { All[] value(); }

            @Retention(RetentionPolicy.RUNTIME)
            @Repeatable(AllContainer.class)
            @interface All {}

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ ElementType.FIELD, ElementType.RECORD_COMPONENT })
            @interface RecComponentAndFieldContainer { RecComponentAndField[] value(); }

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ ElementType.FIELD, ElementType.RECORD_COMPONENT })
            @Repeatable(RecComponentAndFieldContainer.class)
            @interface RecComponentAndField {}

            record R1(@Parameter @Parameter int i) {}

            record R2(@Method @Method int i) {}

            record R3(@Field @Field int i) {}

            record R4(@All @All int i) {}

            record R5(@RecComponent @RecComponent int i) {}

            record R6(@RecComponentAndField @RecComponentAndField int i) {}
            """;

    @Test
    public void testAnnoProcessing(Path base) throws Exception {
        Path src = base.resolve("src");
        Path r = src.resolve("Records");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(r, SOURCE);

        for (Mode mode : new Mode[] {Mode.API}) {
            new JavacTask(tb, mode)
                    .options("-nowarn", "-processor", Processor.class.getName())
                    .files(findJavaFiles(src))
                    .outdir(classes)
                    .run()
                    .writeAll()
                    .getOutputLines(Task.OutputKind.DIRECT);
        }
    }

    @SupportedAnnotationTypes("*")
    public static final class Processor extends JavacTestingAbstractProcessor {
        public boolean process(Set<? extends TypeElement> tes, RoundEnvironment renv) {
            for (TypeElement te : tes) {
                switch (te.toString()) {
                    case "ParameterContainer" :
                        checkElements(te, renv, 1, Set.of(ElementKind.PARAMETER), "Parameter");
                        break;
                    case "MethodContainer":
                        checkElements(te, renv, 1, Set.of(ElementKind.METHOD), "Method");
                        break;
                    case "FieldContainer":
                        checkElements(te, renv, 1, Set.of(ElementKind.FIELD), "Field");
                        break;
                    case "AllContainer":
                        checkElements(te, renv, 4,
                                Set.of(ElementKind.FIELD,
                                        ElementKind.METHOD,
                                        ElementKind.PARAMETER,
                                        ElementKind.RECORD_COMPONENT), "All");
                        break;
                    case "RecComponentContainer":
                        checkElements(te, renv, 1, Set.of(ElementKind.RECORD_COMPONENT), "RecComponent");
                        break;
                    case "RecComponentAndFieldContainer":
                        checkElements(te, renv, 2, Set.of(ElementKind.RECORD_COMPONENT, ElementKind.FIELD),
                                "RecComponentAndField");
                        break;
                    default:
                        // ignore, just another annotation like Target, we don't care about
                }
            }
            return true;
        }

        void checkElements(TypeElement te,
                           RoundEnvironment renv,
                           int numberOfElementsAppliedTo,
                           Set<ElementKind> kinds,
                           String nameOfRepeatableAnno) {
            Set<? extends Element> annotatedElements = renv.getElementsAnnotatedWith(te);
            Assert.check(annotatedElements.size() == numberOfElementsAppliedTo, "for type element " + te + " expected = " + numberOfElementsAppliedTo + " found = " + annotatedElements.size());
            for (Element e : annotatedElements) {
                Symbol s = (Symbol) e;
                Assert.check(kinds.contains(s.getKind()));
                java.util.List<? extends AnnotationMirror> annoMirrors = e.getAnnotationMirrors();
                Assert.check(annoMirrors.size() == 1, "there must be only one annotation container");
                AnnotationMirror annotationMirror = annoMirrors.get(0);
                Map<? extends ExecutableElement, ? extends AnnotationValue> map = annotationMirror.getElementValues();
                // as we are dealing with a container that contains two annotations, its value is a list of
                // contained annotations
                List<? extends AnnotationMirror> containedAnnotations = (List<? extends AnnotationMirror>) map.values().iterator().next().getValue();
                Assert.check(containedAnnotations.size() == 2, "there can only be two repeated annotations");
                for (AnnotationMirror annoMirror : containedAnnotations) {
                    Assert.check(annoMirror.getAnnotationType().toString().equals(nameOfRepeatableAnno),
                            "incorrect declared type for contained annotation");
                }
            }
        }
    }
}
