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
 * @bug 7062745 8006694 8129962
 * @summary  Regression: difference in overload resolution when two methods
 *  are maximally specific
 *  temporarily workaround combo tests are causing time out in several platforms
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @run main GenericOverrideTest
 */

import java.io.IOException;

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;

public class GenericOverrideTest extends ComboInstance<GenericOverrideTest> {

    enum SourceLevel {
        SOURCE_7("-source", "7"),
        SOURCE_DEFAULT();

        String[] opts;

        SourceLevel(String... opts) {
            this.opts = opts;
        }
    }

    enum SignatureKind implements ComboParameter {
        NON_GENERIC(""),
        GENERIC("<X>");

        String paramStr;

        SignatureKind(String paramStr) {
            this.paramStr = paramStr;
        }

        @Override
        public String expand(String optParameter) {
            return paramStr;
        }
    }

    enum ReturnTypeKind implements ComboParameter {
        LIST("List"),
        ARRAYLIST("ArrayList");

        String retStr;

        ReturnTypeKind(String retStr) {
            this.retStr = retStr;
        }

        boolean moreSpecificThan(ReturnTypeKind that) {
            switch (this) {
                case LIST:
                    return that == this;
                case ARRAYLIST:
                    return that == LIST || that == ARRAYLIST;
                default: throw new AssertionError("Unexpected ret kind: " + this);
            }
        }

        @Override
        public String expand(String optParameter) {
            return retStr;
        }
    }

    enum TypeArgumentKind implements ComboParameter {
        NONE(""),
        UNBOUND("<?>"),
        INTEGER("<Number>"),
        NUMBER("<Integer>"),
        TYPEVAR("<X>");

        String typeargStr;

        TypeArgumentKind(String typeargStr) {
            this.typeargStr = typeargStr;
        }

        boolean compatibleWith(SignatureKind sig) {
            switch (this) {
                case TYPEVAR: return sig != SignatureKind.NON_GENERIC;
                default: return true;
            }
        }

        boolean moreSpecificThan(TypeArgumentKind that) {
            switch (this) {
                case NONE:
                    return that == this;
                case UNBOUND:
                    return that == this || that == NONE;
                case INTEGER:
                case NUMBER:
                case TYPEVAR:
                    return that == this || that == NONE || that == UNBOUND;
                default: throw new AssertionError("Unexpected typearg kind: " + this);
            }
        }

        boolean assignableTo(TypeArgumentKind that, SignatureKind sig, SourceLevel level) {
            switch (this) {
                case NONE:
                    //this case needs to workaround to javac's impl of 15.12.2.8 being too strict
                    //ideally should be just 'return true' (see 7067746/8015505)
                    return level == SourceLevel.SOURCE_DEFAULT ||
                            sig == SignatureKind.NON_GENERIC || that == NONE;
                case UNBOUND:
                    return that == this || that == NONE;
                case INTEGER:
                case NUMBER:
                    return that == this || that == NONE || that == UNBOUND;
                case TYPEVAR:
                    return true;
                default: throw new AssertionError("Unexpected typearg kind: " + this);
            }
        }

        @Override
        public String expand(String optParameter) {
            return typeargStr;
        }
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<GenericOverrideTest>()
                .withFilter(GenericOverrideTest::argMismatchFilter)
                .withDimension("SOURCE", (x, level) -> x.level = level, SourceLevel.values())
                .withArrayDimension("SIG", (x, sig, idx) -> x.sigs[idx] = sig, 2, SignatureKind.values())
                .withArrayDimension("TARG", (x, targ, idx) -> x.targs[idx] = targ, 3, TypeArgumentKind.values())
                .withArrayDimension("RET", (x, ret, idx) -> x.rets[idx] = ret, 3, ReturnTypeKind.values())
                .run(GenericOverrideTest::new);
    }

    SignatureKind[] sigs = new SignatureKind[2];
    ReturnTypeKind[] rets = new ReturnTypeKind[3];
    TypeArgumentKind[] targs = new TypeArgumentKind[3];
    SourceLevel level;

    boolean argMismatchFilter() {
        return targs[0].compatibleWith(sigs[0]) &&
                targs[1].compatibleWith(sigs[1]) &&
                targs[2].compatibleWith(SignatureKind.NON_GENERIC);
    }

    String template = "import java.util.*;\n" +
                      "interface A { #{SIG[0]} #{RET[0]}#{TARG[0]} m(); }\n" +
                      "interface B { #{SIG[1]} #{RET[1]}#{TARG[1]} m(); }\n" +
                      "interface AB extends A, B {}\n" +
                      "class Test {\n" +
                      "  void test(AB ab) { #{RET[2]}#{TARG[2]} n = ab.m(); }\n" +
                      "}";

    @Override
    public void doWork() throws IOException {
        newCompilationTask()
                .withOption("-XDuseUnsharedTable") //this test relies on predictable name indexes!
                .withOptions(level.opts)
                .withSourceFromTemplate(template)
                .analyze(this::check);
    }

    void check(Result<?> res) {
        boolean errorExpected = false;
        boolean loose = false;
        int mostSpecific = 0;

        //first check that either |R1| <: |R2| or |R2| <: |R1|
        if (rets[0] != rets[1]) {
            if (!rets[0].moreSpecificThan(rets[1]) &&
                    !rets[1].moreSpecificThan(rets[0])) {
                errorExpected = true;
            } else {
                mostSpecific = rets[0].moreSpecificThan(rets[1]) ? 1 : 2;
            }
        } else if (sigs[0] != sigs[1]) {
            mostSpecific = sigs[0] == SignatureKind.GENERIC ? 2 : 1;
            loose = true;
        }

        //check that either TA1 <= TA2 or TA2 <= TA1 (unless most specific return found above is raw)
        if (!errorExpected) {
            if (targs[0] != targs[1]) {
                boolean ta1ms = targs[0].moreSpecificThan(targs[1]);
                boolean ta2ms = targs[1].moreSpecificThan(targs[0]);
                if (!ta1ms && !ta2ms) {
                    errorExpected = true;
                } else if (mostSpecific != 0) {
                    errorExpected = !loose && targs[mostSpecific - 1] != TypeArgumentKind.NONE &&
                            (mostSpecific == 1 ? !ta1ms : !ta2ms);
                } else {
                    mostSpecific = ta1ms ? 1 : 2;
                }
            }
        }

        if (mostSpecific == 0) {
            //when no signature is better than the other, an arbitrary choice
            //must be made - javac always picks the second signature
            mostSpecific = 2;
        }

        if (!errorExpected) {
            ReturnTypeKind msrt = mostSpecific == 1 ? rets[0] : rets[1];
            TypeArgumentKind msta = mostSpecific == 1 ? targs[0] : targs[1];
            SignatureKind mssig = mostSpecific == 1 ? sigs[0] : sigs[1];

            //check that most specific is subsignature
            errorExpected = sigs[0] != sigs[1] &&
                    mssig == SignatureKind.GENERIC;

            //finally, check that most specific return type is compatible with expected type
            if (!msrt.moreSpecificThan(rets[2]) ||
                    !msta.assignableTo(targs[2], mssig, level)) {
                errorExpected = true;
            }
        }

        if (errorExpected != res.hasErrors()) {
            fail("invalid diagnostics for source:\n" +
                res.compilationInfo() +
                "\nFound error: " + res.hasErrors() +
                "\nExpected error: " + errorExpected);
        }
    }
}
