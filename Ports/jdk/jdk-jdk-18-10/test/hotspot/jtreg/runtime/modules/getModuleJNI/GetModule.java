/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary test JNI_GetModule() API
 * @run main/native GetModule
 */

import java.lang.management.LockInfo;
public class GetModule {

    static {
        System.loadLibrary("GetModule");
    }

    static native Object callGetModule(java.lang.Class clazz);

    public static void main(String[] args) {
        Module module;
        Module javaBaseModule;

        // Module for primitive type, should be "java.base"
        java.lang.Integer primitive_int = 1;
        try {
            javaBaseModule = (Module)callGetModule(primitive_int.getClass());
            if (!javaBaseModule.getName().equals("java.base")) {
                throw new RuntimeException("Unexpected module name for primitive type: " +
                                           javaBaseModule.getName());
            }
        } catch(Throwable e) {
            throw new RuntimeException("Unexpected exception for Integer: " + e.toString());
        }

        // Module for array of primitives, should be "java.base"
        int[] int_array = {1, 2, 3};
        try {
            javaBaseModule = (Module)callGetModule(int_array.getClass());
            if (!javaBaseModule.getName().equals("java.base")) {
                throw new RuntimeException("Unexpected module name for array of primitives: " +
                                           javaBaseModule.getName());
            }
        } catch(Throwable e) {
            throw new RuntimeException("Unexpected exception for [I: " + e.toString());
        }

        // Module for multi-dimensional array of primitives, should be "java.base"
        int[][] multi_int_array = { {1, 2, 3}, {4, 5, 6} };
        try {
            javaBaseModule = (Module)callGetModule(multi_int_array.getClass());
            if (!javaBaseModule.getName().equals("java.base")) {
                throw new RuntimeException("Unexpected module name for multi-dimensional array of primitives: " +
                                           javaBaseModule.getName());
            }
        } catch(Throwable e) {
            throw new RuntimeException("Unexpected exception for multi-dimensional Integer array: " + e.toString());
        }

        // Module for java.lang.String, should be "java.base"
        java.lang.String str = "abc";
        try {
            module = (Module)callGetModule(str.getClass());
            if (!module.getName().equals("java.base")) {
                throw new RuntimeException("Unexpected module name for class String: " +
                                           module.getName());
            }
        } catch(Throwable e) {
            throw new RuntimeException("Unexpected exception for String: " + e.toString());
        }

        // Module for array of java.lang.Strings, should be "java.base"
        java.lang.String[] str_array = {"a", "b", "c"};
        try {
            javaBaseModule = (Module)callGetModule(str_array.getClass());
            if (!javaBaseModule.getName().equals("java.base")) {
                throw new RuntimeException("Unexpected module name for array of Strings: " +
                                           javaBaseModule.getName());
            }
        } catch(Throwable e) {
            throw new RuntimeException("Unexpected exception for String array: " + e.toString());
        }

        // Module for multi-dimensional array of java.lang.Strings, should be "java.base"
        java.lang.String[][] multi_str_array = { {"a", "b", "c"}, {"d", "e", "f"} };
        try {
            javaBaseModule = (Module)callGetModule(multi_str_array.getClass());
            if (!javaBaseModule.getName().equals("java.base")) {
                throw new RuntimeException("Unexpected module name for multi-dimensional array of Strings: " +
                                           javaBaseModule.getName());
            }
        } catch(Throwable e) {
            throw new RuntimeException("Unexpected exception for multidimensional String array: " + e.toString());
        }

        // Module for java.lang.management.LockInfo
        try {
            LockInfo li = new LockInfo("java.lang.Class", 57);
            module = (Module)callGetModule(li.getClass());
            if (!module.getName().equals("java.management")) {
                throw new RuntimeException("Unexpected module name for class LockInfo: " +
                                           module.getName());
            }
        } catch(Throwable e) {
            throw new RuntimeException("Unexpected exception for LockInfo: " + e.toString());
        }

        // Unnamed module.
        try {
            module = (Module)callGetModule(MyClassLoader.class);
            if (module == null || module.getName() != null) {
                throw new RuntimeException("Bad module for unnamed module");
            }
        } catch(Throwable e) {
            throw new RuntimeException("Unexpected exception for unnamed module: " + e.toString());
        }

        try {
            module = (Module)callGetModule(null);
            throw new RuntimeException("Failed to get expected NullPointerException");
        } catch(NullPointerException e) {
            // Expected
        }
    }

    static class MyClassLoader extends ClassLoader { }
}
