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
 * @bug 8002099 8006694 8129962
 * @summary Add support for intersection types in cast expression
 *  temporarily workaround combo tests are causing time out in several platforms
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper

 * @run main IntersectionTypeCastTest
 */

import com.sun.tools.javac.util.List;

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;

import java.io.IOException;

public class IntersectionTypeCastTest extends ComboInstance<IntersectionTypeCastTest> {

    interface Type extends ComboParameter {
        boolean subtypeOf(Type that);
        boolean isClass();
        boolean isInterface();
    }

    enum InterfaceKind implements Type {
        A("A", null),
        B("B", null),
        C("C", A);

        String typeStr;
        InterfaceKind superInterface;

        InterfaceKind(String typeStr, InterfaceKind superInterface) {
            this.typeStr = typeStr;
            this.superInterface = superInterface;
        }

        @Override
        public boolean subtypeOf(Type that) {
            return this == that || superInterface == that ||
                   that == ClassKind.OBJECT;
        }

        @Override
        public boolean isClass() {
            return false;
        }

        @Override
        public boolean isInterface() {
            return true;
        }

        @Override
        public String expand(String optParameter) {
            return typeStr;
        }
    }

    enum ClassKind implements Type {
        OBJECT("Object"),
        CA("CA", InterfaceKind.A),
        CB("CB", InterfaceKind.B),
        CAB("CAB", InterfaceKind.A, InterfaceKind.B),
        CC("CC", InterfaceKind.C, InterfaceKind.A),
        CCA("CCA", InterfaceKind.C, InterfaceKind.A),
        CCB("CCB", InterfaceKind.C, InterfaceKind.A, InterfaceKind.B),
        CCAB("CCAB", InterfaceKind.C, InterfaceKind.A, InterfaceKind.B);

        String typeStr;
        List<InterfaceKind> superInterfaces;

        ClassKind(String typeStr, InterfaceKind... superInterfaces) {
            this.typeStr = typeStr;
            this.superInterfaces = List.from(superInterfaces);
        }

        @Override
        public boolean subtypeOf(Type that) {
            return this == that || superInterfaces.contains(that) ||
                    that == OBJECT;
        }

        @Override
        public boolean isClass() {
            return true;
        }

        @Override
        public boolean isInterface() {
            return false;
        }

        @Override
        public String expand(String optParameter) {
            return typeStr;
        }
    }

    enum ModifierKind implements ComboParameter {
        NONE(""),
        FINAL("final");

        String modStr;

        ModifierKind(String modStr) {
            this.modStr = modStr;
        }

        @Override
        public String expand(String optParameter) {
            return modStr;
        }
    }

    enum CastKind implements ComboParameter {
        CLASS("(#{CLAZZ#IDX})", 0),
        INTERFACE("(#{INTF1#IDX})", 1),
        INTERSECTION2("(#{CLAZZ#IDX} & #{INTF1#IDX})", 1),
        INTERSECTION3("(#{CLAZZ#IDX} & #{INTF1#IDX} & #{INTF2#IDX})", 2);

        String castTemplate;
        int interfaceBounds;

        CastKind(String castTemplate, int interfaceBounds) {
            this.castTemplate = castTemplate;
            this.interfaceBounds = interfaceBounds;
        }

        @Override
        public String expand(String optParameter) {
            return castTemplate.replaceAll("#IDX", optParameter);
        }
    }

    static class CastInfo {
        CastKind kind;
        Type[] types;

        CastInfo(CastKind kind, Type... types) {
            this.kind = kind;
            this.types = types;
        }

        boolean hasDuplicateTypes() {
            for (int i = 0 ; i < arity() ; i++) {
                for (int j = 0 ; j < arity() ; j++) {
                    if (i != j && types[i] == types[j]) {
                        return true;
                    }
                }
            }
            return false;
        }

        boolean compatibleWith(ModifierKind mod, CastInfo that) {
            for (int i = 0 ; i < arity() ; i++) {
                Type t1 = types[i];
                for (int j = 0 ; j < that.arity() ; j++) {
                    Type t2 = that.types[j];
                    boolean compat =
                            t1.subtypeOf(t2) ||
                            t2.subtypeOf(t1) ||
                            (t1.isInterface() && t2.isInterface()) || //side-cast (1)
                            (mod == ModifierKind.NONE &&
                            (t1.isInterface() != t2.isInterface())); //side-cast (2)
                    if (!compat) return false;
                }
            }
            return true;
        }

        private int arity() {
            return kind.interfaceBounds + 1;
        }
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<IntersectionTypeCastTest>()
                .withFilter(IntersectionTypeCastTest::isRedundantCast)
                .withFilter(IntersectionTypeCastTest::arityFilter)
                .withArrayDimension("CAST", (x, ck, idx) -> x.castKinds[idx] = ck, 2, CastKind.values())
                .withDimension("CLAZZ1", (x, ty) -> x.types1[0] = ty, ClassKind.values())
                .withDimension("INTF11", (x, ty) -> x.types1[1] = ty, InterfaceKind.values())
                .withDimension("INTF21", (x, ty) -> x.types1[2] = ty, InterfaceKind.values())
                .withDimension("CLAZZ2", (x, ty) -> x.types2[0] = ty, ClassKind.values())
                .withDimension("INTF12", (x, ty) -> x.types2[1] = ty, InterfaceKind.values())
                .withDimension("INTF22", (x, ty) -> x.types2[2] = ty, InterfaceKind.values())
                .withDimension("MOD", (x, mod) -> x.mod = mod, ModifierKind.values())
                .run(IntersectionTypeCastTest::new);
    }

    boolean isRedundantCast() {
        for (int i = 0 ; i < 2 ; i++) {
            Type[] types = i == 0 ? types1 : types2;
            if (castKinds[i] == CastKind.INTERFACE && types[0] != ClassKind.OBJECT) {
                return false;
            }
        }
        return true;
    }

    boolean arityFilter() {
        for (int i = 0 ; i < 2 ; i++) {
            int lastPos = castKinds[i].interfaceBounds + 1;
            Type[] types = i == 0 ? types1 : types2;
            for (int j = 1; j < types.length; j++) {
                boolean shouldBeSet = j < lastPos;
                if (!shouldBeSet && (types[j] != InterfaceKind.A)) {
                    return false;
                }
            }
        }
        return true;
    }

    ModifierKind mod;
    CastKind[] castKinds = new CastKind[2];
    Type[] types1 = new Type[3];
    Type[] types2 = new Type[3];

    @Override
    public void doWork() throws IOException {
        newCompilationTask()
                .withSourceFromTemplate(bodyTemplate)
                .analyze(this::check);
    }

    String bodyTemplate = "class Test {\n" +
                          "   void test() {\n" +
                          "      Object o = #{CAST[0].1}#{CAST[1].2}null;\n" +
                          "   } }\n" +
                          "interface A { }\n" +
                          "interface B { }\n" +
                          "interface C extends A { }\n" +
                          "#{MOD} class CA implements A { }\n" +
                          "#{MOD} class CB implements B { }\n" +
                          "#{MOD} class CAB implements A, B { }\n" +
                          "#{MOD} class CC implements C { }\n" +
                          "#{MOD} class CCA implements C, A { }\n" +
                          "#{MOD} class CCB implements C, B { }\n" +
                          "#{MOD} class CCAB implements C, A, B { }";

    void check(Result<?> res) {
        CastInfo cast1 = new CastInfo(castKinds[0], types1);
        CastInfo cast2 = new CastInfo(castKinds[1], types2);
        boolean errorExpected = cast1.hasDuplicateTypes() ||
                cast2.hasDuplicateTypes();

        errorExpected |= !cast2.compatibleWith(mod, cast1);

        boolean errorsFound = res.hasErrors();
        if (errorExpected != errorsFound) {
            fail("invalid diagnostics for source:\n" +
                res.compilationInfo() +
                "\nFound error: " + errorsFound +
                "\nExpected error: " + errorExpected);
        }
    }
}
