/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.tests.separate;

import java.io.*;
import java.util.*;

public class ClassToInterfaceConverter implements ClassFilePreprocessor {

    private String whichClass;

    public ClassToInterfaceConverter(String className) {
        this.whichClass = className;
    }

    private boolean utf8Matches(ClassFile.CpEntry entry, String v) {
        if (!(entry instanceof ClassFile.CpUtf8)) {
            return false;
        }
        ClassFile.CpUtf8 utf8 = (ClassFile.CpUtf8)entry;
        if (v.length() != utf8.bytes.length) {
            return false;
        }
        for (int i = 0; i < v.length(); ++i) {
            if (v.charAt(i) != utf8.bytes[i]) {
                return false;
            }
        }
        return true;
    }

    private void convertToInterface(ClassFile cf) {
        cf.access_flags = 0x0601; // ACC_INTERFACE | ACC_ABSTRACT | ACC_PUBLIC
        ArrayList<ClassFile.Method> new_methods = new ArrayList<>();
        // Find <init> method and delete it
        for (int i = 0; i < cf.methods.size(); ++i) {
            ClassFile.Method method = cf.methods.get(i);
            ClassFile.CpEntry name = cf.constant_pool.get(method.name_index);
            if (!utf8Matches(name, "<init>")) {
                new_methods.add(method);
            }
        }
        cf.methods = new_methods;
    }

    public byte[] preprocess(String classname, byte[] bytes) {
        ClassFile cf = new ClassFile(bytes);

        ClassFile.CpEntry entry = cf.constant_pool.get(cf.this_class);
        ClassFile.CpEntry name = cf.constant_pool.get(
            ((ClassFile.CpClass)entry).name_index);
        if (utf8Matches(name, whichClass)) {
            convertToInterface(cf);
            return cf.toByteArray();
        } else {
            return bytes; // unmodified
        }
    }

/*
    public static void main(String argv[]) throws Exception {
        File input = new File(argv[0]);
        byte[] buffer = new byte[(int)input.length()];
        new FileInputStream(input).read(buffer);

        ClassFilePreprocessor cfp = new ClassToInterfaceConverter("Hello");
        byte[] cf = cfp.preprocess(argv[0], buffer);
        new FileOutputStream(argv[0] + ".mod").write(cf);
    }
*/
}
