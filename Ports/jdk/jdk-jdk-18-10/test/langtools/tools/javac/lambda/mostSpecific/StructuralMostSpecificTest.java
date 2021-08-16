/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003280 8006694 8129962
 * @summary Add lambda tests
 *  Automatic test for checking correctness of structural most specific test routine
 *  temporarily workaround combo tests are causing time out in several platforms
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper

 * @run main StructuralMostSpecificTest
 */

import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import com.sun.tools.javac.api.ClientCodeWrapper;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.List;
import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;

public class StructuralMostSpecificTest extends ComboInstance<StructuralMostSpecificTest> {

    enum RetTypeKind implements ComboParameter {
        SHORT("short"),
        INT("int"),
        OBJECT("Object"),
        INTEGER("Integer"),
        VOID("void"),
        J_L_VOID("Void");

        String retTypeStr;

        RetTypeKind(String retTypeStr) {
            this.retTypeStr = retTypeStr;
        }

        boolean moreSpecificThan(RetTypeKind rk) {
            return moreSpecificThan[this.ordinal()][rk.ordinal()];
        }

        static boolean[][] moreSpecificThan = {
                //              SHORT |  INT  | OBJECT | INTEGER | VOID  | J_L_VOID
                /* SHORT */   { true  , true  , true   , false   , false , false },
                /* INT */     { false , true  , true   , true    , false , false },
                /* OBJECT */  { false , false , true   , false   , false , false },
                /* INTEGER */ { false , false , true   , true    , false , false },
                /* VOID */    { false , false , false  , false   , true  , true  },
                /* J_L_VOID */{ false , false , true   , false   , false , true  } };

        public String expand(String optParameter) {
            return retTypeStr;
        }
    }

    enum ArgTypeKind implements ComboParameter {
        SHORT("short"),
        INT("int"),
        BOOLEAN("boolean"),
        OBJECT("Object"),
        INTEGER("Integer"),
        DOUBLE("Double");

        String argTypeStr;

        ArgTypeKind(String typeStr) {
            this.argTypeStr = typeStr;
        }

        public String expand(String optParameter) {
            return argTypeStr;
        }
    }

    enum ExceptionKind implements ComboParameter {
        NONE(""),
        EXCEPTION("throws Exception"),
        SQL_EXCEPTION("throws java.sql.SQLException"),
        IO_EXCEPTION("throws java.io.IOException");

        String exceptionStr;

        ExceptionKind(String exceptionStr) {
            this.exceptionStr = exceptionStr;
        }

        public String expand(String optParameter) {
            return exceptionStr;
        }
    }

    enum LambdaReturnKind implements ComboParameter {
        VOID("return;"),
        SHORT("return (short)0;"),
        INT("return 0;"),
        INTEGER("return (Integer)null;"),
        NULL("return null;");

        String retStr;

        LambdaReturnKind(String retStr) {
            this.retStr = retStr;
        }

        boolean compatibleWith(RetTypeKind rk) {
            return compatibleWith[rk.ordinal()][ordinal()];
        }

        static boolean[][] compatibleWith = {
                //              VOID  | SHORT | INT     | INTEGER | NULL
                /* SHORT */   { false , true  , false   , false   , false },
                /* INT */     { false , true  , true    , true    , false },
                /* OBJECT */  { false , true  , true    , true    , true  },
                /* INTEGER */ { false , false , true    , true    , true  },
                /* VOID */    { true  , false , false   , false   , false },
                /* J_L_VOID */{ false , false , false   , false   , true  } };

        boolean needsConversion(RetTypeKind rk) {
            return needsConversion[rk.ordinal()][ordinal()];
        }

        static boolean[][] needsConversion = {
                //              VOID  | SHORT | INT     | INTEGER | NULL
                /* SHORT */   { false , false , false   , false   , false },
                /* INT */     { false , false , false   , true    , false },
                /* OBJECT */  { false , true  , true    , false   , false },
                /* INTEGER */ { false , false , true    , false   , false },
                /* VOID */    { false , false , false   , false   , false },
                /* J_L_VOID */{ true  , false , false   , false   , false } };

        public String expand(String optParameter) {
            return retStr;
        }
    }

    static final String sourceTemplate =
            "interface SAM1 {\n" +
            "   #{RET[0]} m(#{ARG[0]} a1) #{EX[0]};\n" +
            "}\n" +
            "interface SAM2 {\n" +
            "   #{RET[1]} m(#{ARG[1]} a1) #{EX[1]};\n" +
            "}\n" +
            "class Test {\n" +
            "   void m(SAM1 s) { }\n" +
            "   void m(SAM2 s) { }\n" +
            "   { m((#{ARG[0]} x)->{ #{EXPR} }); }\n" +
            "}\n";

    public static void main(String... args) throws Exception {
        new ComboTestHelper<StructuralMostSpecificTest>()
                .withFilter(StructuralMostSpecificTest::hasSameArguments)
                .withFilter(StructuralMostSpecificTest::hasCompatibleReturns)
                .withFilter(StructuralMostSpecificTest::hasSameOverloadPhase)
                .withDimension("EXPR", (x, expr) -> x.lambdaReturnKind = expr, LambdaReturnKind.values())
                .withArrayDimension("RET", (x, ret, idx) -> x.returnType[idx] = ret, 2, RetTypeKind.values())
                .withArrayDimension("EX", 2, ExceptionKind.values())
                .withArrayDimension("ARG", (x, arg, idx) -> x.argumentKind[idx] = arg, 2, ArgTypeKind.values())
                .run(StructuralMostSpecificTest::new);
    }

    LambdaReturnKind lambdaReturnKind;
    RetTypeKind[] returnType = new RetTypeKind[2];
    ArgTypeKind[] argumentKind = new ArgTypeKind[2];

    boolean hasSameArguments() {
        return argumentKind[0] == argumentKind[1];
    }

    boolean hasCompatibleReturns() {
        return lambdaReturnKind.compatibleWith(returnType[0]) &&
                lambdaReturnKind.compatibleWith(returnType[1]);
    }

    boolean hasSameOverloadPhase() {
        return lambdaReturnKind.needsConversion(returnType[0]) == lambdaReturnKind.needsConversion(returnType[1]);
    }

    @Override
    public void doWork() throws Throwable {
        newCompilationTask()
                .withSourceFromTemplate(sourceTemplate)
                .withOption("--debug=verboseResolution=all,-predef,-internal,-object-init")
                .analyze(this::check);
    }

    void check(Result<Iterable<? extends Element>> result) {
        boolean m1MoreSpecific = returnType[0].moreSpecificThan(returnType[1]);
        boolean m2MoreSpecific = returnType[1].moreSpecificThan(returnType[0]);

        boolean ambiguous = (m1MoreSpecific == m2MoreSpecific);

        if (ambiguous != ambiguityFound(result)) {
            fail("invalid diagnostics for combo:\n" +
                result.compilationInfo() + "\n" +
                "\nAmbiguity found: " + ambiguityFound(result) +
                "\nm1 more specific: " + m1MoreSpecific +
                "\nm2 more specific: " + m2MoreSpecific +
                "\nexpected ambiguity: " + ambiguous);
        }

        if (!ambiguous) {
            String sigToCheck = m1MoreSpecific ? "m(SAM1)" : "m(SAM2)";
            if (!sigToCheck.equals(mostSpecificSignature(result))) {
                fail("invalid most specific method selected:\n" +
                        result.compilationInfo() + "\n" +
                        "\nMost specific found: " + mostSpecificSignature(result) +
                        "\nm1 more specific: " + m1MoreSpecific +
                        "\nm2 more specific: " + m2MoreSpecific);
            }
        }
    }

    boolean ambiguityFound(Result<Iterable<? extends Element>> result) {
        return result.containsKey("compiler.err.ref.ambiguous");
    }

    String mostSpecificSignature(Result<Iterable<? extends Element>> result) {
        List<Diagnostic<? extends JavaFileObject>> rsDiag =
                result.diagnosticsForKey("compiler.note.verbose.resolve.multi");
        if (rsDiag.nonEmpty()) {
            ClientCodeWrapper.DiagnosticSourceUnwrapper dsu =
                        (ClientCodeWrapper.DiagnosticSourceUnwrapper)rsDiag.head;
            JCDiagnostic.MultilineDiagnostic mdiag =
                (JCDiagnostic.MultilineDiagnostic)dsu.d;
            int mostSpecificIndex = (Integer)mdiag.getArgs()[2];
            return mdiag.getSubdiagnostics().get(mostSpecificIndex).getArgs()[1].toString();
        } else {
            return null;
        }
    }
}
