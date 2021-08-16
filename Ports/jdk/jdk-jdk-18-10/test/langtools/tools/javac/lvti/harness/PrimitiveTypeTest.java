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
 * @bug 8177466
 * @summary Add compiler support for local variable type-inference
 * @modules jdk.compiler/com.sun.source.tree
 *          jdk.compiler/com.sun.source.util
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.util
 * @build LocalVariableInferenceTester
 * @run main LocalVariableInferenceTester PrimitiveTypeTest.java
 */
class PrimitiveTypeTest {

    byte[] b_arr = { 0 };
    short[] s_arr = { 0 };
    int[] i_arr = { 0 };
    long[] l_arr = { 0 };
    float[] f_arr = { 0 };
    double[] d_arr = { 0 };
    char[] c_arr = { 0 };
    boolean[] z_arr = { false };

    void testPrimitive() {
        @InferredType("byte")
        var b = (byte)0;
        @InferredType("short")
        var s = (short)0;
        @InferredType("int")
        var i = 0;
        @InferredType("long")
        var l = 0L;
        @InferredType("float")
        var f = 0f;
        @InferredType("double")
        var d = 0d;
        @InferredType("char")
        var c = 'c';
        @InferredType("boolean")
        var z = false;
    }

    void testPrimitiveArray() {
        @InferredType("byte[]")
        var b = b_arr;
        @InferredType("short[]")
        var s = s_arr;
        @InferredType("int[]")
        var i = i_arr;
        @InferredType("long[]")
        var l = l_arr;
        @InferredType("float[]")
        var f = f_arr;
        @InferredType("double[]")
        var d = d_arr;
        @InferredType("char[]")
        var c = c_arr;
        @InferredType("boolean[]")
        var z = z_arr;
    }

    void testForEachPrimitive() {
        for (@InferredType("byte") var b : b_arr) { break; }
        for (@InferredType("short") var s : s_arr) { break;  }
        for (@InferredType("int") var i : i_arr) { break; }
        for (@InferredType("long") var l : l_arr) { break; }
        for (@InferredType("float") var f : f_arr) { break; }
        for (@InferredType("double") var d : d_arr) { break; }
        for (@InferredType("char") var c : c_arr) { break; }
        for (@InferredType("boolean") var z : z_arr) { break; }
    }
}
