/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7115050 8003280 8005852 8006694 8129962
 * @summary Add lambda tests
 *  Add parser support for lambda expressions
 *  temporarily workaround combo tests are causing time out in several platforms
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper

 * @run main LambdaParserTest
 */

import java.io.IOException;
import java.util.Arrays;

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;

public class LambdaParserTest extends ComboInstance<LambdaParserTest> {

    enum LambdaKind implements ComboParameter {
        NILARY_EXPR("()->x"),
        NILARY_STMT("()->{ return x; }"),
        ONEARY_SHORT_EXPR("#{NAME}->x"),
        ONEARY_SHORT_STMT("#{NAME}->{ return x; }"),
        ONEARY_EXPR("(#{MOD[0]} #{TYPE[0]} #{NAME})->x"),
        ONEARY_STMT("(#{MOD[0]} #{TYPE[0]} #{NAME})->{ return x; }"),
        TWOARY_EXPR("(#{MOD[0]} #{TYPE[0]} #{NAME}, #{MOD[1]} #{TYPE[1]} y)->x"),
        TWOARY_STMT("(#{MOD[0]} #{TYPE[0]} #{NAME}, #{MOD[1]} #{TYPE[1]} y)->{ return x; }");

        String lambdaTemplate;

        LambdaKind(String lambdaTemplate) {
            this.lambdaTemplate = lambdaTemplate;
        }

        @Override
        public String expand(String optParameter) {
            return lambdaTemplate;
        }

        int arity() {
            switch (this) {
                case NILARY_EXPR:
                case NILARY_STMT: return 0;
                case ONEARY_SHORT_EXPR:
                case ONEARY_SHORT_STMT:
                case ONEARY_EXPR:
                case ONEARY_STMT: return 1;
                case TWOARY_EXPR:
                case TWOARY_STMT: return 2;
                default: throw new AssertionError("Invalid lambda kind " + this);
            }
        }

        boolean isShort() {
            return this == ONEARY_SHORT_EXPR ||
                    this == ONEARY_SHORT_STMT;
        }
    }

    enum LambdaParameterName implements ComboParameter {
        IDENT("x"),
        UNDERSCORE("_");

        String nameStr;

        LambdaParameterName(String nameStr) {
            this.nameStr = nameStr;
        }

        @Override
        public String expand(String optParameter) {
            return nameStr;
        }
    }

    enum SourceKind {
        SOURCE_10("10"),
        SOURCE_11("11");

        String sourceNumber;

        SourceKind(String sourceNumber) {
            this.sourceNumber = sourceNumber;
        }
    }

    enum LambdaParameterKind implements ComboParameter {

        IMPLICIT_1("", ExplicitKind.IMPLICIT),
        IMPLICIT_2("var", ExplicitKind.IMPLICIT_VAR),
        EXPLICIT_SIMPLE("A", ExplicitKind.EXPLICIT),
        EXPLICIT_SIMPLE_ARR1("A[]", ExplicitKind.EXPLICIT),
        EXPLICIT_SIMPLE_ARR2("A[][]", ExplicitKind.EXPLICIT),
        EXPLICIT_VARARGS("A...", ExplicitKind.EXPLICIT),
        EXPLICIT_GENERIC1("A<X>", ExplicitKind.EXPLICIT),
        EXPLICIT_GENERIC2("A<? extends X, ? super Y>", ExplicitKind.EXPLICIT),
        EXPLICIT_GENERIC2_VARARGS("A<? extends X, ? super Y>...", ExplicitKind.EXPLICIT),
        EXPLICIT_GENERIC2_ARR1("A<? extends X, ? super Y>[]", ExplicitKind.EXPLICIT),
        EXPLICIT_GENERIC2_ARR2("A<? extends X, ? super Y>[][]", ExplicitKind.EXPLICIT);

        enum ExplicitKind {
            IMPLICIT,
            IMPLICIT_VAR,
            EXPLICIT;
        }

        String parameterType;
        ExplicitKind explicitKind;


        LambdaParameterKind(String parameterType, ExplicitKind ekind) {
            this.parameterType = parameterType;
            this.explicitKind = ekind;
        }

        boolean isVarargs() {
            return this == EXPLICIT_VARARGS ||
                    this == EXPLICIT_GENERIC2_VARARGS;
        }

        @Override
        public String expand(String optParameter) {
            return parameterType;
        }

        ExplicitKind explicitKind(SourceKind sk) {
            return explicitKind;
        }
    }

    enum ModifierKind implements ComboParameter {
        NONE(""),
        FINAL("final"),
        PUBLIC("public"),
        ANNO("@A");

        String modifier;

        ModifierKind(String modifier) {
            this.modifier = modifier;
        }

        boolean compatibleWith(LambdaParameterKind pk) {
            switch (this) {
                case PUBLIC: return false;
                case ANNO:
                case FINAL: return pk != LambdaParameterKind.IMPLICIT_1;
                case NONE: return true;
                default: throw new AssertionError("Invalid modifier kind " + this);
            }
        }

        @Override
        public String expand(String optParameter) {
            return modifier;
        }
    }

    enum ExprKind implements ComboParameter {
        NONE("#{LAMBDA}#{SUBEXPR}"),
        SINGLE_PAREN1("(#{LAMBDA}#{SUBEXPR})"),
        SINGLE_PAREN2("(#{LAMBDA})#{SUBEXPR}"),
        DOUBLE_PAREN1("((#{LAMBDA}#{SUBEXPR}))"),
        DOUBLE_PAREN2("((#{LAMBDA})#{SUBEXPR})"),
        DOUBLE_PAREN3("((#{LAMBDA}))#{SUBEXPR}");

        String expressionTemplate;

        ExprKind(String expressionTemplate) {
            this.expressionTemplate = expressionTemplate;
        }

        @Override
        public String expand(String optParameter) {
            return expressionTemplate;
        }
    }

    enum SubExprKind implements ComboParameter {
        NONE(""),
        SELECT_FIELD(".f"),
        SELECT_METHOD(".f()"),
        SELECT_NEW(".new Foo()"),
        POSTINC("++"),
        POSTDEC("--");

        String subExpression;

        SubExprKind(String subExpression) {
            this.subExpression = subExpression;
        }

        @Override
        public String expand(String optParameter) {
            return subExpression;
        }
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<LambdaParserTest>()
                .withFilter(LambdaParserTest::redundantTestFilter)
                .withDimension("SOURCE", (x, sk) -> x.sk = sk, SourceKind.values())
                .withDimension("LAMBDA", (x, lk) -> x.lk = lk, LambdaKind.values())
                .withDimension("NAME", (x, name) -> x.pn = name, LambdaParameterName.values())
                .withArrayDimension("TYPE", (x, type, idx) -> x.pks[idx] = type, 2, LambdaParameterKind.values())
                .withArrayDimension("MOD", (x, mod, idx) -> x.mks[idx] = mod, 2, ModifierKind.values())
                .withDimension("EXPR", (x, exp) -> x.exp = exp, ExprKind.values())
                .withDimension("SUBEXPR", (x, sub) -> x.sub = sub, SubExprKind.values())
                .run(LambdaParserTest::new);
    }

    LambdaParameterKind[] pks = new LambdaParameterKind[2];
    ModifierKind[] mks = new ModifierKind[2];
    LambdaKind lk;
    LambdaParameterName pn;
    SourceKind sk;
    ExprKind exp;
    SubExprKind sub;

    boolean redundantTestFilter() {
        if (sub == SubExprKind.NONE) {
            switch (exp) {
                //followings combinations with empty sub-expressions produces the same source
                case SINGLE_PAREN2, DOUBLE_PAREN2, DOUBLE_PAREN3: return false;
            }
        } else {
            switch (lk) {
                //any non-empty subexpression does not combine with lambda statements
                case NILARY_STMT, ONEARY_SHORT_STMT, ONEARY_STMT, TWOARY_STMT: return false;
            }
        }
        switch (lk) {
            //parameters not present in the expression are redundant
            case NILARY_EXPR, NILARY_STMT:
                if (pn.ordinal() != 0) return false;
            case ONEARY_SHORT_EXPR, ONEARY_SHORT_STMT:
                if (pks[0].ordinal() != 0 || mks[0].ordinal() != 0) return false;
            case ONEARY_EXPR, ONEARY_STMT :
                if (pks[1].ordinal() != 0 || mks[1].ordinal() != 0) return false;
        }
        return true;
    }

    String template = "@interface A { }\n" +
            "class Test {\n" +
            "   SAM s = #{EXPR};\n" +
            "}";

    @Override
    public void doWork() throws IOException {
        newCompilationTask()
                .withOptions(Arrays.asList("-source", sk.sourceNumber))
                .withSourceFromTemplate(template)
                .parse(this::check);
    }

    void check(Result<?> res) {
        boolean errorExpected = (lk.arity() > 0 && !mks[0].compatibleWith(pks[0])) ||
                (lk.arity() > 1 && !mks[1].compatibleWith(pks[1]));

        if (lk.arity() == 2 &&
                (pks[0].explicitKind(sk) != pks[1].explicitKind(sk) ||
                pks[0].isVarargs())) {
            errorExpected = true;
        }

        errorExpected |= pn == LambdaParameterName.UNDERSCORE &&
                lk.arity() > 0;

        for (int i = 0; i < lk.arity(); i++) {
            if (!lk.isShort() &&
                    pks[i].explicitKind(sk) == LambdaParameterKind.ExplicitKind.IMPLICIT_VAR &&
                    sk == SourceKind.SOURCE_10) {
                errorExpected = true;
                break;
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
