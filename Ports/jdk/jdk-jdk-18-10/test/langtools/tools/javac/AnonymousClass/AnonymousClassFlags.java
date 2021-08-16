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

/*
 * @test
 * @bug 8161013
 * @summary Verify that anonymous class binaries have the correct flags set
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main AnonymousClassFlags
 */

import java.util.*;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sun.tools.classfile.*;
import static com.sun.tools.classfile.AccessFlags.*;

public class AnonymousClassFlags {
    public static void main(String[] args) throws Exception {
        new AnonymousClassFlags().test(System.getProperty("test.classes", "."));
    }

    /** Maps names of anonymous classes to their expected inner_class_access_flags */
    private static Map<String, Integer> anonClasses = new LinkedHashMap<>();

    // ******* TEST CASES ********

    static Object o1 = new Object() {
        { anonClasses.put(getClass().getName(), 0); }
    };

    static void staticMethod() {
        Object o2 = new Object() {
            { anonClasses.put(getClass().getName(), 0); }
        };
    }

    static {
        staticMethod();

        Object o3 = new Object() {
            { anonClasses.put(getClass().getName(), 0); }
        };
    }

    Object o4 = new Object() {
        { anonClasses.put(getClass().getName(), 0); }
    };

    void instanceMethod() {
        Object o5 = new Object() {
            { anonClasses.put(getClass().getName(), 0); }
        };
    }

    {
        instanceMethod();

        Object o6 = new Object() {
            { anonClasses.put(getClass().getName(), 0); }
        };
    }

    // ******* TEST IMPLEMENTATION ********

    void test(String classesDir) throws Exception {
        staticMethod();
        instanceMethod();

        Path outerFile = Paths.get(classesDir, getClass().getName() + ".class");
        ClassFile outerClass = ClassFile.read(outerFile);
        for (Map.Entry<String,Integer> entry : anonClasses.entrySet()) {
            Path innerFile = Paths.get(classesDir, entry.getKey() + ".class");
            ClassFile innerClass = ClassFile.read(innerFile);
            String name = entry.getKey();
            int expected = entry.getValue();
            assertInnerFlags(outerClass, name, expected);
            assertClassFlags(innerClass, name, expected);
            assertInnerFlags(innerClass, name, expected);
        }
    }

    static void assertClassFlags(ClassFile classFile, String name, int expected) {
        int mask = ACC_PUBLIC | ACC_FINAL | ACC_INTERFACE | ACC_ABSTRACT |
                   ACC_SYNTHETIC | ACC_ANNOTATION | ACC_ENUM;
        int classExpected = (expected & mask) | ACC_SUPER;
        int classActual = classFile.access_flags.flags;
        if (classActual != classExpected) {
            throw new AssertionError("Incorrect access_flags for class " + name +
                                     ": expected=" + classExpected + ", actual=" + classActual);
        }

    }

    static void assertInnerFlags(ClassFile classFile, String name, int expected) throws ConstantPoolException {
        int innerActual = lookupInnerFlags(classFile, name).flags;
        if (innerActual != expected) {
            throw new AssertionError("Incorrect inner_class_access_flags for class " + name +
                                     " in class " + classFile.getName() +
                                     ": expected=" + expected + ", actual=" + innerActual);
        }
    }

    private static AccessFlags lookupInnerFlags(ClassFile classFile, String innerName) throws ConstantPoolException {
        InnerClasses_attribute inners = (InnerClasses_attribute) classFile.getAttribute("InnerClasses");
        if (inners == null) {
            throw new AssertionError("InnerClasses attribute missing in class " + classFile.getName());
        }
        for (InnerClasses_attribute.Info info : inners.classes) {
            String entryName = info.getInnerClassInfo(classFile.constant_pool).getName();
            if (innerName.equals(entryName)) {
                return info.inner_class_access_flags;
            }
        }
        throw new AssertionError("No InnerClasses entry in class " + classFile.getName() + " for class " + innerName);
    }

}
