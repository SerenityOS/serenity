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
 * @bug 8009170
 * @summary Regression: javac generates redundant bytecode in assignop involving
 * arrays
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main RedundantByteCodeInArrayTest
 */

import java.io.File;
import java.io.IOException;

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.Code_attribute.InvalidIndex;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Descriptor.InvalidDescriptor;
import com.sun.tools.classfile.Method;

public class RedundantByteCodeInArrayTest {
    public static void main(String[] args)
            throws IOException, ConstantPoolException, InvalidDescriptor, InvalidIndex {
        new RedundantByteCodeInArrayTest()
                .checkClassFile(new File(System.getProperty("test.classes", "."),
                    RedundantByteCodeInArrayTest.class.getName() + ".class"));
    }

    void arrMethod(int[] array, int p, int inc) {
        array[p] += inc;
    }

    void checkClassFile(File file)
            throws IOException, ConstantPoolException, InvalidDescriptor, InvalidIndex {
        ClassFile classFile = ClassFile.read(file);
        ConstantPool constantPool = classFile.constant_pool;

        //lets get all the methods in the class file.
        for (Method method : classFile.methods) {
            if (method.getName(constantPool).equals("arrMethod")) {
                Code_attribute code = (Code_attribute) method.attributes
                        .get(Attribute.Code);
                if (code.max_locals > 4)
                    throw new AssertionError("Too many locals for method arrMethod");
            }
        }
    }
}
