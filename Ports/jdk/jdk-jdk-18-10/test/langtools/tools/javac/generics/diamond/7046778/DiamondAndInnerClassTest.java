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
 * @bug 7046778 8006694 8129962
 * @summary Project Coin: problem with diamond and member inner classes
 *  temporarily workaround combo tests are causing time out in several platforms
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @compile -Xlint:all DiamondAndInnerClassTest.java
 * @run main DiamondAndInnerClassTest
 */

import java.io.IOException;
import java.util.Arrays;

import combo.ComboTestHelper;
import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;

public class DiamondAndInnerClassTest extends ComboInstance<DiamondAndInnerClassTest> {

    enum TypeArgumentKind implements ComboParameter {
        NONE(""),
        STRING("<String>"),
        INTEGER("<Integer>"),
        DIAMOND("<>");

        String typeargStr;

        TypeArgumentKind(String typeargStr) {
            this.typeargStr = typeargStr;
        }

        boolean compatible(TypeArgumentKind that) {
            switch (this) {
                case NONE: return true;
                case STRING: return that != INTEGER;
                case INTEGER: return that != STRING;
                default: throw new AssertionError("Unexpected decl kind: " + this);
            }
        }

        boolean compatible(ArgumentKind that) {
            switch (this) {
                case NONE: return true;
                case STRING: return that == ArgumentKind.STRING;
                case INTEGER: return that == ArgumentKind.INTEGER;
                default: throw new AssertionError("Unexpected decl kind: " + this);
            }
        }

        @Override
        public String expand(String optParameter) {
            return typeargStr;
        }
    }

    enum ArgumentKind implements ComboParameter {
        OBJECT("(Object)null"),
        STRING("(String)null"),
        INTEGER("(Integer)null");

        String argStr;

        ArgumentKind(String argStr) {
            this.argStr = argStr;
        }

        @Override
        public String expand(String optParameter) {
            return argStr;
        }
    }

    enum TypeQualifierArity implements ComboParameter {
        ONE(1, "A1#{TA#IDX[0]}"),
        TWO(2, "A1#{TA#IDX[0]}.A2#{TA#IDX[1]}"),
        THREE(3, "A1#{TA#IDX[0]}.A2#{TA#IDX[1]}.A3#{TA#IDX[2]}");

        int n;
        String qualifierStr;

        TypeQualifierArity(int n, String qualifierStr) {
            this.n = n;
            this.qualifierStr = qualifierStr;
        }

        @Override
        public String expand(String optParameter) {
            return qualifierStr.replaceAll("#IDX", optParameter);
        }
    }

    enum InnerClassDeclArity implements ComboParameter {
        ONE(1, "class A1<X> { A1(X x1) { } #{BODY} }"),
        TWO(2, "class A1<X1> { class A2<X2> { A2(X1 x1, X2 x2) { }  #{BODY} } }"),
        THREE(3, "class A1<X1> { class A2<X2> { class A3<X3> { A3(X1 x1, X2 x2, X3 x3) { } #{BODY} } } }");

        int n;
        String classDeclStr;

        InnerClassDeclArity(int n, String classDeclStr) {
            this.n = n;
            this.classDeclStr = classDeclStr;
        }

        @Override
        public String expand(String optParameter) {
            return classDeclStr;
        }
    }

    enum ArgumentListArity implements ComboParameter {
        ONE(1, "(#{A[0]})"),
        TWO(2, "(#{A[0]},#{A[1]})"),
        THREE(3, "(#{A[0]},#{A[1]},#{A[2]})");

        int n;
        String argListStr;

        ArgumentListArity(int n, String argListStr) {
            this.n = n;
            this.argListStr = argListStr;
        }

        @Override
        public String expand(String optParameter) {
            return argListStr.replaceAll("#IDX", optParameter);
        }
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<DiamondAndInnerClassTest>()
                .withFilter(DiamondAndInnerClassTest::rareTypesFilter)
                .withFilter(DiamondAndInnerClassTest::noDiamondOnDecl)
                .withFilter(DiamondAndInnerClassTest::noDiamondOnIntermediateTypes)
                .withFilter(DiamondAndInnerClassTest::arityMismatch)
                .withFilter(DiamondAndInnerClassTest::redundantFilter)
                .withDimension("BODY", new ComboParameter.Constant<>("#{D.1} res = new #{S.2}#{AL};"))
                .withDimension("DECL", (x, arity) -> x.innerClassDeclArity = arity, InnerClassDeclArity.values())
                .withDimension("D", (x, arity) -> x.declArity = arity, TypeQualifierArity.values())
                .withDimension("S", (x, arity) -> x.siteArity = arity, TypeQualifierArity.values())
                .withDimension("AL", (x, alist) -> x.argumentListArity = alist, ArgumentListArity.values())
                .withArrayDimension("TA1", (x, targs, idx) -> x.declTypeArgumentKinds[idx] = targs, 3, TypeArgumentKind.values())
                .withArrayDimension("TA2", (x, targs, idx) -> x.siteTypeArgumentKinds[idx] = targs, 3, TypeArgumentKind.values())
                .withArrayDimension("A", (x, argsk, idx) -> x.argumentKinds[idx] = argsk, 3, ArgumentKind.values())
                .run(DiamondAndInnerClassTest::new);
    }

    InnerClassDeclArity innerClassDeclArity;
    TypeQualifierArity declArity;
    TypeQualifierArity siteArity;
    TypeArgumentKind[] declTypeArgumentKinds = new TypeArgumentKind[3];
    TypeArgumentKind[] siteTypeArgumentKinds = new TypeArgumentKind[3];
    ArgumentKind[] argumentKinds = new ArgumentKind[3];
    ArgumentListArity argumentListArity;

    boolean rareTypesFilter() {
        for (TypeArgumentKind[] types : Arrays.asList(declTypeArgumentKinds, siteTypeArgumentKinds)) {
            boolean isRaw = types[0] == TypeArgumentKind.NONE;
            for (int i = 1; i < innerClassDeclArity.n; i++) {
                if (isRaw != (types[i] == TypeArgumentKind.NONE)) {
                    return false;
                }
            }
        }
        return true;
    }

    boolean noDiamondOnDecl() {
        for (int i = 0; i < innerClassDeclArity.n; i++) {
            if (declTypeArgumentKinds[i] == TypeArgumentKind.DIAMOND) {
                return false;
            }
        }
        return true;
    }

    boolean noDiamondOnIntermediateTypes() {
        for (int i = 0; i < (innerClassDeclArity.n - 1); i++) {
            if (siteTypeArgumentKinds[i] == TypeArgumentKind.DIAMOND) {
                return false;
            }
        }
        return true;
    }

    boolean redundantFilter() {
        for (TypeArgumentKind[] types : Arrays.asList(declTypeArgumentKinds, siteTypeArgumentKinds)) {
            for (int i = innerClassDeclArity.n; i < types.length; i++) {
                if (types[i].ordinal() != 0) {
                    return false;
                }
            }
        }
        for (int i = innerClassDeclArity.n; i < argumentKinds.length; i++) {
            if (argumentKinds[i].ordinal() != 0) {
                return false;
            }
        }
        return true;
    }

    boolean arityMismatch() {
        return argumentListArity.n == innerClassDeclArity.n &&
                siteArity.n == innerClassDeclArity.n &&
                declArity.n == innerClassDeclArity.n;
    }

    @Override
    public void doWork() throws IOException {
        newCompilationTask()
                .withSourceFromTemplate("#{DECL}")
                .analyze(this::check);
    }

    void check(Result<?> res) {
        boolean errorExpected = false;

        TypeArgumentKind[] expectedArgKinds =
                new TypeArgumentKind[innerClassDeclArity.n];

        for (int i = 0 ; i < innerClassDeclArity.n ; i++) {
            if (!declTypeArgumentKinds[i].compatible(siteTypeArgumentKinds[i])) {
                errorExpected = true;
                break;
            }
            expectedArgKinds[i] = siteTypeArgumentKinds[i] ==
                    TypeArgumentKind.DIAMOND ?
                declTypeArgumentKinds[i] : siteTypeArgumentKinds[i];
        }

        if (!errorExpected) {
            for (int i = 0 ; i < innerClassDeclArity.n ; i++) {
                if (!expectedArgKinds[i].compatible(argumentKinds[i])) {
                    errorExpected = true;
                    break;
                }
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
