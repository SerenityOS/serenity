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
 * @bug 7042566 8006694 8129962
 * @summary Unambiguous varargs method calls flagged as ambiguous
 *  temporarily workaround combo tests are causing time out in several platforms
 * @library /tools/javac/lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @run main T7042566
 */

import java.io.IOException;
import java.io.InputStream;
import javax.tools.JavaFileObject;

import com.sun.tools.classfile.Instruction;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPool.*;
import com.sun.tools.classfile.Method;
import com.sun.tools.javac.util.List;

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;

public class T7042566 extends ComboInstance<T7042566> {

    enum TypeKind {
        OBJECT("Object", "(Object)null", "Ljava/lang/Object;"),
        STRING("String", "(String)null", "Ljava/lang/String;");

        String typeString;
        String valueString;
        String bytecodeString;

        TypeKind(String typeString, String valueString, String bytecodeString) {
            this.typeString = typeString;
            this.valueString = valueString;
            this.bytecodeString = bytecodeString;
        }

        boolean isSubtypeOf(TypeKind that) {
            return that == OBJECT ||
                    (that == STRING && this == STRING);
        }
    }

    enum TypeConfiguration implements ComboParameter {
        A(TypeKind.OBJECT),
        B(TypeKind.STRING),
        AA(TypeKind.OBJECT, TypeKind.OBJECT),
        AB(TypeKind.OBJECT, TypeKind.STRING),
        BA(TypeKind.STRING, TypeKind.OBJECT),
        BB(TypeKind.STRING, TypeKind.STRING),
        AAA(TypeKind.OBJECT, TypeKind.OBJECT, TypeKind.OBJECT),
        AAB(TypeKind.OBJECT, TypeKind.OBJECT, TypeKind.STRING),
        ABA(TypeKind.OBJECT, TypeKind.STRING, TypeKind.OBJECT),
        ABB(TypeKind.OBJECT, TypeKind.STRING, TypeKind.STRING),
        BAA(TypeKind.STRING, TypeKind.OBJECT, TypeKind.OBJECT),
        BAB(TypeKind.STRING, TypeKind.OBJECT, TypeKind.STRING),
        BBA(TypeKind.STRING, TypeKind.STRING, TypeKind.OBJECT),
        BBB(TypeKind.STRING, TypeKind.STRING, TypeKind.STRING);

        List<TypeKind> typeKindList;
        String expressionListStr;
        String parameterListStr;
        String bytecodeSigStr;

        TypeConfiguration(TypeKind... typeKindList) {
            this.typeKindList = List.from(typeKindList);
            expressionListStr = asExpressionList();
            parameterListStr = asParameterList();
            bytecodeSigStr = asBytecodeString();
        }

        private String asExpressionList() {
            StringBuilder buf = new StringBuilder();
            String sep = "";
            for (TypeKind tk : typeKindList) {
                buf.append(sep);
                buf.append(tk.valueString);
                sep = ",";
            }
            return buf.toString();
        }

        private String asParameterList() {
            StringBuilder buf = new StringBuilder();
            String sep = "";
            int count = 0;
            for (TypeKind arg : typeKindList) {
                buf.append(sep);
                buf.append(arg.typeString);
                if (count == (typeKindList.size() - 1)) {
                    buf.append("...");
                }
                buf.append(" ");
                buf.append("arg" + count++);
                sep = ",";
            }
            return buf.toString();
        }

        private String asBytecodeString() {
            StringBuilder buf = new StringBuilder();
            int count = 0;
            for (TypeKind arg : typeKindList) {
                if (count == (typeKindList.size() - 1)) {
                    buf.append("[");
                }
                buf.append(arg.bytecodeString);
                count++;
            }
            return buf.toString();
        }

        @Override
        public String expand(String optParameter) {
            return expressionListStr;
        }
    }

    static class VarargsMethod {
        TypeConfiguration parameterTypes;

        public VarargsMethod(TypeConfiguration parameterTypes) {
            this.parameterTypes = parameterTypes;
        }

        @Override
        public String toString() {
            return "void m( " + parameterTypes.parameterListStr + ") {}";
        }

        boolean isApplicable(TypeConfiguration that) {
            List<TypeKind> actuals = that.typeKindList;
            List<TypeKind> formals = parameterTypes.typeKindList;
            if ((actuals.size() - formals.size()) < -1)
                return false; //not enough args
            for (TypeKind actual : actuals) {
                if (!actual.isSubtypeOf(formals.head))
                    return false; //type mismatch
                formals = formals.tail.isEmpty() ?
                    formals :
                    formals.tail;
            }
            return true;
        }

        boolean isMoreSpecificThan(VarargsMethod that) {
            List<TypeKind> actuals = parameterTypes.typeKindList;
            List<TypeKind> formals = that.parameterTypes.typeKindList;
            int checks = 0;
            int expectedCheck = Math.max(actuals.size(), formals.size());
            while (checks < expectedCheck) {
                if (!actuals.head.isSubtypeOf(formals.head))
                    return false; //type mismatch
                formals = formals.tail.isEmpty() ?
                    formals :
                    formals.tail;
                actuals = actuals.tail.isEmpty() ?
                    actuals :
                    actuals.tail;
                checks++;
            }
            return true;
        }
    }

    public static void main(String[] args) {
        new ComboTestHelper<T7042566>()
                .withArrayDimension("SIG", (x, sig, idx) -> x.methodSignatures[idx] = sig, 2, TypeConfiguration.values())
                .withDimension("ACTUALS", (x, actuals) -> x.actuals = actuals, TypeConfiguration.values())
                .run(T7042566::new, T7042566::setup);
    }

    VarargsMethod m1;
    VarargsMethod m2;
    TypeConfiguration[] methodSignatures = new TypeConfiguration[2];
    TypeConfiguration actuals;

    void setup() {
        this.m1 = new VarargsMethod(methodSignatures[0]);
        this.m2 = new VarargsMethod(methodSignatures[1]);
    }

    final String source_template = "class Test {\n" +
                "   #{METH.1}\n" +
                "   #{METH.2}\n" +
                "   void test() { m(#{ACTUALS}); }\n" +
                "}";

    @Override
    public void doWork() throws IOException {
        newCompilationTask()
                .withSourceFromTemplate(source_template, this::getMethodDecl)
                .generate(this::check);
    }

    ComboParameter getMethodDecl(String parameterName) {
        switch (parameterName) {
            case "METH": return optParameter -> {
                return optParameter.equals("1") ?
                        m1.toString() : m2.toString();
            };
            default:
                return null;
        }
    }

    void check(Result<Iterable<? extends JavaFileObject>> res) {
        boolean resolutionError = false;
        VarargsMethod selectedMethod = null;

        boolean m1_applicable = m1.isApplicable(actuals);
        boolean m2_applicable = m2.isApplicable(actuals);

        if (!m1_applicable && !m2_applicable) {
            resolutionError = true;
        } else if (m1_applicable && m2_applicable) {
            //most specific
            boolean m1_moreSpecific = m1.isMoreSpecificThan(m2);
            boolean m2_moreSpecific = m2.isMoreSpecificThan(m1);

            resolutionError = m1_moreSpecific == m2_moreSpecific;
            selectedMethod = m1_moreSpecific ? m1 : m2;
        } else {
            selectedMethod = m1_applicable ?
                m1 : m2;
        }

        if (res.hasErrors() != resolutionError) {
            fail("invalid diagnostics for source:\n" +
                    res.compilationInfo() +
                    "\nExpected resolution error: " + resolutionError +
                    "\nFound error: " + res.hasErrors());
        } else if (!resolutionError) {
            verifyBytecode(res, selectedMethod);
        }
    }

    void verifyBytecode(Result<Iterable<? extends JavaFileObject>> res, VarargsMethod selected) {
        try (InputStream is = res.get().iterator().next().openInputStream()) {
            ClassFile cf = ClassFile.read(is);
            Method testMethod = null;
            for (Method m : cf.methods) {
                if (m.getName(cf.constant_pool).equals("test")) {
                    testMethod = m;
                    break;
                }
            }
            if (testMethod == null) {
                fail("Test method not found");
                return;
            }
            Code_attribute ea =
                (Code_attribute)testMethod.attributes.get(Attribute.Code);
            if (testMethod == null) {
                fail("Code attribute for test() method not found");
                return;
            }

            for (Instruction i : ea.getInstructions()) {
                if (i.getMnemonic().equals("invokevirtual")) {
                    int cp_entry = i.getUnsignedShort(1);
                    CONSTANT_Methodref_info methRef =
                        (CONSTANT_Methodref_info)cf.constant_pool.get(cp_entry);
                    String type = methRef.getNameAndTypeInfo().getType();
                    String sig = selected.parameterTypes.bytecodeSigStr;
                    if (!type.contains(sig)) {
                        fail("Unexpected type method call: " +
                                        type + "" +
                                        "\nfound: " + sig +
                                        "\n" + res.compilationInfo());
                        return;
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("error reading classfile; " + res.compilationInfo() +": " + e);
        }
    }
}
