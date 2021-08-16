/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;

import javax.lang.model.element.Element;
import java.util.stream.Stream;

/*
 * @test
 * @bug 8176534
 * @summary Missing check against target-type during applicability inference
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 *
 * @run main TestUncheckedCalls
 */
public class TestUncheckedCalls extends ComboInstance<TestUncheckedCalls> {
    enum InputExpressionKind implements ComboParameter {
        A("(A)null"),
        A_STRING("(A<String>)null"),
        B("(B)null"),
        B_STRING("(B<String>)null");

        String inputExpr;

        InputExpressionKind(String inputExpr) {
            this.inputExpr = inputExpr;
        }


        @Override
        public String expand(String optParameter) {
            return inputExpr;
        }
    }

    enum TypeKind implements ComboParameter {
        Z("Z"),
        C_T("#C<T>"),
        C_STRING("#C<String>"),
        C("#C");

        String typeTemplate;

        TypeKind(String typeTemplate) {
            this.typeTemplate = typeTemplate;
        }

        boolean hasTypeVars() {
            return this == Z || this == C_T;
        }

        @Override
        public String expand(String className) {
            return typeTemplate.replaceAll("#C", className);
        }
    }

    enum TypeVarsKind implements ComboParameter {
        NONE("", "Object"),
        Z_T("<Z extends #C<T>, T>", "Z");

        String typeVarsTemplate;
        String paramString;

        TypeVarsKind(String typeVarsTemplate, String paramString) {
            this.typeVarsTemplate = typeVarsTemplate;
            this.paramString = paramString;
        }


        @Override
        public String expand(String className) {
            if (className.equals("Z")) {
                return paramString;
            } else {
                return typeVarsTemplate.replaceAll("#C", className);
            }
        }
    }

    enum CallKind implements ComboParameter {
        M("M(#{IN}, #{IN})"),
        M_G("M(G(#{IN}, #{IN}), #{IN})"),
        M_G_G("M(G(#{IN}, #{IN}), G(#{IN}, #{IN}))");

        String callExpr;

        CallKind(String callExpr) {
            this.callExpr = callExpr;
        }


        @Override
        public String expand(String optParameter) {
            return callExpr;
        }
    }

    enum DeclKind implements ComboParameter {
        NONE(""),
        ONE("#{TVARS[#M_IDX].I1} #{RET[#M_IDX].A} #M(#{ARG[#M_IDX].A} x1, #{TVARS[#M_IDX].Z} x2) { return null; }"),
        TWO("#{TVARS[#M_IDX].I1} #{RET[#M_IDX].A} #M(#{ARG[#M_IDX].A} x1, #{TVARS[#M_IDX].Z} x2) { return null; }\n" +
        "    #{TVARS[#M_IDX].I2} #{RET[#M_IDX].B} #M(#{ARG[#M_IDX].B} x1, #{TVARS[#M_IDX].Z} x2) { return null; }");

        String declTemplate;

        DeclKind(String declTemplate) {
            this.declTemplate = declTemplate;
        }

        @Override
        public String expand(String methName) {
            return declTemplate.replaceAll("#M_IDX", methName.equals("M") ? "0" : "1")
                    .replaceAll("#M", methName);

        }
    }

    static final String sourceTemplate =
            "class Test {\n" +
            "   interface I1<X> { }\n" +
            "   interface I2<X> { }\n" +
            "   static class A<X> implements I1<X> { }\n" +
            "   static class B<X> implements I2<X> { }\n" +
            "   #{DECL[0].M}\n" +
            "   #{DECL[1].G}\n" +
            "   void test() {\n" +
            "       #{CALL};\n" +
            "   }\n" +
            "}\n";

    public static void main(String... args) throws Exception {
        new ComboTestHelper<TestUncheckedCalls>()
                .withFilter(TestUncheckedCalls::arityFilter)
                .withFilter(TestUncheckedCalls::declFilter)
                .withFilter(TestUncheckedCalls::tvarFilter)
                .withFilter(TestUncheckedCalls::inputExprFilter)
                .withDimension("IN", (x, expr) -> x.inputExpressionKind = expr, InputExpressionKind.values())
                .withDimension("CALL", (x, expr) -> x.callKind = expr, CallKind.values())
                .withArrayDimension("DECL", (x, decl, idx) -> x.decls[idx] = x.new Decl(decl, idx), 2, DeclKind.values())
                .withArrayDimension("TVARS", (x, tvars, idx) -> x.typeVarsKinds[idx] = tvars, 2, TypeVarsKind.values())
                .withArrayDimension("RET", (x, ret, idx) -> x.returnKinds[idx] = ret, 2, TypeKind.values())
                .withArrayDimension("ARG", (x, arg, idx) -> x.argumentKinds[idx] = arg, 2, TypeKind.values())
                .run(TestUncheckedCalls::new);
    }

    class Decl {
        private DeclKind declKind;
        private int index;

        Decl(DeclKind declKind, int index) {
            this.declKind = declKind;
            this.index = index;
        }

        boolean hasKind(DeclKind declKind) {
            return this.declKind == declKind;
        }

        boolean isGeneric() {
            return typeVarsKind() == TypeVarsKind.Z_T;
        }

        TypeKind returnKind() {
            return returnKinds[index];
        }

        TypeKind argumentsKind() {
            return argumentKinds[index];
        }

        TypeVarsKind typeVarsKind() {
            return typeVarsKinds[index];
        }
    }

    CallKind callKind;
    InputExpressionKind inputExpressionKind;
    TypeKind[] returnKinds = new TypeKind[2];
    TypeKind[] argumentKinds = new TypeKind[2];
    TypeVarsKind[] typeVarsKinds = new TypeVarsKind[2];
    Decl[] decls = new Decl[2];

    boolean arityFilter() {
        return (callKind == CallKind.M || !decls[1].hasKind(DeclKind.NONE)) &&
                !decls[0].hasKind(DeclKind.NONE);
    }

    boolean declFilter() {
        return Stream.of(decls)
                .filter(d -> d.hasKind(DeclKind.NONE))
                .flatMap(d -> Stream.of(d.returnKind(), d.argumentsKind(), d.typeVarsKind()))
                .noneMatch((Enum<? extends Enum<?>> tk) -> tk.ordinal() != 0);
    }

    boolean tvarFilter() {
        return Stream.of(decls)
                .filter(d -> !d.hasKind(DeclKind.NONE))
                .filter(d -> !d.isGeneric())
                .flatMap(d -> Stream.of(d.returnKind(), d.argumentsKind()))
                .noneMatch(TypeKind::hasTypeVars);
    }

    boolean inputExprFilter() {
        return (inputExpressionKind != InputExpressionKind.B && inputExpressionKind != InputExpressionKind.B_STRING) ||
                Stream.of(decls).allMatch(d -> d.declKind == DeclKind.TWO);
    }

    @Override
    public void doWork() throws Throwable {
        newCompilationTask()
                .withSourceFromTemplate(sourceTemplate)
                .analyze(this::check);
    }

    void check(Result<Iterable<? extends Element>> result) {
        if (result.hasErrors()) {
            fail("compiler error:\n" +
                    result.compilationInfo());
        }
    }
}
