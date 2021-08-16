/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003280
 * @summary Add lambda tests
 *  compile crashes on partial lambda expressions
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


public class BadLambdaExpr {

    static int checkCount = 0;

    enum ParameterListKind {
        ZERO_ARY("()"),
        UNARY("(#P)"),
        TWO_ARY("(#P, #P)"),
        THREE_ARY("(#P, #P, #P)");

        String parametersTemplateStr;

        ParameterListKind(String parametersTemplateStr) {
            this.parametersTemplateStr = parametersTemplateStr;
        }

        String getParameterString(ParameterKind pk) {
            return parametersTemplateStr.replaceAll("#P", pk.parameterStr);
        }
    }

    enum ParameterKind {
        IMPLICIT("a"),
        EXPLIICT("A a");

        String parameterStr;

        ParameterKind(String parameterStr) {
            this.parameterStr = parameterStr;
        }
    }

    enum ArrowKind {
        NONE(""),
        SEMI("-"),
        FULL("->");

        String arrowStr;

        ArrowKind(String arrowStr) {
            this.arrowStr = arrowStr;
        }
    }

    enum ExprKind {
        NONE("#P#A"),
        METHOD_CALL("m(#P#A)"),
        CONSTR_CALL("new Foo(#P#A)");

        String expressionTemplate;

        ExprKind(String expressionTemplate) {
            this.expressionTemplate = expressionTemplate;
        }

        String expressionString(ParameterListKind plk, ParameterKind pk,
                ArrowKind ak) {
            return expressionTemplate.replaceAll("#P", plk.getParameterString(pk))
                    .replaceAll("#A", ak.arrowStr);
        }
    }

    public static void main(String... args) throws Exception {

        //create default shared JavaCompiler - reused across multiple compilations
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {

            for (ParameterListKind plk : ParameterListKind.values()) {
                for (ParameterKind pk : ParameterKind.values()) {
                    for (ArrowKind ak : ArrowKind.values()) {
                        for (ExprKind ek : ExprKind.values()) {
                            new BadLambdaExpr(plk, pk, ak, ek).run(comp, fm);
                        }
                    }
                }
            }
            System.out.println("Total check executed: " + checkCount);
        }
    }

    ParameterListKind plk;
    ParameterKind pk;
    ArrowKind ak;
    ExprKind ek;
    JavaSource source;
    DiagnosticChecker diagChecker;

    BadLambdaExpr(ParameterListKind plk, ParameterKind pk, ArrowKind ak, ExprKind ek) {
        this.plk = plk;
        this.pk = pk;
        this.ak = ak;
        this.ek = ek;
        this.source = new JavaSource();
        this.diagChecker = new DiagnosticChecker();
    }

    class JavaSource extends SimpleJavaFileObject {

        String template = "class Test {\n" +
                          "   SAM s = #E;\n" +
                          "}";

        String source;

        public JavaSource() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            source = template.replaceAll("#E", ek.expressionString(plk, pk, ak));
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
            ct.parse();
        } catch (Throwable ex) {
            throw new AssertionError("Error thron when parsing the following source:\n" + source.getCharContent(true));
        }
        check();
    }

    void check() {
        boolean errorExpected =
                ak != ArrowKind.NONE ||
                plk != ParameterListKind.UNARY ||
                pk != ParameterKind.IMPLICIT;
        if (errorExpected != diagChecker.errorFound) {
            throw new Error("bad diag for source:\n" +
                source.getCharContent(true));
        }
        checkCount++;
    }

    static class DiagnosticChecker implements javax.tools.DiagnosticListener<JavaFileObject> {

        boolean errorFound;

        @Override
        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                errorFound = true;
            }
        }
    }
}
