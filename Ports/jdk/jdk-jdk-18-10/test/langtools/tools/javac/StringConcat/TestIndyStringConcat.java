/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.classfile.*;
import com.sun.tools.classfile.BootstrapMethods_attribute.BootstrapMethodSpecifier;
import com.sun.tools.classfile.ConstantPool.CONSTANT_InvokeDynamic_info;
import com.sun.tools.classfile.ConstantPool.CONSTANT_MethodHandle_info;

import java.io.File;

/*
 * @test
 * @bug     8148483 8151516 8151223
 * @summary Test that StringConcat is working for JDK >= 9
 * @modules jdk.jdeps/com.sun.tools.classfile
 *
 * @clean TestIndyStringConcat*
 * @compile -source 7 -target 7 TestIndyStringConcat.java
 * @run main TestIndyStringConcat false
 *
 * @clean TestIndyStringConcat*
 * @compile -source 8 -target 8 TestIndyStringConcat.java
 * @run main TestIndyStringConcat false
 *
 * @clean TestIndyStringConcat*
 * @compile -XDstringConcat=inline -source 9 -target 9 TestIndyStringConcat.java
 * @run main TestIndyStringConcat false
 *
 * @clean TestIndyStringConcat*
 * @compile -XDstringConcat=indy -source 9 -target 9 TestIndyStringConcat.java
 * @run main TestIndyStringConcat true
 *
 * @clean TestIndyStringConcat*
 * @compile -XDstringConcat=indyWithConstants -source 9 -target 9 TestIndyStringConcat.java
 * @run main TestIndyStringConcat true
 */
public class TestIndyStringConcat {

    static String other;

    public static String test() {
        return "Foo" + other;
    }

    public static void main(String[] args) throws Exception {
        boolean expected = Boolean.valueOf(args[0]);
        boolean actual = hasStringConcatFactoryCall("test");
        if (expected != actual) {
            throw new AssertionError("expected = " + expected + ", actual = " + actual);
        }
    }

    public static boolean hasStringConcatFactoryCall(String methodName) throws Exception {
        ClassFile classFile = ClassFile.read(new File(System.getProperty("test.classes", "."),
                TestIndyStringConcat.class.getName() + ".class"));
        ConstantPool constantPool = classFile.constant_pool;

        BootstrapMethods_attribute bsm_attr =
                (BootstrapMethods_attribute)classFile
                        .getAttribute(Attribute.BootstrapMethods);

        for (Method method : classFile.methods) {
            if (method.getName(constantPool).equals(methodName)) {
                Code_attribute code = (Code_attribute) method.attributes
                        .get(Attribute.Code);
                for (Instruction i : code.getInstructions()) {
                    if (i.getOpcode() == Opcode.INVOKEDYNAMIC) {
                        CONSTANT_InvokeDynamic_info indyInfo =
                                (CONSTANT_InvokeDynamic_info) constantPool.get(i.getUnsignedShort(1));

                        BootstrapMethodSpecifier bsmSpec =
                                bsm_attr.bootstrap_method_specifiers[indyInfo.bootstrap_method_attr_index];

                        CONSTANT_MethodHandle_info bsmInfo =
                                (CONSTANT_MethodHandle_info) constantPool.get(bsmSpec.bootstrap_method_ref);

                        if (bsmInfo.getCPRefInfo().getClassName().equals("java/lang/invoke/StringConcatFactory")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

}
