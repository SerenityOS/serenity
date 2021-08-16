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
 * @bug 7101822
 * @summary static import fails to resolve interfaces on nested enums via import statements
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

public class TestLazyImportScope {

    static int checkCount = 0;

    enum ImportOrder {
        NORMAL("import a.C.D;\n" +
               "#I"),
        REVERSE("#I\n" +
               "import a.C.D;");

        String importLayout;

        ImportOrder(String importLayout) {
            this.importLayout = importLayout;
        }

        String getImportString(ImportKind ik) {
            return importLayout.replaceAll("#I", ik.importStr);
        }
    }

    enum ImportKind {
        NAMED("import a.A.B.E;"),
        ON_DEMAND("import a.A.B.*;"),
        STATIC_NAMED_TYPE("import static a.A.B.E;"),
        STATIC_NAMED_MEMBER("import static a.A.B.bm;"),
        STATIC_ON_DEMAND("import static a.A.B.*;");

        String importStr;

        private ImportKind(String importStr) {
            this.importStr = importStr;
        }
    }

    enum TypeRefKind {
        NONE(""),
        E("E e = null;"),
        F("F f = null;"),
        BOTH("E e = null; F f = null;");

        String typeRefStr;

        private TypeRefKind(String typeRefStr) {
            this.typeRefStr = typeRefStr;
        }

        boolean isImported(ImportKind ik) {
            switch (ik) {
                case NAMED:
                case STATIC_NAMED_TYPE: return this == NONE || this == E;
                case ON_DEMAND:
                case STATIC_ON_DEMAND: return true;
                default: return this == NONE;
            }
        }
    }

    enum MemberRefKind {
        NONE(""),
        FIELD("Object o = bf;"),
        METHOD("bm();"),
        BOTH("Object o = bf; bm();");

        String memberRefStr;

        private MemberRefKind(String memberRefStr) {
            this.memberRefStr = memberRefStr;
        }

        boolean isImported(ImportKind ik) {
            switch (ik) {
                case STATIC_NAMED_MEMBER: return this == NONE || this == METHOD;
                case STATIC_ON_DEMAND: return true;
                default: return this == NONE;
            }
        }
    }

    public static void main(String... args) throws Exception {

        //create default shared JavaCompiler - reused across multiple compilations
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null);

        for (ImportOrder ord : ImportOrder.values()) {
            for (ImportKind ik : ImportKind.values()) {
                for (TypeRefKind tk : TypeRefKind.values()) {
                    for (MemberRefKind mk : MemberRefKind.values()) {
                        new TestLazyImportScope(ord, ik, tk, mk).run(comp, fm);
                    }
                }
            }
        }
        System.out.println("Total check executed: " + checkCount);
    }

    ImportOrder ord;
    ImportKind ik;
    TypeRefKind tk;
    MemberRefKind mk;
    JavaSource source;
    DiagnosticChecker diagChecker;

    TestLazyImportScope(ImportOrder ord, ImportKind ik, TypeRefKind tk, MemberRefKind mk) {
        this.ord = ord;
        this.ik = ik;
        this.tk = tk;
        this.mk = mk;
        this.source = new JavaSource();
        this.diagChecker = new DiagnosticChecker();
    }

    class JavaSource extends SimpleJavaFileObject {

        String bodyTemplate = "package a;\n" +
                              "#I\n" +
                              "class A {\n" +
                              "   static class B extends D {\n" +
                              "      static class E { }\n" +
                              "      static class F { }\n" +
                              "      static Object bf;\n" +
                              "      static void bm() { }\n" +
                              "   }\n" +
                              "}\n" +
                              "class C {\n" +
                              "   static class D { }\n" +
                              "}\n" +
                              "class Test {\n" +
                              "   void test() {\n" +
                              "      #T\n" +
                              "      #M\n" +
                              "   }\n" +
                              "}";

        String source;

        public JavaSource() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            source = bodyTemplate.replaceAll("#I", ord.getImportString(ik))
                    .replaceAll("#T", tk.typeRefStr)
                    .replaceAll("#M", mk.memberRefStr);
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

        boolean errorExpected = !tk.isImported(ik) || !mk.isImported(ik);

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
