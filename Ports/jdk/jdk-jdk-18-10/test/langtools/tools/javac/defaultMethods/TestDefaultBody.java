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
 * @bug 7192246
 * @summary  check that code attributed for default methods is correctly generated
 * @modules jdk.jdeps/com.sun.tools.classfile
 */

import com.sun.tools.classfile.AccessFlags;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool.*;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.Instruction;
import com.sun.tools.classfile.Method;

import com.sun.tools.classfile.Opcode;
import java.io.*;

public class TestDefaultBody {

    interface TestInterface {
        int no_default(int i);
        default int yes_default(int i) { return impl(this, i); }
    }

    static int impl(TestInterface ti, int i) { return 0; }

    static final String TARGET_CLASS_NAME = "TestDefaultBody";
    static final String TARGET_NAME = "impl";
    static final String TARGET_TYPE = "(LTestDefaultBody$TestInterface;I)I";
    static final String SUBTEST_NAME = TestInterface.class.getName() + ".class";
    static final String TEST_METHOD_NAME = "yes_default";

    public static void main(String... args) throws Exception {
        new TestDefaultBody().run();
    }

    public void run() throws Exception {
        String workDir = System.getProperty("test.classes");
        File compiledTest = new File(workDir, SUBTEST_NAME);
        verifyDefaultBody(compiledTest);
    }

    void verifyDefaultBody(File f) {
        System.err.println("verify: " + f);
        try {
            ClassFile cf = ClassFile.read(f);
            Method testMethod = null;
            Code_attribute codeAttr = null;
            for (Method m : cf.methods) {
                codeAttr = (Code_attribute)m.attributes.get(Attribute.Code);
                String mname = m.getName(cf.constant_pool);
                if (mname.equals(TEST_METHOD_NAME)) {
                    testMethod = m;
                    break;
                } else {
                    codeAttr = null;
                }
            }
            if (testMethod == null) {
                throw new Error("Test method not found");
            }
            if (testMethod.access_flags.is(AccessFlags.ACC_ABSTRACT)) {
                throw new Error("Test method is abstract");
            }
            if (codeAttr == null) {
                throw new Error("Code attribute in test method not found");
            }

            boolean found = false;
            for (Instruction instr : codeAttr.getInstructions()) {
                if (instr.getOpcode() == Opcode.INVOKESTATIC) {
                    found = true;
                    int pc_index = instr.getShort(1);
                    CONSTANT_Methodref_info mref = (CONSTANT_Methodref_info)cf.constant_pool.get(pc_index);
                    String className = mref.getClassName();
                    String targetName = mref.getNameAndTypeInfo().getName();
                    String targetType = mref.getNameAndTypeInfo().getType();

                    if (!className.equals(TARGET_CLASS_NAME)) {
                        throw new Error("unexpected class in default method body " + className);
                    }
                    if (!targetName.equals(TARGET_NAME)) {
                        throw new Error("unexpected method name in default method body " + targetName);
                    }
                    if (!targetType.equals(TARGET_TYPE)) {
                        throw new Error("unexpected method type in default method body " + targetType);
                    }
                    break;
                }
            }

            if (!found) {
                throw new Error("no invokestatic found in default method body");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("error reading " + f +": " + e);
        }
    }
}
