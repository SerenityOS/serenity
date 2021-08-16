/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Verify that annotation processing works for records
 * @library /tools/lib /tools/javac/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.compiler/com.sun.tools.javac.code
 *      jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask
 * @build JavacTestingAbstractProcessor
 * @compile CheckingAccessorsOnLoadedRecordClasses.java
 * @run main/othervm CheckingAccessorsOnLoadedRecordClasses
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

import java.lang.annotation.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.tools.Diagnostic.Kind;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sun.tools.javac.util.Assert;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.Mode;
import toolbox.Task.OutputKind;
import toolbox.TestRunner;
import toolbox.ToolBox;

public class CheckingAccessorsOnLoadedRecordClasses extends TestRunner {
    protected ToolBox tb;

    CheckingAccessorsOnLoadedRecordClasses() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        new CheckingAccessorsOnLoadedRecordClasses().runTests();
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    Path[] findJavaFiles(Path... paths) throws IOException {
        return tb.findJavaFiles(paths);
    }

    static final String RecordSrc =
            """
            package pkg1;
            import java.util.List;
            public record R(List<String> data) {}
            """;

    static final String ISrc =
            """
            package pkg2;
            import pkg1.R;

            @FunctionalInterface
            public interface I {
                void foo(R r);
            }
            """;

    @Test
    public void testAnnoProcessing(Path base) throws Exception {
        Path src = base.resolve("src");
        Path out = base.resolve("out");
        Files.createDirectories(out);
        Path pkg1 = src.resolve("pkg1");

        tb.writeJavaFiles(src, RecordSrc);
        // lets first compile the record
        new JavacTask(tb)
                .files(findJavaFiles(pkg1))
                .outdir(out)
                .run();

        Path pkg2 = src.resolve("pkg2");
        tb.writeJavaFiles(src, ISrc);
        /* now the annotated interface which uses the record, given that the record class
         * is now in the classpath, we will force jvm.ClassReader to load it and set the
         * accessors correctly
         */
        new JavacTask(tb, Mode.API)
                .options("-nowarn", "-processor", Processor.class.getName())
                .classpath(out)
                .files(findJavaFiles(pkg2))
                .outdir(out)
                .run();
    }

    /** This processor will look for records in the arguments of the methods annotated with any
     *  annotation for a given source. Then it will check that those records have at least one
     *  record component and that the accessor associated with it is not null and that it has the
     *  same name as its corresponding record component
     */
    @SupportedAnnotationTypes("*")
    public static final class Processor extends JavacTestingAbstractProcessor {
        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (!roundEnv.processingOver()) {
                for (TypeElement annotation : annotations) {
                    Set<? extends Element> annotatedElems = roundEnv.getElementsAnnotatedWith(annotation);
                    for (Element annotatedElement : annotatedElems) {
                        TypeElement typeElement = (TypeElement) annotatedElement;

                        for (Element enclosedElement : typeElement.getEnclosedElements()) {
                            if (enclosedElement.getKind() == ElementKind.METHOD) {
                                validateMethod((ExecutableElement) enclosedElement, roundEnv);
                            }
                        }
                    }
                }
            }
            return false;
        }

        protected void validateMethod(ExecutableElement method, RoundEnvironment roundEnv) {
            for (VariableElement parameter : method.getParameters()) {
                TypeMirror parameterType = parameter.asType();
                if (parameterType.getKind() == TypeKind.DECLARED) {
                    Element parameterElement = ((DeclaredType) parameterType).asElement();
                    if (parameterElement.getKind() == ElementKind.RECORD) {
                        validateRecord((TypeElement) parameterElement, roundEnv);
                    }
                }
            }
        }

        protected void validateRecord(TypeElement recordElement, RoundEnvironment roundEnv) {
            List<? extends RecordComponentElement> recordComponents = recordElement.getRecordComponents();

            if (recordComponents.isEmpty()) {
                processingEnv.getMessager()
                        .printMessage(Diagnostic.Kind.ERROR, "Record element " + recordElement.getQualifiedName()
                                + " has no record components");
            } else {
                for (RecordComponentElement recordComponent : recordComponents) {
                    ExecutableElement accessor = recordComponent.getAccessor();
                    if (accessor == null) {
                        processingEnv.getMessager()
                                .printMessage(Diagnostic.Kind.ERROR,
                                        "Record component " + recordComponent.getSimpleName() + " from record " + recordElement
                                                .getQualifiedName() + " has no accessor");
                    }
                    if (!accessor.getSimpleName().equals(recordComponent.getSimpleName())) {
                        processingEnv.getMessager()
                                .printMessage(Diagnostic.Kind.ERROR,
                                        "Record component " + recordComponent.getSimpleName() + " from record " +
                                                recordElement.getQualifiedName() + " has an accessor with name " + accessor.getSimpleName());
                    }
                }
            }
        }
    }
}
