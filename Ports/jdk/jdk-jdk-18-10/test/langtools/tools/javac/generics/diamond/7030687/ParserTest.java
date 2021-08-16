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
 * @bug 7030687
 * @summary Diamond: compiler accepts erroneous code where diamond is used with non-generic inner class
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

public class ParserTest {

    enum TypeArgumentKind {
        NONE(""),
        EXPLICIT("<String>"),
        DIAMOND("<>");

        String typeargStr;

        private TypeArgumentKind(String typeargStr) {
            this.typeargStr = typeargStr;
        }
    }

    enum TypeQualifierArity {
        ONE(1, "A1#TA1"),
        TWO(2, "A1#TA1.A2#TA2"),
        THREE(3, "A1#TA1.A2#TA2.A3#TA3"),
        FOUR(4, "A1#TA1.A2#TA2.A3#TA3.A4#TA4");

        int n;
        String qualifierStr;

        private TypeQualifierArity(int n, String qualifierStr) {
            this.n = n;
            this.qualifierStr = qualifierStr;
        }

        String getType(TypeArgumentKind... typeArgumentKinds) {
            String res = qualifierStr;
            for (int i = 1 ; i <= typeArgumentKinds.length ; i++) {
                res = res.replace("#TA" + i, typeArgumentKinds[i-1].typeargStr);
            }
            return res;
        }
    }

    public static void main(String... args) throws Exception {

        //create default shared JavaCompiler - reused across multiple compilations
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {

            for (TypeQualifierArity arity : TypeQualifierArity.values()) {
                for (TypeArgumentKind tak1 : TypeArgumentKind.values()) {
                    if (arity == TypeQualifierArity.ONE) {
                        new ParserTest(arity, tak1).run(comp, fm);
                        continue;
                    }
                    for (TypeArgumentKind tak2 : TypeArgumentKind.values()) {
                        if (arity == TypeQualifierArity.TWO) {
                            new ParserTest(arity, tak1, tak2).run(comp, fm);
                            continue;
                        }
                        for (TypeArgumentKind tak3 : TypeArgumentKind.values()) {
                            if (arity == TypeQualifierArity.THREE) {
                                new ParserTest(arity, tak1, tak2, tak3).run(comp, fm);
                                continue;
                            }
                            for (TypeArgumentKind tak4 : TypeArgumentKind.values()) {
                                new ParserTest(arity, tak1, tak2, tak3, tak4).run(comp, fm);
                            }
                        }
                    }
                }
            }
        }
    }

    TypeQualifierArity qualifierArity;
    TypeArgumentKind[] typeArgumentKinds;
    JavaSource source;
    DiagnosticChecker diagChecker;

    ParserTest(TypeQualifierArity qualifierArity, TypeArgumentKind... typeArgumentKinds) {
        this.qualifierArity = qualifierArity;
        this.typeArgumentKinds = typeArgumentKinds;
        this.source = new JavaSource();
        this.diagChecker = new DiagnosticChecker();
    }

    class JavaSource extends SimpleJavaFileObject {

        String template = "class Test {\n" +
                              "R res = new #T();\n" +
                          "}\n";

        String source;

        public JavaSource() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            source = template.replace("#T", qualifierArity.getType(typeArgumentKinds));
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    void run(JavaCompiler tool, StandardJavaFileManager fm) throws Exception {
        JavacTask ct = (JavacTask)tool.getTask(null, fm, diagChecker,
                null, null, Arrays.asList(source));
        ct.parse();
        check();
    }

    void check() {

        boolean errorExpected = false;

        for (int i = 0 ; i < qualifierArity.n - 1 ; i++) {
            if (typeArgumentKinds[i] == TypeArgumentKind.DIAMOND) {
                errorExpected = true;
                break;
            }
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
