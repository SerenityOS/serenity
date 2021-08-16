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
 * @bug 7115052 8003280 8006694 8129962
 * @summary Add lambda tests
 *  Add parser support for method references
 *  temporarily workaround combo tests are causing time out in several platforms
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @run main MethodReferenceParserTest
 */

import java.io.IOException;

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;

public class MethodReferenceParserTest extends ComboInstance<MethodReferenceParserTest> {

    enum ReferenceKind implements ComboParameter {
        METHOD_REF("#{QUAL}::#{TARGS}m"),
        CONSTRUCTOR_REF("#{QUAL}::#{TARGS}new"),
        FALSE_REF("min < max"),
        ERR_SUPER("#{QUAL}::#{TARGS}super"),
        ERR_METH0("#{QUAL}::#{TARGS}m()"),
        ERR_METH1("#{QUAL}::#{TARGS}m(X)"),
        ERR_CONSTR0("#{QUAL}::#{TARGS}new()"),
        ERR_CONSTR1("#{QUAL}::#{TARGS}new(X)");

        String referenceTemplate;

        ReferenceKind(String referenceTemplate) {
            this.referenceTemplate = referenceTemplate;
        }

        boolean erroneous() {
            switch (this) {
                case ERR_SUPER:
                case ERR_METH0:
                case ERR_METH1:
                case ERR_CONSTR0:
                case ERR_CONSTR1:
                    return true;
                default: return false;
            }
        }

        @Override
        public String expand(String optParameter) {
            return referenceTemplate;
        }
    }

    enum ContextKind implements ComboParameter {
        ASSIGN("SAM s = #{EXPR};"),
        METHOD("m(#{EXPR}, i);");

        String contextTemplate;

        ContextKind(String contextTemplate) {
            this.contextTemplate = contextTemplate;
        }

        @Override
        public String expand(String optParameter) {
            return contextTemplate;
        }
    }

    enum GenericKind implements ComboParameter {
        NONE(""),
        ONE("<X>"),
        TWO("<X,Y>");

        String typeParameters;

        GenericKind(String typeParameters) {
            this.typeParameters = typeParameters;
        }

        @Override
        public String expand(String optParameter) {
            return typeParameters;
        }
    }

    enum QualifierKind implements ComboParameter {
        THIS("this"),
        SUPER("super"),
        NEW("new Foo()"),
        METHOD("m()"),
        FIELD("a.f"),
        UBOUND_SIMPLE("A"),
        UNBOUND_ARRAY1("int[]"),
        UNBOUND_ARRAY2("A<G>[][]"),
        UNBOUND_GENERIC1("A<X>"),
        UNBOUND_GENERIC2("A<X, Y>"),
        UNBOUND_GENERIC3("A<? extends X, ? super Y>"),
        UNBOUND_GENERIC4("A<int[], short[][]>"),
        NESTED_GENERIC1("A<A<X,Y>, A<X,Y>>"),
        NESTED_GENERIC2("A<A<A<X,Y>,A<X,Y>>, A<A<X,Y>,A<X,Y>>>");

        String qualifier;

        QualifierKind(String qualifier) {
            this.qualifier = qualifier;
        }

        @Override
        public String expand(String optParameter) {
            return qualifier;
        }
    }

    enum ExprKind implements ComboParameter {
        NONE("#{MREF}"),
        SINGLE_PAREN1("(#{MREF}#{SUBEXPR})"),
        SINGLE_PAREN2("(#{MREF})#{SUBEXPR}"),
        DOUBLE_PAREN1("((#{MREF}#{SUBEXPR}))"),
        DOUBLE_PAREN2("((#{MREF})#{SUBEXPR})"),
        DOUBLE_PAREN3("((#{MREF}))#{SUBEXPR}");

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
        new ComboTestHelper<MethodReferenceParserTest>()
                .withDimension("MREF", (x, ref) -> x.rk = ref, ReferenceKind.values())
                .withDimension("QUAL", QualifierKind.values())
                .withDimension("TARGS", GenericKind.values())
                .withDimension("EXPR", ExprKind.values())
                .withDimension("SUBEXPR", SubExprKind.values())
                .withDimension("CTX", ContextKind.values())
                .run(MethodReferenceParserTest::new);
    }

    ReferenceKind rk;

    String template = "class Test {\n" +
                      "   void test() {\n" +
                      "      #{CTX}\n" +
                      "   }" +
                      "}";

    @Override
    public void doWork() throws IOException {
        newCompilationTask()
                .withSourceFromTemplate(template)
                .parse(this::check);
    }

    void check(Result<?> res) {
        if (res.hasErrors() != rk.erroneous()) {
            fail("invalid diagnostics for source:\n" +
                res.compilationInfo() +
                "\nFound error: " + res.hasErrors() +
                "\nExpected error: " + rk.erroneous());
        }
    }
}
