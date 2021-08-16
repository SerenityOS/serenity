/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7030150 7039931
 * @summary Type inference for generic instance creation failed for formal type parameter
 * @modules jdk.compiler
 */

import com.sun.source.util.JavacTask;
import java.net.URI;
import java.util.Arrays;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class GenericConstructorAndDiamondTest {

    enum BoundKind {
        NO_BOUND(""),
        STRING_BOUND("extends String"),
        INTEGER_BOUND("extends Integer");

        String boundStr;

        private BoundKind(String boundStr) {
            this.boundStr = boundStr;
        }

        boolean matches(TypeArgumentKind tak) {
            switch (tak) {
                case NONE: return true;
                case STRING: return this != INTEGER_BOUND;
                case INTEGER: return this != STRING_BOUND;
                default: return false;
            }
        }
    }

    enum ConstructorKind {
        NON_GENERIC("Foo(Object o) {}"),
        GENERIC_NO_BOUND("<T> Foo(T t) {}"),
        GENERIC_STRING_BOUND("<T extends String> Foo(T t) {}"),
        GENERIC_INTEGER_BOUND("<T extends Integer> Foo(T t) {}");

        String constrStr;

        private ConstructorKind(String constrStr) {
            this.constrStr = constrStr;
        }

        boolean matches(ArgumentKind ak) {
            switch (ak) {
                case STRING: return this != GENERIC_INTEGER_BOUND;
                case INTEGER: return this != GENERIC_STRING_BOUND;
                default: return false;
            }
        }
    }

    enum TypeArgArity {
        ONE(1),
        TWO(2),
        THREE(3);

        int n;

        private TypeArgArity(int n) {
            this.n = n;
        }
    }

    enum TypeArgumentKind {
        NONE(""),
        STRING("String"),
        INTEGER("Integer");

        String typeargStr;

        private TypeArgumentKind(String typeargStr) {
            this.typeargStr = typeargStr;
        }

        String getArgs(TypeArgArity arity) {
            if (this == NONE) return "";
            else {
                StringBuilder buf = new StringBuilder();
                String sep = "";
                for (int i = 0 ; i < arity.n ; i++) {
                    buf.append(sep);
                    buf.append(typeargStr);
                    sep = ",";
                }
                return "<" + buf.toString() + ">";
            }
        }

        boolean matches(ArgumentKind ak) {
            switch (ak) {
                case STRING: return this != INTEGER;
                case INTEGER: return this != STRING;
                default: return false;
            }
        }

        boolean matches(TypeArgumentKind other) {
            switch (other) {
                case STRING: return this != INTEGER;
                case INTEGER: return this != STRING;
                default: return true;
            }
        }
    }

    enum ArgumentKind {
        STRING("\"\""),
        INTEGER("1");

        String argStr;

        private ArgumentKind(String argStr) {
            this.argStr = argStr;
        }
    }

    public static void main(String... args) throws Exception {

        //create default shared JavaCompiler - reused across multiple compilations
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {
            for (BoundKind boundKind : BoundKind.values()) {
                for (ConstructorKind constructorKind : ConstructorKind.values()) {
                    for (TypeArgumentKind declArgKind : TypeArgumentKind.values()) {
                        for (TypeArgArity arity : TypeArgArity.values()) {
                            for (TypeArgumentKind useArgKind : TypeArgumentKind.values()) {
                                for (TypeArgumentKind diamondArgKind : TypeArgumentKind.values()) {
                                    for (ArgumentKind argKind : ArgumentKind.values()) {
                                        new GenericConstructorAndDiamondTest(boundKind, constructorKind,
                                                declArgKind, arity, useArgKind, diamondArgKind, argKind).run(comp, fm);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    BoundKind boundKind;
    ConstructorKind constructorKind;
    TypeArgumentKind declTypeArgumentKind;
    TypeArgArity useTypeArgArity;
    TypeArgumentKind useTypeArgumentKind;
    TypeArgumentKind diamondTypeArgumentKind;
    ArgumentKind argumentKind;
    JavaSource source;
    DiagnosticChecker diagChecker;

    GenericConstructorAndDiamondTest(BoundKind boundKind, ConstructorKind constructorKind,
            TypeArgumentKind declTypeArgumentKind, TypeArgArity useTypeArgArity,
            TypeArgumentKind useTypeArgumentKind, TypeArgumentKind diamondTypeArgumentKind,
            ArgumentKind argumentKind) {
        this.boundKind = boundKind;
        this.constructorKind = constructorKind;
        this.declTypeArgumentKind = declTypeArgumentKind;
        this.useTypeArgArity = useTypeArgArity;
        this.useTypeArgumentKind = useTypeArgumentKind;
        this.diamondTypeArgumentKind = diamondTypeArgumentKind;
        this.argumentKind = argumentKind;
        this.source = new JavaSource();
        this.diagChecker = new DiagnosticChecker();
    }

    class JavaSource extends SimpleJavaFileObject {

        String template = "class Foo<X #BK> {\n" +
                              "#CK\n" +
                          "}\n" +
                          "class Test {\n" +
                              "void test() {\n" +
                                 "Foo#TA1 f = new #TA2 Foo<#TA3>(#A);\n" +
                              "}\n" +
                          "}\n";

        String source;

        public JavaSource() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            source = template.replace("#BK", boundKind.boundStr).
                    replace("#CK", constructorKind.constrStr)
                    .replace("#TA1", declTypeArgumentKind.getArgs(TypeArgArity.ONE))
                    .replace("#TA2", useTypeArgumentKind.getArgs(useTypeArgArity))
                    .replace("#TA3", diamondTypeArgumentKind.typeargStr)
                    .replace("#A", argumentKind.argStr);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    void run(JavaCompiler tool, StandardJavaFileManager fm) throws Exception {
        JavacTask ct = (JavacTask)tool.getTask(null, fm, diagChecker,
                null, null, Arrays.asList(source));
        ct.analyze();
        check();
    }

    void check() {
        boolean badActual = !constructorKind.matches(argumentKind);

        boolean badArity = constructorKind != ConstructorKind.NON_GENERIC &&
                useTypeArgumentKind != TypeArgumentKind.NONE &&
                useTypeArgArity != TypeArgArity.ONE;

        boolean badMethodTypeArg = constructorKind != ConstructorKind.NON_GENERIC &&
                !useTypeArgumentKind.matches(argumentKind);

        boolean badExplicitParams = (useTypeArgumentKind != TypeArgumentKind.NONE &&
                diamondTypeArgumentKind == TypeArgumentKind.NONE) ||
                !boundKind.matches(diamondTypeArgumentKind);

        boolean badGenericType = !boundKind.matches(declTypeArgumentKind) ||
                !diamondTypeArgumentKind.matches(declTypeArgumentKind);

        boolean shouldFail = badActual || badArity ||
                badMethodTypeArg || badExplicitParams || badGenericType;

        if (shouldFail != diagChecker.errorFound) {
            throw new Error("invalid diagnostics for source:\n" +
                source.getCharContent(true) +
                "\nFound error: " + diagChecker.errorFound +
                "\nExpected error: " + shouldFail);
        }
    }

    static class DiagnosticChecker implements javax.tools.DiagnosticListener<JavaFileObject> {

        boolean errorFound;

        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                errorFound = true;
            }
        }
    }
}
