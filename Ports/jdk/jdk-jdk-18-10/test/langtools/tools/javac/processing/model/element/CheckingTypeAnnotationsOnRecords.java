/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @run main/othervm CheckingTypeAnnotationsOnRecords
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

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;

import com.sun.tools.javac.util.Assert;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.Mode;
import toolbox.Task.OutputKind;
import toolbox.TestRunner;
import toolbox.ToolBox;

public class CheckingTypeAnnotationsOnRecords extends TestRunner {
    protected ToolBox tb;

    CheckingTypeAnnotationsOnRecords() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        new CheckingTypeAnnotationsOnRecords().runTests();
    }

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

    static final String SOURCE =
            """
            import java.lang.annotation.*;
            import java.util.*;
            import javax.annotation.processing.*;
            import javax.lang.model.element.*;

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ ElementType.TYPE_USE })
            @interface TypeUse {}

            record R1(@TypeUse int annotated) {}

            record R2(@TypeUse String annotated) {}

            record R3(@TypeUse String... annotated) {}

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ ElementType.TYPE_PARAMETER })
            @interface TypeParameter {}

            record R4<@TypeParameter T>(T t) {}
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

        static private final Map<String, String> recordNameExpectedAnnotationMap = new HashMap<>();
        static private final Map<String, Integer> recordNameExpectedAnnotationNumberMap = new HashMap<>();
        static {
            recordNameExpectedAnnotationMap.put("R1", "@TypeUse");
            recordNameExpectedAnnotationMap.put("R2", "@TypeUse");
            recordNameExpectedAnnotationMap.put("R3", "@TypeUse");
            recordNameExpectedAnnotationMap.put("R4", "@TypeParameter");

            recordNameExpectedAnnotationNumberMap.put("R1", 4);
            recordNameExpectedAnnotationNumberMap.put("R2", 4);
            recordNameExpectedAnnotationNumberMap.put("R3", 4);
            recordNameExpectedAnnotationNumberMap.put("R4", 1);
        }

        @Override
        public boolean process(Set<? extends TypeElement> annotations,
                               RoundEnvironment roundEnv) {
            if (roundEnv.processingOver()) {
                for (String key : recordNameExpectedAnnotationMap.keySet()) {
                    Element element = processingEnv.getElementUtils().getTypeElement(key);
                    numberOfAnnotations = 0;
                    verifyReferredTypesAcceptable(element, recordNameExpectedAnnotationMap.get(key));
                    Assert.check(numberOfAnnotations == recordNameExpectedAnnotationNumberMap.get(key), "expected = " +
                            recordNameExpectedAnnotationNumberMap.get(key) + " found = " + numberOfAnnotations);
                }
            }
            return true;
        }

        int numberOfAnnotations = 0;

        private void verifyReferredTypesAcceptable(Element rootElement, String expectedAnnotationName) {
            new ElementScanner<Void, Void>() {
                @Override public Void visitType(TypeElement e, Void p) {
                    scan(e.getTypeParameters(), p);
                    scan(e.getEnclosedElements(), p);
                    verifyAnnotations(e.getAnnotationMirrors(), expectedAnnotationName);
                    return null;
                }
                @Override public Void visitTypeParameter(TypeParameterElement e, Void p) {
                    verifyTypesAcceptable(e.getBounds(), expectedAnnotationName);
                    scan(e.getEnclosedElements(), p);
                    verifyAnnotations(e.getAnnotationMirrors(), expectedAnnotationName);
                    return null;
                }
                @Override public Void visitVariable(VariableElement e, Void p) {
                    verifyTypeAcceptable(e.asType(), expectedAnnotationName);
                    scan(e.getEnclosedElements(), p);
                    verifyAnnotations(e.getAnnotationMirrors(), expectedAnnotationName);
                    return null;
                }
                @Override
                public Void visitExecutable(ExecutableElement e, Void p) {
                    scan(e.getTypeParameters(), p);
                    verifyTypeAcceptable(e.getReturnType(), expectedAnnotationName);
                    scan(e.getParameters(), p);
                    verifyTypesAcceptable(e.getThrownTypes(), expectedAnnotationName);
                    scan(e.getEnclosedElements(), p);
                    verifyAnnotations(e.getAnnotationMirrors(), expectedAnnotationName);
                    return null;
                }
                @Override public Void visitRecordComponent(RecordComponentElement e, Void p) {
                    verifyTypeAcceptable(e.asType(), expectedAnnotationName);
                    scan(e.getEnclosedElements(), p);
                    verifyAnnotations(e.getAnnotationMirrors(), expectedAnnotationName);
                    return null;
                }
            }.scan(rootElement, null);
        }

        private void verifyAnnotations(Iterable<? extends AnnotationMirror> annotations, String expectedName) {
            for (AnnotationMirror mirror : annotations) {
                Assert.check(mirror.toString().equals(expectedName));
                numberOfAnnotations++;
            }
        }

        private void verifyTypesAcceptable(Iterable<? extends TypeMirror> types, String expectedAnnotationName) {
            if (types == null) return ;

            for (TypeMirror type : types) {
                verifyTypeAcceptable(type, expectedAnnotationName);
            }
        }

        private void verifyTypeAcceptable(TypeMirror type, String expectedAnnotationName) {
            if (type == null) return ;

            verifyAnnotations(type.getAnnotationMirrors(), expectedAnnotationName);

            switch (type.getKind()) {
                case BOOLEAN: case BYTE: case CHAR: case DOUBLE: case FLOAT:
                case INT: case LONG: case SHORT: case VOID: case NONE: case NULL:
                    return ;
                case DECLARED:
                    DeclaredType dt = (DeclaredType) type;
                    TypeElement outermostTypeElement = outermostTypeElement(dt.asElement());
                    String outermostType = outermostTypeElement.getQualifiedName().toString();

                    for (TypeMirror bound : dt.getTypeArguments()) {
                        verifyTypeAcceptable(bound, expectedAnnotationName);
                    }
                    break;
                case ARRAY:
                    verifyTypeAcceptable(((ArrayType) type).getComponentType(), expectedAnnotationName);
                    break;
                case INTERSECTION:
                    for (TypeMirror element : ((IntersectionType) type).getBounds()) {
                        verifyTypeAcceptable(element, expectedAnnotationName);
                    }
                    break;
                case TYPEVAR:
                    verifyTypeAcceptable(((TypeVariable) type).getLowerBound(), expectedAnnotationName);
                    verifyTypeAcceptable(((TypeVariable) type).getUpperBound(), expectedAnnotationName);
                    break;
                case WILDCARD:
                    verifyTypeAcceptable(((WildcardType) type).getExtendsBound(), expectedAnnotationName);
                    verifyTypeAcceptable(((WildcardType) type).getSuperBound(), expectedAnnotationName);
                    break;
                default:
                    error("Type not acceptable for this API: " + type.toString());
                    break;

            }
        }

        private TypeElement outermostTypeElement(Element el) {
            while (el.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
                el = el.getEnclosingElement();
            }

            return (TypeElement) el;
        }

        private void error(String text) {
            processingEnv.getMessager().printMessage(Kind.ERROR, text);
        }
    }
}
