/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8074869
 * @summary C2 code generator can replace -0.0f with +0.0f on Linux
 * @run main compiler.loopopts.ConstFPVectorization 8
 * @author volker.simonis@gmail.com
 */

package compiler.loopopts;

public class ConstFPVectorization {

    static float[] f = new float[16];
    static double[] d = new double[16];

    static void floatLoop(int count) {
        for (int i = 0; i < count; i++) {
            f[i] = -0.0f;
        }
    }

    static void doubleLoop(int count) {
        for (int i = 0; i < count; i++) {
            d[i] = -0.0d;
        }
    }

    public static void main(String args[]) {
        for (int i = 0; i < 10_000; i++) {
            floatLoop(Integer.parseInt(args[0]));
            doubleLoop(Integer.parseInt(args[0]));
        }
        for (int i = 0; i < Integer.parseInt(args[0]); i++) {
            if (Float.floatToRawIntBits(f[i]) != Float.floatToRawIntBits(-0.0f))
                throw new Error("Float error at index " + i);
            if (Double.doubleToRawLongBits(d[i]) != Double.doubleToRawLongBits(-0.0d))
                throw new Error("Double error at index " + i);
        }
    }
}
