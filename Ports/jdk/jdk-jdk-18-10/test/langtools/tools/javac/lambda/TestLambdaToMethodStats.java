/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8013576 8129962
 * @summary Add stat support to LambdaToMethod
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @run main TestLambdaToMethodStats
 */

import java.io.IOException;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.sun.tools.javac.api.ClientCodeWrapper;

import com.sun.tools.javac.util.List;
import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;

public class TestLambdaToMethodStats extends ComboInstance<TestLambdaToMethodStats> {

    enum ExprKind implements ComboParameter {
        LAMBDA("()->null"),
        MREF1("this::g"),
        MREF2("this::h");

        String exprStr;

        ExprKind(String exprStr) {
            this.exprStr = exprStr;
        }

        @Override
        public String expand(String optParameter) {
            return exprStr;
        }
    }

    enum TargetKind implements ComboParameter {
        IMPLICIT(""),
        SERIALIZABLE("(A & java.io.Serializable)");

        String targetStr;

        TargetKind(String targetStr) {
            this.targetStr = targetStr;
        }

        @Override
        public String expand(String optParameter) {
            return targetStr;
        }
    }

    enum DiagnosticKind {
        LAMBDA_STAT("compiler.note.lambda.stat", true, false),
        MREF_STAT("compiler.note.mref.stat", false, false),
        MREF_STAT1("compiler.note.mref.stat.1", false, true);

        String code;
        boolean lambda;
        boolean bridge;

        DiagnosticKind(String code, boolean lambda, boolean bridge) {
            this.code = code;
            this.lambda = lambda;
            this.bridge = bridge;
        }
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<TestLambdaToMethodStats>()
                .withDimension("EXPR", (x, expr) -> x.ek = expr, ExprKind.values())
                .withDimension("CAST", (x, target) -> x.tk = target, TargetKind.values())
                .run(TestLambdaToMethodStats::new);
    }

    ExprKind ek;
    TargetKind tk;

    String template = "interface A {\n" +
            "   Object o();\n" +
            "}\n" +
            "class Test {\n" +
            "   A a = #{CAST}#{EXPR};\n" +
            "   Object g() { return null; }\n" +
            "   Object h(Object... o) { return null; }\n" +
            "}";

    @Override
    public void doWork() throws IOException {
        newCompilationTask()
                .withOption("--debug=dumpLambdaToMethodStats")
                .withSourceFromTemplate(template)
                .generate(this::check);
    }

    void check(Result<?> res) {
        DiagnosticKind diag = null;
        boolean altMetafactory = false;
        for (DiagnosticKind dk : DiagnosticKind.values()) {
            List<Diagnostic<? extends JavaFileObject>> jcDiag = res.diagnosticsForKey(dk.code);
            if (jcDiag.nonEmpty()) {
                diag = dk;
                ClientCodeWrapper.DiagnosticSourceUnwrapper dsu =
                        (ClientCodeWrapper.DiagnosticSourceUnwrapper)jcDiag.head;
                altMetafactory = (Boolean)dsu.d.getArgs()[0];
                break;
            }
        }

        if (diag == null) {
            fail("No diagnostic found; " + res.compilationInfo());
        }

        boolean error = diag.lambda !=
                (ek == ExprKind.LAMBDA);

        error |= diag.bridge !=
                (ek == ExprKind.MREF2);

        error |= altMetafactory !=
                (tk == TargetKind.SERIALIZABLE);

        if (error) {
            fail("Bad stat diagnostic found for source\n" +
                    "lambda = " + diag.lambda + "\n" +
                    "bridge = " + diag.bridge + "\n" +
                    "altMF = " + altMetafactory + "\n" +
                    res.compilationInfo());
        }
    }
}
