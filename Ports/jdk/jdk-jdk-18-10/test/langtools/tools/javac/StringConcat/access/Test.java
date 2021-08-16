/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.tools.classfile.ConstantPool.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/*
 * @test
 * @bug     8151223
 * @summary String concatenation fails with implicit toString() on package-private class
 * @modules jdk.jdeps/com.sun.tools.classfile
 *
 * @clean *
 * @compile -XDstringConcat=indy              Holder.java PublicClass.java PublicInterface.java Public_PublicClass.java Public_PublicInterface.java Public_PrivateInterface1.java Public_PrivateInterface2.java Test.java
 * @run main Test
 *
 * @clean *
 * @compile -XDstringConcat=indyWithConstants Holder.java PublicClass.java PublicInterface.java Public_PublicClass.java Public_PublicInterface.java Public_PrivateInterface1.java Public_PrivateInterface2.java Test.java
 * @run main Test
 */

public class Test {
    static List<String> actualTypes;

    public static void main(String[] argv) throws Exception {
        readIndyTypes();

        p1.Holder holder = new p1.Holder();

        int idx = 0;

        // ----------------------------------------------------------------------------

        // public Private_PublicClass            c1 = new Private_PublicClass();
        test("" + holder.c1, idx++, "(Lp1/PublicClass;)Ljava/lang/String;");

        // public Private_PublicInterface        c2 = new Private_PublicInterface();
        test("" + holder.c2, idx++, "(Ljava/lang/Object;)Ljava/lang/String;");

        // public Private_PrivateInterface1      c3 = new Private_PrivateInterface1();
        test("" + holder.c3, idx++, "(Ljava/lang/Object;)Ljava/lang/String;");

        // public Private_PrivateInterface2      c4 = new Private_PrivateInterface2();
        test("" + holder.c4, idx++, "(Ljava/lang/Object;)Ljava/lang/String;");

        // public Public_PublicClass             c5 = new Public_PublicClass();
        test("" + holder.c5, idx++, "(Lp1/Public_PublicClass;)Ljava/lang/String;");

        // public Public_PublicInterface         c6 = new Public_PublicInterface();
        test("" + holder.c6, idx++, "(Lp1/Public_PublicInterface;)Ljava/lang/String;");

        // public Public_PrivateInterface1       c7 = new Public_PrivateInterface1();
        test("" + holder.c7, idx++, "(Lp1/Public_PrivateInterface1;)Ljava/lang/String;");

        // public Public_PrivateInterface2       c8 = new Public_PrivateInterface2();
        test("" + holder.c8, idx++, "(Lp1/Public_PrivateInterface2;)Ljava/lang/String;");

        // ----------------------------------------------------------------------------

        // public Private_PublicClass[]          ac1 = new Private_PublicClass[0];
        test("" + holder.ac1, idx++, "([Lp1/PublicClass;)Ljava/lang/String;");

        // public Private_PublicInterface[]      ac2 = new Private_PublicInterface[0];
        test("" + holder.ac2, idx++, "([Ljava/lang/Object;)Ljava/lang/String;");

        // public Private_PrivateInterface1[]    ac3 = new Private_PrivateInterface1[0];
        test("" + holder.ac3, idx++, "([Ljava/lang/Object;)Ljava/lang/String;");

        // public Private_PrivateInterface2[]    ac4 = new Private_PrivateInterface2[0];
        test("" + holder.ac4, idx++, "([Ljava/lang/Object;)Ljava/lang/String;");

        // public Public_PublicClass[]           ac5 = new Public_PublicClass[0];
        test("" + holder.ac5, idx++, "([Lp1/Public_PublicClass;)Ljava/lang/String;");

        // public Public_PublicInterface[]       ac6 = new Public_PublicInterface[0];
        test("" + holder.ac6, idx++, "([Lp1/Public_PublicInterface;)Ljava/lang/String;");

        // public Public_PrivateInterface1[]     ac7 = new Public_PrivateInterface1[0];
        test("" + holder.ac7, idx++, "([Lp1/Public_PrivateInterface1;)Ljava/lang/String;");

        // public Public_PrivateInterface2[]     ac8 = new Public_PrivateInterface2[0];
        test("" + holder.ac8, idx++, "([Lp1/Public_PrivateInterface2;)Ljava/lang/String;");

        // ----------------------------------------------------------------------------

        // public Private_PublicClass[][]       aac1 = new Private_PublicClass[0][];
        test("" + holder.aac1, idx++, "([[Lp1/PublicClass;)Ljava/lang/String;");

        // public Private_PublicInterface[][]   aac2 = new Private_PublicInterface[0][];
        test("" + holder.aac2, idx++, "([[Ljava/lang/Object;)Ljava/lang/String;");

        // public Private_PrivateInterface1[][] aac3 = new Private_PrivateInterface1[0][];
        test("" + holder.aac3, idx++, "([[Ljava/lang/Object;)Ljava/lang/String;");

        // public Private_PrivateInterface2[][] aac4 = new Private_PrivateInterface2[0][];
        test("" + holder.aac4, idx++, "([[Ljava/lang/Object;)Ljava/lang/String;");

        // public Public_PublicClass[][]        aac5 = new Public_PublicClass[0][];
        test("" + holder.aac5, idx++, "([[Lp1/Public_PublicClass;)Ljava/lang/String;");

        // public Public_PublicInterface[][]    aac6 = new Public_PublicInterface[0][];
        test("" + holder.aac6, idx++, "([[Lp1/Public_PublicInterface;)Ljava/lang/String;");

        // public Public_PrivateInterface1[][]  aac7 = new Public_PrivateInterface1[0][];
        test("" + holder.aac7, idx++, "([[Lp1/Public_PrivateInterface1;)Ljava/lang/String;");

        // public Public_PrivateInterface2[][]  aac8 = new Public_PrivateInterface2[0][];
        test("" + holder.aac8, idx++, "([[Lp1/Public_PrivateInterface2;)Ljava/lang/String;");

        // ----------------------------------------------------------------------------

        // public PublicInterface                i1 = new Private_PublicInterface();
        test("" + holder.i1, idx++, "(Lp1/PublicInterface;)Ljava/lang/String;");

        // public PrivateInterface1              i2 = new Private_PrivateInterface1();
        test("" + holder.i2, idx++, "(Ljava/lang/Object;)Ljava/lang/String;");

        // public PrivateInterface2              i3 = new Private_PrivateInterface2();
        test("" + holder.i3, idx++, "(Ljava/lang/Object;)Ljava/lang/String;");

        // public PublicInterface[]              ai1 = new Private_PublicInterface[0];
        test("" + holder.ai1, idx++, "([Lp1/PublicInterface;)Ljava/lang/String;");

        // public PrivateInterface1[]            ai2 = new Private_PrivateInterface1[0];
        test("" + holder.ai2, idx++, "([Ljava/lang/Object;)Ljava/lang/String;");

        // public PrivateInterface2[]            ai3 = new Private_PrivateInterface2[0];
        test("" + holder.ai3, idx++, "([Ljava/lang/Object;)Ljava/lang/String;");

        // public PublicInterface[][]           aai1 = new Private_PublicInterface[0][];
        test("" + holder.aai1, idx++, "([[Lp1/PublicInterface;)Ljava/lang/String;");

        // public PrivateInterface1[][]         aai2 = new Private_PrivateInterface1[0][];
        test("" + holder.aai2, idx++, "([[Ljava/lang/Object;)Ljava/lang/String;");

        // public PrivateInterface2[][]         aai3 = new Private_PrivateInterface2[0][];
        test("" + holder.aai3, idx++, "([[Ljava/lang/Object;)Ljava/lang/String;");

    }

    public static void test(String actual, int index, String expectedType) {
        if (!"passed".equals(actual) && !actual.startsWith("[")) {
            throw new IllegalStateException("Unexpected result: " + actual);
        }
        String actualType = actualTypes.get(index);
        if (!actualType.equals(expectedType)) {
            throw new IllegalStateException("Unexpected type: expected = " + expectedType + ", actual = " + actualType);
        }
    }

    public static void readIndyTypes() throws Exception {
        actualTypes = new ArrayList<String>();

        ClassFile classFile = ClassFile.read(new File(System.getProperty("test.classes", "."),
                    Test.class.getName() + ".class"));
        ConstantPool constantPool = classFile.constant_pool;

        for (Method method : classFile.methods) {
            if (method.getName(constantPool).equals("main")) {
                Code_attribute code = (Code_attribute) method.attributes
                        .get(Attribute.Code);
                for (Instruction i : code.getInstructions()) {
                    if (i.getOpcode() == Opcode.INVOKEDYNAMIC) {
                        CONSTANT_InvokeDynamic_info indyInfo = (CONSTANT_InvokeDynamic_info) constantPool.get(i.getUnsignedShort(1));
                        CONSTANT_NameAndType_info natInfo = indyInfo.getNameAndTypeInfo();
                        actualTypes.add(natInfo.getType());
                    }
                }
            }
        }
    }
}
