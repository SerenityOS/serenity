/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8262913
 * @summary Verifies DefineClass with null or truncate bytes gets appropriate exception
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @compile A.java
 * @run main/native NullClassBytesTest
 */

import jdk.test.lib.classloader.ClassUnloadCommon;

public class NullClassBytesTest {

    static native Class<?> nativeDefineClass(String name, ClassLoader ldr, byte[] class_bytes, int length);

    static {
        System.loadLibrary("NullClassBytesTest");
    }

    static class SimpleLoader extends ClassLoader {

        public Class<?> loadClass(String name) throws ClassNotFoundException {
            synchronized(getClassLoadingLock(name)) {
                Class<?> c = findLoadedClass(name);
                if (c != null) return c;

                // load the class data from the connection
                if (name.equals("A")) {
                    byte[] b = ClassUnloadCommon.getClassData("A");
                    return defineClass(name, b, 0, b.length);
                } else if (name.equals("B")) {
                    byte[] b = new byte[0];
                    return defineClass(name, b, 0, b.length);
                } else if (name.equals("C")) {
                    byte[] b = null;
                    return defineClass(name, b, 0, 0);
                } else if (name.equals("D")) {
                    byte[] b = new byte[0];
                    return nativeDefineClass(name, this, b, b.length);
                } else if (name.equals("E")) {
                    byte[] b = null;
                    return nativeDefineClass(name, this, b, 0);
                } else {
                    return super.loadClass(name);
                }
            }
        }
    }

    public static void main(java.lang.String[] unused) throws Exception {
        SimpleLoader ldr = new SimpleLoader();
        Class<?> a = Class.forName("A", true, ldr);
        Object obj = a.getConstructor().newInstance();

        // If byte array points to null, the JVM throws ClassFormatError("Truncated class file")
        try {
            Class<?> b = Class.forName("B", true, ldr);
        } catch (ClassFormatError cfe) {
            if (!cfe.getMessage().contains("Truncated class file")) {
                cfe.printStackTrace();
                throw new RuntimeException("Wrong message");
            }
        }

        // If byte array is null, ClassLoader native detects this and throws NPE
        // before calling JVM_DefineClassWithSource
        try {
            Class<?> c = Class.forName("C", true, ldr);
        } catch (NullPointerException npe) {}

        // Test JNI_DefineClass with truncated bytes
        try {
            Class<?> c = Class.forName("D", true, ldr);
        } catch (ClassFormatError cfe) {
            if (!cfe.getMessage().contains("Truncated class file")) {
                cfe.printStackTrace();
                throw new RuntimeException("Wrong message");
            }
        }

        // Native methods must throw their own NPE
        try {
            Class<?> c = Class.forName("E", true, ldr);
        } catch (NullPointerException npe) {
            if (!npe.getMessage().equals("class_bytes are null")) {
                npe.printStackTrace();
                throw new RuntimeException("Wrong message");
            }
        }
        System.out.println("TEST PASSED");
    }
}
