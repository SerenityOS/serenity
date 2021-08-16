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
 * @bug 8005166 8129962
 * @summary Add support for static interface methods
 *          Smoke test for static interface method hiding
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @run main InterfaceMethodHidingTest
 */

import java.io.IOException;

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;

public class InterfaceMethodHidingTest extends ComboInstance<InterfaceMethodHidingTest> {

    enum SignatureKind implements ComboParameter {
        VOID_INTEGER("void m(Integer s)", false),
        STRING_INTEGER("String m(Integer s)", true),
        VOID_STRING("void m(String s)", false),
        STRING_STRING("String m(String s)", true);

        String sigStr;
        boolean needsReturn;

        SignatureKind(String sigStr, boolean needsReturn) {
            this.sigStr = sigStr;
            this.needsReturn = needsReturn;
        }

        boolean overrideEquivalentWith(SignatureKind s2) {
            switch (this) {
                case VOID_INTEGER:
                case STRING_INTEGER:
                    return s2 == VOID_INTEGER || s2 == STRING_INTEGER;
                case VOID_STRING:
                case STRING_STRING:
                    return s2 == VOID_STRING || s2 == STRING_STRING;
                default:
                    throw new AssertionError("bad signature kind");
            }
        }

        @Override
        public String expand(String optParameter) {
            return sigStr;
        }
    }

    enum MethodKind implements ComboParameter {
        VIRTUAL("#{SIG[#IDX]};"),
        STATIC("static #{SIG[#IDX]} { #{BODY[#IDX]}; #{RET.#IDX} }"),
        DEFAULT("default #{SIG[#IDX]} { #{BODY[#IDX]}; #{RET.#IDX} }");

        String methTemplate;

        MethodKind(String methTemplate) {
            this.methTemplate = methTemplate;
        }

        boolean inherithed() {
            return this != STATIC;
        }

        static boolean overrides(MethodKind mk1, SignatureKind sk1, MethodKind mk2, SignatureKind sk2) {
            return sk1 == sk2 &&
                    mk2.inherithed() &&
                    mk1 != STATIC;
        }

        @Override
        public String expand(String optParameter) {
            return methTemplate.replaceAll("#IDX", optParameter);
        }
    }

    enum BodyExpr implements ComboParameter {
        NONE(""),
        THIS("Object o = this");

        String bodyExprStr;

        BodyExpr(String bodyExprStr) {
            this.bodyExprStr = bodyExprStr;
        }

        boolean allowed(MethodKind mk) {
            return this == NONE ||
                    mk != MethodKind.STATIC;
        }

        @Override
        public String expand(String optParameter) {
            return bodyExprStr;
        }
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<InterfaceMethodHidingTest>()
                .withArrayDimension("SIG", (x, sig, idx) -> x.signatureKinds[idx] = sig, 3, SignatureKind.values())
                .withArrayDimension("BODY", (x, body, idx) -> x.bodyExprs[idx] = body, 3, BodyExpr.values())
                .withArrayDimension("MET", (x, meth, idx) -> x.methodKinds[idx] = meth, 3, MethodKind.values())
                .run(InterfaceMethodHidingTest::new);
    }

    MethodKind[] methodKinds = new MethodKind[3];
    SignatureKind[] signatureKinds = new SignatureKind[3];
    BodyExpr[] bodyExprs = new BodyExpr[3];

    String template = "interface Sup {\n" +
                          "   default void sup() { }\n" +
                          "}\n" +
                          "interface A extends Sup {\n" +
                          "   #{MET[0].0}\n" +
                          "}\n" +
                          "interface B extends A, Sup {\n" +
                          "   #{MET[1].1}\n" +
                          "}\n" +
                          "interface C extends B, Sup {\n" +
                          "   #{MET[2].2}\n" +
                          "}\n";

    @Override
    public void doWork() throws IOException {
        newCompilationTask()
                .withOption("-XDallowStaticInterfaceMethods")
                .withSourceFromTemplate(template, this::returnExpr)
                .analyze(this::check);
    }

    ComboParameter returnExpr(String name) {
        switch (name) {
            case "RET":
                return optParameter -> {
                    int idx = new Integer(optParameter);
                    return signatureKinds[idx].needsReturn ? "return null;" : "return;";
                };
            default:
                return null;
        }
    }

    void check(Result<?> res) {
        boolean errorExpected = !bodyExprs[0].allowed(methodKinds[0]) ||
                !bodyExprs[1].allowed(methodKinds[1]) ||
                !bodyExprs[2].allowed(methodKinds[2]);

        if (methodKinds[0].inherithed()) {
            errorExpected |= signatureKinds[1].overrideEquivalentWith(signatureKinds[0]) &&
                    !MethodKind.overrides(methodKinds[1], signatureKinds[1], methodKinds[0], signatureKinds[0]) ||
                    signatureKinds[2].overrideEquivalentWith(signatureKinds[0]) &&
                    !MethodKind.overrides(methodKinds[2], signatureKinds[2], methodKinds[0], signatureKinds[0]);
        }

        if (methodKinds[1].inherithed()) {
            errorExpected |= signatureKinds[2].overrideEquivalentWith(signatureKinds[1]) &&
                    !MethodKind.overrides(methodKinds[2], signatureKinds[2], methodKinds[1], signatureKinds[1]);
        }

        if (res.hasErrors() != errorExpected) {
            fail("Problem when compiling source:\n" + res.compilationInfo() +
                    "\nfound error: " + res.hasErrors());
        }
    }
}
