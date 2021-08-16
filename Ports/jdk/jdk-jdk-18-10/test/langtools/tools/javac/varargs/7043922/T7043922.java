/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7043922
 * @summary Regression: internal compiler error for nested anonymous inner class featuring varargs constructor
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 */

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.util.List;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class T7043922 {

    ClassKind[] classKinds;
    ConstructorKind[] constructorKinds;

    T7043922(ClassKind[] classKinds, ConstructorKind[] constructorKinds) {
        this.classKinds = classKinds;
        this.constructorKinds = constructorKinds;
    }

    void compileAndCheck() throws Exception {
        JavaSource source = new JavaSource();
        ErrorChecker ec = new ErrorChecker();
        JavacTask ct = (JavacTask)tool.getTask(null, fm, ec,
                null, null, Arrays.asList(source));
        ct.analyze();
        if (ec.errorFound) {
            throw new Error("invalid diagnostics for source:\n" +
                    source.getCharContent(true) +
                    "\nCompiler diagnostics:\n" + ec.printDiags());
        }
    }

    class JavaSource extends SimpleJavaFileObject {

        static final String source_template = "#C0 A0 { #K0 }\n" +
                                              "#C1 A1 { #K1 }\n" +
                                              "#C2 A2 { #K2 }\n" +
                                              "class D {\n" +
                                              "   void test() {\n" +
                                              "      new A0(#V0) {\n" +
                                              "         void test() {\n" +
                                              "            new A1(#V1) {\n" +
                                              "               void test() {\n" +
                                              "                   new A2(#V2) {};\n" +
                                              "               }\n" +
                                              "            };\n" +
                                              "         }\n" +
                                              "      };\n" +
                                              "   }\n" +
                                              "}\n";

        String source;

        public JavaSource() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            source = source_template;
            for (int i = 0; i < 3; i++) {
                source = source.replaceAll("#C" + i, classKinds[i].classKind).
                    replaceAll("#K" + i, classKinds[i].getConstructor("A" + i, constructorKinds[i])).
                    replaceAll("#V" + i, constructorKinds[i].constrArgs);
            }
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    /** global decls ***/

    enum ConstructorKind {
        DEFAULT("", ""),
        FIXED_ARITY("String s", "\"\""),
        VARIABLE_ARITY("String... ss", "\"\",\"\"");

        String constrParam;
        String constrArgs;

        private ConstructorKind(String constrParam, String constrArgs) {
            this.constrParam = constrParam;
            this.constrArgs = constrArgs;
        }
    }

    enum ClassKind {
        ABSTRACT("abstract class"),
        CLASS("class"),
        INTERFACE("interface");

        String classKind;

        private ClassKind(String classKind) {
            this.classKind = classKind;
        }

        boolean isConstructorOk(ConstructorKind ck) {
            return this != INTERFACE ||
                    ck == ConstructorKind.DEFAULT;
        }

        String getConstructor(String className, ConstructorKind ck) {
            return this == INTERFACE ?
                "" :
                (className + "(" + ck.constrParam + ") {}");
        }
    }

    // Create a single file manager and JavaCompiler tool
    // and reuse them for each compile to save time.
    static final StandardJavaFileManager fm = JavacTool.create().getStandardFileManager(null, null, null);
    static final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();

    public static void main(String... args) throws Exception {
        try {
            for (ClassKind classKind1 : ClassKind.values()) {
                for (ConstructorKind constrKind1 : ConstructorKind.values()) {
                    if (!classKind1.isConstructorOk(constrKind1)) continue;
                    for (ClassKind classKind2 : ClassKind.values()) {
                        for (ConstructorKind constrKind2 : ConstructorKind.values()) {
                            if (!classKind2.isConstructorOk(constrKind2)) continue;
                            for (ClassKind classKind3 : ClassKind.values()) {
                                for (ConstructorKind constrKind3 : ConstructorKind.values()) {
                                    if (!classKind3.isConstructorOk(constrKind3)) continue;
                                    new T7043922(new ClassKind[] { classKind1, classKind2, classKind3 },
                                            new ConstructorKind[] { constrKind1, constrKind2, constrKind3 }).compileAndCheck();
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            fm.close();
        }
    }

    static class ErrorChecker implements javax.tools.DiagnosticListener<JavaFileObject> {

        boolean errorFound;
        List<String> errDiags = List.nil();

        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                errDiags = errDiags.append(diagnostic.getMessage(Locale.getDefault()));
                errorFound = true;
            }
        }

        String printDiags() {
            StringBuilder buf = new StringBuilder();
            for (String s : errDiags) {
                buf.append(s);
                buf.append("\n");
            }
            return buf.toString();
        }
    }
}
