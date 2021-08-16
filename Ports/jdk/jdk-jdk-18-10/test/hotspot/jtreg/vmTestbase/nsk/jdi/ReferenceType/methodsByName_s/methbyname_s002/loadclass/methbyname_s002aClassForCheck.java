/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ReferenceType.methodsByName_s;

/**
 * This class is loaded by methbyname_s002aClassLoader .
 */

class methbyname_s002aClassForCheck {

    // constructor
    public void ClassForCheck() {
    }

    // static methods
    static void s_void_method() {}
    static long    s_long_method() {return 100;}
    static String  s_string_method() {return "return";}
    static Object  s_object_method() {return new Object();}
    static long[]  s_prim_array_method() {return new long[100];}
    static Object[]  s_ref_array_method() {return new Object[100];}

    static void s_void_par_method(boolean z) {}
    static long    s_long_par_method(long l) {return 100;}
    static String  s_string_par_method(String s) {return "return";}
    static Object  s_object_par_method(Object obj) {return new Object();}
    static long[]  s_prim_array_par_method(long[] la) {return new long[100];}
    static Object[]  s_ref_array_par_method(Object[] obja) {return new Object[100];}


    // instance methods
    void i_void_method() {}
    long    i_long_method() {return 100;}
    String  i_string_method() {return "return";}
    Object  i_object_method() {return new Object();}
    long[]  i_prim_array_method() {return new long[100];}
    Object[]  i_ref_array_method() {return new Object[100];}

    void i_void_par_method(boolean z) {}
    long    i_long_par_method(long l) {return 100;}
    String  i_string_par_method(String s) {return "return";}
    Object  i_object_par_method(Object obj) {return new Object();}
    long[]  i_prim_array_par_method(long[] la) {return new long[100];}
    Object[]  i_ref_array_par_method(Object[] obja) {return new Object[100];}

    // static initializer
    static {}


}
