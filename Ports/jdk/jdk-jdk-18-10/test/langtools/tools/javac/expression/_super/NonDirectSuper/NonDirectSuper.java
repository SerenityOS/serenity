/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8027789
 * @summary check that the direct superclass is used as the site when calling
 *          a superclass' method
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile Base.java NonDirectSuper.java
 * @run main test.NonDirectSuper
 */

package test;

import java.io.File;

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPool.CPRefInfo;
import com.sun.tools.classfile.Instruction;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.Opcode;

public class NonDirectSuper {
    public static void main(String... args) {
        new NonDirectSuper().run();
    }

    void run() {
        String workDir = System.getProperty("test.classes");
        File testPackage = new File(workDir, "test");

        for (File clazz : testPackage.listFiles()) {
            if ("NonDirectSuper.class".equals(clazz.getName())) continue;
            verifyInvokeSpecialRefToObject(clazz);
        }
    }

    void verifyInvokeSpecialRefToObject(File clazz) {
        try {
            final ClassFile cf = ClassFile.read(clazz);
            for (Method m : cf.methods) {
                Code_attribute codeAttr = (Code_attribute)m.attributes.get(Attribute.Code);
                for (Instruction instr : codeAttr.getInstructions()) {
                    if (instr.getOpcode() == Opcode.INVOKESPECIAL ||
                        instr.getOpcode() == Opcode.INVOKEVIRTUAL) {
                        int pc_index = instr.getShort(1);
                        CPRefInfo ref = (CPRefInfo)cf.constant_pool.get(pc_index);
                        String className = ref.getClassName();
                        String methodName = ref.getNameAndTypeInfo().getName();
                        if (methodName.equals("toString")) {
                            if (!className.equals("java/lang/Object"))
                                throw new IllegalStateException("Must directly refer to j.l.Object");
                        } else if (methodName.startsWith("refTo")) {
                            String expectedClass = methodName.substring("refTo".length());
                            if (!className.replace("/", "").equals(expectedClass)) {
                                throw new IllegalStateException("Unexpected reference to: " +
                                        className + ", should be " + expectedClass);
                            }
                        }
                    }
                    if (instr.getOpcode() == Opcode.GETFIELD ||
                        instr.getOpcode() == Opcode.PUTFIELD) {
                        int pc_index = instr.getShort(1);
                        CPRefInfo ref = (CPRefInfo)cf.constant_pool.get(pc_index);
                        String className = ref.getClassName();
                        String fieldName = ref.getNameAndTypeInfo().getName();
                        if (fieldName.startsWith("refTo")) {
                            String expectedClass = fieldName.substring("refTo".length());
                            if (!className.replace("/", "").equals(expectedClass)) {
                                throw new IllegalStateException("Unexpected reference to: " +
                                        className + ", should be " + expectedClass);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("error reading " + clazz +": " + e);
        }
    }
}

class OtherPackageTest extends base.Base {
    void run() {
        new Runnable() {
            public void run() {
                OtherPackageTest.super.refTobaseBase();
                OtherPackageTest.super.refTobaseBase = OtherPackageTest.super.refTobaseBase + 1;
                OtherPackageTest.super.toString();
                refTotestOtherPackageTest();
                refTotestOtherPackageTest = refTotestOtherPackageTest + 1;
                OtherPackageTest.this.refTotestOtherPackageTest();
                OtherPackageTest.this.refTotestOtherPackageTest =
                        OtherPackageTest.this.refTotestOtherPackageTest + 1;
            }
        }.run();
        super.refTobaseBase();
        super.refTobaseBase = super.refTobaseBase + 1;
        super.toString();
        OtherPackageTest.super.refTobaseBase();
        OtherPackageTest.super.refTobaseBase = OtherPackageTest.super.refTobaseBase + 1;
        OtherPackageTest.super.toString();
        refTotestOtherPackageTest();
        refTotestOtherPackageTest = refTotestOtherPackageTest + 1;
    }

    static class InnerBase {
        private void refTotestOtherPackageTest$InnerBase() { }
    }
    static class InnerTest extends InnerBase {
        void run() {
            new Runnable() {
                public void run() {
                    InnerTest.super.refTotestOtherPackageTest$InnerBase();
                }
            }.run();
            super.refTotestOtherPackageTest$InnerBase();
            InnerTest.super.refTotestOtherPackageTest$InnerBase();
        }
    }
}

class CurPackagePrivateBase {
    void refTotestCurPackagePrivateExt() { }
    void refTotestCurPackagePrivateTest() { }
    int refTotestCurPackagePrivateExt;
    int refTotestCurPackagePrivateTest;
}

class CurPackagePrivateExt extends CurPackagePrivateBase {
}

class CurPackagePrivateTest extends CurPackagePrivateExt {
    void run() {
        new Runnable() {
            public void run() {
                CurPackagePrivateTest.super.refTotestCurPackagePrivateExt();
                CurPackagePrivateTest.super.refTotestCurPackagePrivateExt =
                        CurPackagePrivateTest.super.refTotestCurPackagePrivateExt + 1;
                CurPackagePrivateTest.this.refTotestCurPackagePrivateTest();
                CurPackagePrivateTest.this.refTotestCurPackagePrivateTest =
                        CurPackagePrivateTest.this.refTotestCurPackagePrivateTest + 1;
                refTotestCurPackagePrivateTest();
                refTotestCurPackagePrivateTest = refTotestCurPackagePrivateTest + 1;
            }
        }.run();
        super.refTotestCurPackagePrivateExt();
        super.refTotestCurPackagePrivateExt = super.refTotestCurPackagePrivateExt + 1;
        CurPackagePrivateTest.super.refTotestCurPackagePrivateExt();
        CurPackagePrivateTest.super.refTotestCurPackagePrivateExt =
                CurPackagePrivateTest.super.refTotestCurPackagePrivateExt + 1;
        refTotestCurPackagePrivateTest();
        refTotestCurPackagePrivateTest = refTotestCurPackagePrivateTest + 1;
    }
}
