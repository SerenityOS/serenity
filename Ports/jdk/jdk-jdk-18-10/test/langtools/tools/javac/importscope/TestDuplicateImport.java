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


public class TestDuplicateImport {

    static int checkCount = 0;

    enum ImportKind {
        NORMAL("import a.#Q.#N;"),
        STATIC("import static a.#Q.#N;");

        String importStr;

        ImportKind(String importStr) {
            this.importStr = importStr;
        }

        String getImportStatement(QualifierKind qk, NameKind nk) {
            return importStr.replaceAll("#Q", qk.qualifierStr)
                    .replaceAll("#N", nk.nameStr);
        }

        boolean isStatic() {
            return this == STATIC;
        }
    }

    enum QualifierKind {
        A("A"),
        B("B"),
        C("C");

        String qualifierStr;

        QualifierKind(String qualifierStr) {
            this.qualifierStr = qualifierStr;
        }

        public boolean compatible(QualifierKind ik) {
            return this == ik || (this != A && ik != A);
        }
    }

    enum NameKind {
        D("D"),
        E("E"),
        M("m"),
        F("f"),
        STAR("*"),
        NON_EXISTENT("NonExistent");

        String nameStr;

        NameKind(String nameStr) {
            this.nameStr = nameStr;
        }

        boolean exists() {
            return this != NON_EXISTENT;
        }

        boolean isMember() {
            return this == M || this == F;
        }

        boolean isType() {
            return this == D || this == E;
        }
    }

    public static void main(String... args) throws Exception {

        //create default shared JavaCompiler - reused across multiple compilations
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null);

        for (ImportKind ik1 : ImportKind.values()) {
            for (ImportKind ik2 : ImportKind.values()) {
                for (QualifierKind qk1 : QualifierKind.values()) {
                    for (QualifierKind qk2 : QualifierKind.values()) {
                        for (NameKind nk1 : NameKind.values()) {
                            for (NameKind nk2 : NameKind.values()) {
                                new TestDuplicateImport(ik1, ik2, qk1, qk2, nk1, nk2).run(comp, fm);
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Total check executed: " + checkCount);
    }

    ImportKind ik1;
    ImportKind ik2;
    QualifierKind qk1;
    QualifierKind qk2;
    NameKind nk1;
    NameKind nk2;
    JavaSource source;
    DiagnosticChecker diagChecker;

    TestDuplicateImport(ImportKind ik1, ImportKind ik2, QualifierKind qk1, QualifierKind qk2, NameKind nk1, NameKind nk2) {
        this.ik1 = ik1;
        this.ik2 = ik2;
        this.qk1 = qk1;
        this.qk2 = qk2;
        this.nk1 = nk1;
        this.nk2 = nk2;
        this.source = new JavaSource();
        this.diagChecker = new DiagnosticChecker();
    }
    class JavaSource extends SimpleJavaFileObject {

        String bodyTemplate = "package a;\n" +
                              "#I1\n" +
                              "#I2\n" +
                              "class A {\n" +
                              "   static class D { }\n" +
                              "   static class E { }\n" +
                              "   static Object f;\n" +
                              "   static void m() { }\n" +
                              "}\n" +
                              "class B {\n" +
                              "   static class D { }\n" +
                              "   static class E { }\n" +
                              "   static Object f;\n" +
                              "   static void m() { }\n" +
                              "}\n" +
                              "class C extends B {\n" +
                              "}\n";

        String source;

        public JavaSource() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            source = bodyTemplate.replaceAll("#I1", ik1.getImportStatement(qk1, nk1))
                    .replaceAll("#I2", ik2.getImportStatement(qk2, nk2));
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

        //error if the import refers to a non-existent symbol
        if (!nk1.exists() || !nk2.exists()) {
            errorExpected = true;
        }

        //error if a non-static import refers to a non-type symbol
        if ((nk1.isMember() && !ik1.isStatic()) ||
                (nk2.isMember() && !ik2.isStatic())) {
            errorExpected = true;
        }

        //error if two single non-static (or one static and one non-static)
        //imports import same names from different places
        if (nk1 == nk2 && nk1 != NameKind.STAR && !qk1.compatible(qk2) &&
                (!ik1.isStatic() || !ik2.isStatic())) {
            errorExpected = true;
        }

        if ((qk1 == QualifierKind.C && !ik1.isStatic() && nk1 != NameKind.STAR) ||
            (qk2 == QualifierKind.C && !ik2.isStatic() && nk2 != NameKind.STAR)) {
            errorExpected = true;
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
