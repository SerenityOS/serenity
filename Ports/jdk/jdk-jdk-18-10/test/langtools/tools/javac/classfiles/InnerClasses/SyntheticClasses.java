/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

/** @test
 *  @bug 8034854
 *  @summary Verify that the InnerClasses attribute has outer_class_info_index zero if it has
 *           inner_name_index zero (for synthetic classes)
 *  @modules jdk.jdeps/com.sun.tools.classfile
 *  @compile SyntheticClasses.java
 *  @run main SyntheticClasses
 */

import java.io.*;
import java.util.*;
import com.sun.tools.classfile.*;

public class SyntheticClasses {

    public static void main(String[] args) throws IOException, ConstantPoolException {
        new SyntheticClasses().run();
    }

    private void run() throws IOException, ConstantPoolException {
        File testClasses = new File(System.getProperty("test.classes"));
        for (File classFile : testClasses.listFiles(f -> f.getName().endsWith(".class"))) {
            ClassFile cf = ClassFile.read(classFile);
            if (cf.getName().matches(".*\\$[0-9]+")) {
                EnclosingMethod_attribute encl =
                        (EnclosingMethod_attribute) cf.getAttribute(Attribute.EnclosingMethod);
                if (encl != null) {
                    if (encl.method_index != 0)
                        throw new IllegalStateException("Invalid EnclosingMethod.method_index: " +
                                                        encl.method_index + ".");
                }
            }
            InnerClasses_attribute attr =
                    (InnerClasses_attribute) cf.getAttribute(Attribute.InnerClasses);
            if (attr != null) {
                for (InnerClasses_attribute.Info info : attr.classes) {
                    if (cf.major_version < 51)
                        throw new IllegalStateException();
                    if (info.inner_name_index == 0 && info.outer_class_info_index != 0)
                        throw new IllegalStateException("Invalid outer_class_info_index=" +
                                                        info.outer_class_info_index +
                                                        "; inner_name_index=" +
                                                        info.inner_name_index + ".");
                }
            }
        }
    }
}

class SyntheticConstructorAccessTag {

    private static class A {
        private A(){}
    }

    public void test() {
        new A();
    }
}

class SyntheticEnumMapping {
    private int convert(E e) {
        switch (e) {
            case A: return 0;
            default: return -1;
        }
    }
    enum E { A }
}

interface SyntheticAssertionsDisabled {
    public default void test() {
        assert false;
    }
}
