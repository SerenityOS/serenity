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
 * @bug 8027281
 * @summary As per JVMS 4.9.2, invokespecial can only refer to direct superinterfaces
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile TestDirectSuperInterfaceInvoke.java
 * @run main TestDirectSuperInterfaceInvoke
 */

import java.io.File;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPool.CPRefInfo;
import com.sun.tools.classfile.Instruction;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.Opcode;

interface BaseInterface {
    public default int testedMethod(){ return 1; }
}

interface IntermediateInterface extends BaseInterface {
}

interface TestInterface extends IntermediateInterface {
    public default void test() {
        IntermediateInterface.super.testedMethod();
    }
}

abstract class BaseClass implements BaseInterface { }

class TestClass extends BaseClass implements BaseInterface {
    public int testedMethod() {return 9;}
    public void test() {
        if (super.testedMethod() != 1)
            throw new IllegalStateException();
        if (TestClass.super.testedMethod() != 1)
            throw new IllegalStateException();
        new Runnable() {
            public void run() {
                if (TestClass.super.testedMethod() != 1)
                    throw new IllegalStateException();
            }
        }.run();
    }
}

public class TestDirectSuperInterfaceInvoke {
    public static void main(String... args) throws Exception {
        new TestDirectSuperInterfaceInvoke().run();
    }

    public void run() throws Exception {
        new TestClass().test();
        verifyDefaultBody("TestClass.class");
        new TestInterface() {}.test();
        verifyDefaultBody("TestInterface.class");
    }

    void verifyDefaultBody(String classFile) {
        String workDir = System.getProperty("test.classes");
        File file = new File(workDir, classFile);
        try {
            final ClassFile cf = ClassFile.read(file);
            for (Method m : cf.methods) {
                Code_attribute codeAttr = (Code_attribute)m.attributes.get(Attribute.Code);
                for (Instruction instr : codeAttr.getInstructions()) {
                    if (instr.getOpcode() == Opcode.INVOKESPECIAL) {
                        int pc_index = instr.getShort(1);
                        CPRefInfo ref = (CPRefInfo)cf.constant_pool.get(pc_index);
                        String className = ref.getClassName();
                        if (className.equals("BaseInterface"))
                            throw new IllegalStateException("Must not directly refer to TestedInterface");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("error reading " + file +": " + e);
        }
    }

}
