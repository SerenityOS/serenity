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
 * @bug 7086601
 * @summary Error message bug: cause for method mismatch is 'null'
 * @modules jdk.compiler
 */

import com.sun.source.util.JavacTask;
import java.net.URI;
import java.util.Arrays;
import java.util.ArrayList;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;


public class T7086601b {

    static int checkCount = 0;

    enum TypeKind {
        STRING("String", false),
        INTEGER("Integer", false),
        NUMBER("Number", false),
        SERIALIZABLE("java.io.Serializable", true),
        CLONEABLE("Cloneable", true),
        X("X", false),
        Y("Y", false),
        Z("Z", false);

        String typeStr;
        boolean isInterface;

        private TypeKind(String typeStr, boolean isInterface) {
            this.typeStr = typeStr;
            this.isInterface = isInterface;
        }

        boolean isSubtypeof(TypeKind other) {
            return (this == INTEGER && other == NUMBER ||
                    this == Z && other == Y ||
                    this == other);
        }
    }

    enum MethodCallKind {
        ARITY_ONE("m(a1);", 1),
        ARITY_TWO("m(a1, a2);", 2),
        ARITY_THREE("m(a1, a2, a3);", 3);

        String invokeString;
        int arity;

        private MethodCallKind(String invokeString, int arity) {
            this.invokeString = invokeString;
            this.arity = arity;
        }
    }

    public static void main(String... args) throws Exception {

        //create default shared JavaCompiler - reused across multiple compilations
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {

            for (TypeKind a1 : TypeKind.values()) {
                for (TypeKind a2 : TypeKind.values()) {
                    for (TypeKind a3 : TypeKind.values()) {
                        for (MethodCallKind mck : MethodCallKind.values()) {
                            new T7086601b(a1, a2, a3, mck).run(comp, fm);
                        }
                    }
                }
            }
            System.out.println("Total check executed: " + checkCount);
        }
    }

    TypeKind a1;
    TypeKind a2;
    TypeKind a3;
    MethodCallKind mck;
    JavaSource source;
    DiagnosticChecker diagChecker;

    T7086601b(TypeKind a1, TypeKind a2, TypeKind a3, MethodCallKind mck) {
        this.a1 = a1;
        this.a2 = a2;
        this.a3 = a3;
        this.mck = mck;
        this.source = new JavaSource();
        this.diagChecker = new DiagnosticChecker();
    }

    class JavaSource extends SimpleJavaFileObject {

        final String bodyTemplate = "import java.util.List;\n"+
                              "class Test {\n" +
                              "   <Z> void m(List<? super Z> l1) { }\n" +
                              "   <Z> void m(List<? super Z> l1, List<? super Z> l2) { }\n" +
                              "   <Z> void m(List<? super Z> l1, List<? super Z> l2, List<? super Z> l3) { }\n" +
                              "   <X,Y,Z extends Y> void test(List<#A1> a1, List<#A2> a2, List<#A3> a3) { #MC } }";

        String source;

        public JavaSource() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            source = bodyTemplate.replace("#A1", a1.typeStr)
                             .replace("#A2", a2.typeStr).replace("#A3", a3.typeStr)
                             .replace("#MC", mck.invokeString);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    void run(JavaCompiler tool, StandardJavaFileManager fm) throws Exception {
        JavacTask ct = (JavacTask)tool.getTask(null, fm, diagChecker,
                null, null, Arrays.asList(source));
        try {
            ct.analyze();
        } catch (Throwable ex) {
            throw new AssertionError("Error thrown when compiling the following code:\n" + source.getCharContent(true));
        }
        check();
    }

    void check() {
        checkCount++;

        boolean errorExpected = false;

        if (mck.arity > 1) {
            TypeKind[] argtypes = { a1, a2, a3 };
            ArrayList<TypeKind> classes = new ArrayList<>();
            for (int i = 0 ; i < mck.arity ; i ++ ) {
                if (!argtypes[i].isInterface) {
                    classes.add(argtypes[i]);
                }
            }
            boolean glb_exists = true;
            for (TypeKind arg_i : classes) {
                glb_exists = true;
                for (TypeKind arg_j : classes) {
                    if (!arg_i.isSubtypeof(arg_j)) {
                        glb_exists = false;
                        break;
                    }
                }
                if (glb_exists) break;
            }
            errorExpected = !glb_exists;
        }

        if (errorExpected != diagChecker.errorFound) {
            throw new Error("invalid diagnostics for source:\n" +
                source.getCharContent(true) +
                "\nFound error: " + diagChecker.errorFound +
                "\nExpected error: " + errorExpected);
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
