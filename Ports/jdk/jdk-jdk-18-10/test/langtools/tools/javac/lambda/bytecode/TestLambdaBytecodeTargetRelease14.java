/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8238358
 * @summary Lambda back-end should generate invokespecial for method handles referring to
 *          private instance methods when compiling with --release 14
 * @library /tools/javac/lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @run main TestLambdaBytecodeTargetRelease14
 */

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.BootstrapMethods_attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPool.*;
import com.sun.tools.classfile.Instruction;
import com.sun.tools.classfile.Method;

import java.io.IOException;
import java.io.InputStream;

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;

import javax.tools.JavaFileObject;

public class TestLambdaBytecodeTargetRelease14 extends ComboInstance<TestLambdaBytecodeTargetRelease14> {

    static final int MF_ARITY = 3;
    static final String MH_SIG = "()V";

    enum ClassKind implements ComboParameter {
        CLASS("class"),
        INTERFACE("interface");

        String classStr;

        ClassKind(String classStr) {
            this.classStr = classStr;
        }

        @Override
        public String expand(String optParameter) {
            return classStr;
        }
    }

    enum AccessKind implements ComboParameter {
        PUBLIC("public"),
        PRIVATE("private");

        String accessStr;

        AccessKind(String accessStr) {
            this.accessStr = accessStr;
        }

        @Override
        public String expand(String optParameter) {
            return accessStr;
        }
    }

    enum StaticKind implements ComboParameter {
        STATIC("static"),
        INSTANCE("");

        String staticStr;

        StaticKind(String staticStr) {
            this.staticStr = staticStr;
        }

        @Override
        public String expand(String optParameter) {
            return staticStr;
        }
    }

    enum DefaultKind implements ComboParameter {
        DEFAULT("default"),
        NO_DEFAULT("");

        String defaultStr;

        DefaultKind(String defaultStr) {
            this.defaultStr = defaultStr;
        }

        @Override
        public String expand(String optParameter) {
            return defaultStr;
        }
    }

    static class MethodKind {
        ClassKind ck;
        AccessKind ak;
        StaticKind sk;
        DefaultKind dk;

        MethodKind(ClassKind ck, AccessKind ak, StaticKind sk, DefaultKind dk) {
            this.ck = ck;
            this.ak = ak;
            this.sk = sk;
            this.dk = dk;
        }

        boolean inInterface() {
            return ck == ClassKind.INTERFACE;
        }

        boolean isPrivate() {
            return ak == AccessKind.PRIVATE;
        }

        boolean isStatic() {
            return sk == StaticKind.STATIC;
        }

        boolean isDefault() {
            return dk == DefaultKind.DEFAULT;
        }

        boolean isOK() {
            if (isDefault() && (!inInterface() || isStatic())) {
                return false;
            } else if (inInterface() &&
                    ((!isStatic() && !isDefault()) || isPrivate())) {
                return false;
            } else {
                return true;
            }
        }
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<TestLambdaBytecodeTargetRelease14>()
                .withDimension("CLASSKIND", (x, ck) -> x.ck = ck, ClassKind.values())
                .withArrayDimension("ACCESS", (x, acc, idx) -> x.accessKinds[idx] = acc, 2, AccessKind.values())
                .withArrayDimension("STATIC", (x, sk, idx) -> x.staticKinds[idx] = sk, 2, StaticKind.values())
                .withArrayDimension("DEFAULT", (x, dk, idx) -> x.defaultKinds[idx] = dk, 2, DefaultKind.values())
                .run(TestLambdaBytecodeTargetRelease14::new, TestLambdaBytecodeTargetRelease14::init);
    }

    ClassKind ck;
    AccessKind[] accessKinds = new AccessKind[2];
    StaticKind[] staticKinds = new StaticKind[2];
    DefaultKind[] defaultKinds = new DefaultKind[2];
    MethodKind mk1, mk2;

    void init() {
        mk1 = new MethodKind(ck, accessKinds[0], staticKinds[0], defaultKinds[0]);
        mk2 = new MethodKind(ck, accessKinds[1], staticKinds[1], defaultKinds[1]);
    }

    String source_template =
                "#{CLASSKIND} Test {\n" +
                "   #{ACCESS[0]} #{STATIC[0]} #{DEFAULT[0]} void test() { Runnable r = ()->{ target(); }; }\n" +
                "   #{ACCESS[1]} #{STATIC[1]} #{DEFAULT[1]} void target() { }\n" +
                "}\n";

    @Override
    public void doWork() throws IOException {
        newCompilationTask()
                .withSourceFromTemplate(source_template)
                .withOption("--release").withOption("14")
                .generate(this::verifyBytecode);
    }

    void verifyBytecode(Result<Iterable<? extends JavaFileObject>> res) {
        if (res.hasErrors()) {
            boolean errorExpected = !mk1.isOK() || !mk2.isOK();
            errorExpected |= mk1.isStatic() && !mk2.isStatic();

            if (!errorExpected) {
                fail("Diags found when compiling instance; " + res.compilationInfo());
            }
            return;
        }
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

            int bsmIdx = -1;

            for (Instruction i : ea.getInstructions()) {
                if (i.getMnemonic().equals("invokedynamic")) {
                    CONSTANT_InvokeDynamic_info indyInfo =
                         (CONSTANT_InvokeDynamic_info)cf
                            .constant_pool.get(i.getShort(1));
                    bsmIdx = indyInfo.bootstrap_method_attr_index;
                    if (!indyInfo.getNameAndTypeInfo().getType().equals(makeIndyType())) {
                        fail("type mismatch for CONSTANT_InvokeDynamic_info " +
                                res.compilationInfo() + "\n" + indyInfo.getNameAndTypeInfo().getType() +
                                "\n" + makeIndyType());
                        return;
                    }
                }
            }
            if (bsmIdx == -1) {
                fail("Missing invokedynamic in generated code");
                return;
            }

            BootstrapMethods_attribute bsm_attr =
                    (BootstrapMethods_attribute)cf
                    .getAttribute(Attribute.BootstrapMethods);
            if (bsm_attr.bootstrap_method_specifiers.length != 1) {
                fail("Bad number of method specifiers " +
                        "in BootstrapMethods attribute");
                return;
            }
            BootstrapMethods_attribute.BootstrapMethodSpecifier bsm_spec =
                    bsm_attr.bootstrap_method_specifiers[0];

            if (bsm_spec.bootstrap_arguments.length != MF_ARITY) {
                fail("Bad number of static invokedynamic args " +
                        "in BootstrapMethod attribute");
                return;
            }

            CONSTANT_MethodHandle_info mh =
                    (CONSTANT_MethodHandle_info)cf.constant_pool.get(bsm_spec.bootstrap_arguments[1]);

            boolean kindOK;
            switch (mh.reference_kind) {
                case REF_invokeStatic: kindOK = mk2.isStatic(); break;
                case REF_invokeSpecial: kindOK = !mk2.isStatic(); break;
                case REF_invokeInterface: kindOK = mk2.inInterface(); break;
                default:
                    kindOK = false;
            }

            if (!kindOK) {
                fail("Bad invoke kind in implementation method handle");
                return;
            }

            if (!mh.getCPRefInfo().getNameAndTypeInfo().getType().toString().equals(MH_SIG)) {
                fail("Type mismatch in implementation method handle");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("error reading " + res.compilationInfo() + ": " + e);
        }
    }

    String makeIndyType() {
        StringBuilder buf = new StringBuilder();
        buf.append("(");
        if (!mk2.isStatic()) {
            buf.append("LTest;");
        }
        buf.append(")Ljava/lang/Runnable;");
        return buf.toString();
    }
}
