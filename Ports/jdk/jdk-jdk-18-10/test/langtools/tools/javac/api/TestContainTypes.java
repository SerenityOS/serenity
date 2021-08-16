/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6981185
 * @summary  com.sun.tools.model.JavacTypes.contains() calls Type.contains instead of Types.containsType
 * @modules jdk.compiler
 * @run main TestContainTypes
 */

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.DeclaredType;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;

public class TestContainTypes {

    enum ClassType {
        OBJECT("Object"),
        NUMBER("Number"),
        INTEGER("Integer"),
        STRING("String");

        String classStub;

        ClassType(String classStub) {
            this.classStub = classStub;
        }

        boolean subtypeOf(ClassType that) {
            switch (that) {
                case OBJECT: return true;
                case NUMBER: return this == NUMBER || this == INTEGER;
                case INTEGER: return this == INTEGER;
                case STRING: return this == STRING;
                default: throw new AssertionError("Bad type kind in subtyping test");
            }
        }
    }

    enum ParameterType {
        INVARIANT("List<#1>"),
        COVARIANT("List<? extends #1>"),
        CONTRAVARIANT("List<? super #1>"),
        BIVARIANT("List<?>");

        String paramTypeStub;

        ParameterType(String paramTypeStub) {
            this.paramTypeStub = paramTypeStub;
        }

        String instantiate(ClassType ct) {
            return paramTypeStub.replace("#1", ct.classStub);
        }

        static boolean contains(ParameterType pt1, ClassType ct1,
                ParameterType pt2, ClassType ct2) {
            switch (pt1) {
                case INVARIANT: return (pt2 == INVARIANT && ct1 == ct2) ||
                                           (pt2 == CONTRAVARIANT && ct1 == ct2 && ct1 == ClassType.OBJECT);
                case COVARIANT: return ((pt2 == INVARIANT || pt2 == COVARIANT) &&
                                            ct2.subtypeOf(ct1)) ||
                                            (ct1 == ClassType.OBJECT);
                case CONTRAVARIANT: return (pt2 == INVARIANT || pt2 == CONTRAVARIANT) &&
                                            ct1.subtypeOf(ct2);
                case BIVARIANT: return true;
                default: throw new AssertionError("Bad type kind in containment test");
            }
        }
    }

    static class JavaSource extends SimpleJavaFileObject {

        final static String sourceStub =
                        "import java.util.List;\n" +
                        "@interface ToCheck {}\n" +
                        "class Test {\n" +
                        "   @ToCheck void test(#A a, #B b) {}\n" +
                        "}\n";

        String source;

        public JavaSource(String typeA, String typeB) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            source = sourceStub.replace("#A", typeA).replace("#B", typeB);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    static final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
    static final JavaFileManager fm = tool.getStandardFileManager(null, null, null);

    public static void main(String... args) throws Exception {
        try {
            for (ClassType ctA : ClassType.values()) {
                for (ParameterType ptA : ParameterType.values()) {
                    for (ClassType ctB : ClassType.values()) {
                        for (ParameterType ptB : ParameterType.values()) {
                            compileAndCheck(ptA, ctA, ptB, ctB);
                        }
                    }
                }
            }
        } finally {
            fm.close();
        }
    }

    static void compileAndCheck(ParameterType ptA, ClassType ctA, ParameterType ptB, ClassType ctB) throws Exception {
        JavaSource source = new JavaSource(ptA.instantiate(ctA), ptB.instantiate(ctB));
        JavacTask ct = (JavacTask)tool.getTask(null, fm, null,
                null, null, Arrays.asList(source));
        ct.setProcessors(Arrays.asList(new ContainTypesTester(ParameterType.contains(ptA, ctA, ptB, ctB), source)));
        System.err.println("A = " + ptA +" / " + ptA.instantiate(ctA));
        System.err.println("B = " + ptB +" / " + ptB.instantiate(ctB));
        System.err.println("Source = " + source.source);
        ct.analyze();
    }

    @SupportedSourceVersion(SourceVersion.RELEASE_7)
    static class ContainTypesTester extends AbstractProcessor {

        boolean expected;
        JavaSource source;

        ContainTypesTester(boolean expected, JavaSource source) {
            this.expected = expected;
            this.source = source;
        }

        @Override
        public Set<String> getSupportedAnnotationTypes() {
            Set<String> supportedAnnos = new HashSet();
            supportedAnnos.add("*");
            return supportedAnnos;
        }

        private void error(String msg) {
            System.err.println(source.source);
            throw new AssertionError(msg);
        }

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (roundEnv.getRootElements().size() == 0) {
                return true;
            }
            if (annotations.isEmpty() || annotations.size() > 1) {
                error("no anno found/wrong number of annotations found: " + annotations.size());
            }
            TypeElement anno = (TypeElement)annotations.toArray()[0];
            Set<? extends Element> annoElems = roundEnv.getElementsAnnotatedWith(anno);
            if (annoElems.isEmpty() || annoElems.size() > 1) {
                error("no annotated element found/wrong number of annotated elements found: " + annoElems.size());
            }
            Element annoElement = (Element)annoElems.toArray()[0];
            if (!(annoElement instanceof ExecutableElement)) {
                error("annotated element must be a method");
            }
            ExecutableElement method = (ExecutableElement)annoElement;
            if (method.getParameters().size() != 2) {
                error("annotated method must have 2 arguments");
            }
            DeclaredType d1 = (DeclaredType)method.getParameters().get(0).asType();
            DeclaredType d2 = (DeclaredType)method.getParameters().get(1).asType();
            if (d1.getTypeArguments().size() != 1 ||
                    d1.getTypeArguments().size() != 1) {
                error("parameter type must be generic in one type-variable");
            }
            TypeMirror t1 = d1.getTypeArguments().get(0);
            TypeMirror t2 = d2.getTypeArguments().get(0);

            if (processingEnv.getTypeUtils().contains(t1, t2) != expected) {
                error("bad type containment result\n" +
                        "t1 : " + t1 +"\n" +
                        "t2 : " + t2 +"\n" +
                        "expected answer : " + expected +"\n");
            }
            return true;
        }
    }
}
