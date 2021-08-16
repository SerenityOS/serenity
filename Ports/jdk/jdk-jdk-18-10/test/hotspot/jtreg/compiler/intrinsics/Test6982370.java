/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6982370
 * @summary SIGBUS in jbyte_fill
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+OptimizeFill -Xbatch
 *      compiler.intrinsics.Test6982370
 */

package compiler.intrinsics;

import java.util.Arrays;

/**
 * Exercise the fill routine for various short alignments and sizes
 */

public class Test6982370 {
    public static void main(String[] args) {
        test_byte();
        test_char();
        test_short();
        test_int();
        test_float();
    }

    public static void test_int() {
        int[] a = new int[16];
        for (int i = 0; i < 200000; i++) {
            int start = i & 7;
            int end = start + ((i >> 4) & 7);
            int value = i;
            if ((i & 1) == 1) value = -value;
            Arrays.fill(a, start, end, value);
            boolean error = false;
            for (int j = start; j < end; j++) {
                if (a[j] != value) {
                    System.err.println("a[" + j + "] = " + a[j] + " != " + value + " for " + a.length);
                    error = true;
                }
            }
            if (error) throw new InternalError();
        }
    }

    public static void test_float() {
        float[] a = new float[16];
        for (int i = 0; i < 200000; i++) {
            int start = i & 7;
            int end = start + ((i >> 4) & 7);
            float value = (float)i;
            if ((i & 1) == 1) value = -value;
            Arrays.fill(a, start, end, value);
            boolean error = false;
            for (int j = start; j < end; j++) {
                if (a[j] != value) {
                    System.err.println("a[" + j + "] = " + a[j] + " != " + value + " for " + a.length);
                    error = true;
                }
            }
            if (error) throw new InternalError();
        }
    }
    public static void test_char() {
        char[] a = new char[16];
        for (int i = 0; i < 200000; i++) {
            int start = i & 7;
            int end = start + ((i >> 4) & 7);
            char value = (char)i;
            Arrays.fill(a, start, end, value);
            boolean error = false;
            for (int j = start; j < end; j++) {
                if (a[j] != value) {
                    System.err.println("a[" + j + "] = " + a[j] + " != " + value + " for " + a.length);
                    error = true;
                }
            }
            if (error) throw new InternalError();
        }
    }
    public static void test_short() {
        short[] a = new short[16];
        for (int i = 0; i < 200000; i++) {
            int start = i & 7;
            int end = start + ((i >> 4) & 7);
            short value = (short)i;
            if ((i & 1) == 1) value = (short)-value;
            Arrays.fill(a, start, end, value);
            boolean error = false;
            for (int j = start; j < end; j++) {
                if (a[j] != value) {
                    System.err.println("a[" + j + "] = " + a[j] + " != " + value + " for " + a.length);
                    error = true;
                }
            }
            if (error) throw new InternalError();
        }
    }

    public static void test_byte() {
        for (int i = 0; i < 200000; i++) {
            byte[] a = new byte[16];
            int start = i & 7;
            int end = start + ((i >> 4) & 7);
            byte value = (byte)i;
            if ((i & 1) == 1) value = (byte)-value;
            Arrays.fill(a, start, end, value);
            boolean error = false;
            for (int j = start; j < end; j++) {
                if (a[j] != value) {
                    System.err.println("a[" + j + "] = " + a[j] + " != " + value + " for " + a.length);
                    error = true;
                }
            }
            if (error) throw new InternalError();
        }
    }
}
