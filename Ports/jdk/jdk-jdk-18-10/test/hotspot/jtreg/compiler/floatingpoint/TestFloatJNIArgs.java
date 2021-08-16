/*
 * Copyright (c) 2015, 2016 SAP SE. All rights reserved.
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
 * @bug 8139258 8165673
 * @summary Regression test for passing float args to a jni function.
 *
 *
 * @run main/othervm/native -Xint compiler.floatingpoint.TestFloatJNIArgs
 * @run main/othervm/native -XX:+TieredCompilation -Xcomp compiler.floatingpoint.TestFloatJNIArgs
 */

/* @test
 * @bug 8139258 8165673
 * @summary Regression test for passing float args to a jni function.
 *
 * @requires !vm.graal.enabled
 * @run main/othervm/native -XX:-TieredCompilation -Xcomp compiler.floatingpoint.TestFloatJNIArgs
 */

package compiler.floatingpoint;

public class TestFloatJNIArgs {
    static {
        try {
            System.loadLibrary("TestFloatJNIArgs");
        } catch (UnsatisfiedLinkError e) {
            System.out.println("could not load native lib: " + e);
        }
    }

    public static native float add15floats(
        float f1, float f2, float f3, float f4,
        float f5, float f6, float f7, float f8,
        float f9, float f10, float f11, float f12,
        float f13, float f14, float f15);

    public static native float add10floats(
        float f1, float f2, float f3, float f4,
        float f5, float f6, float f7, float f8,
        float f9, float f10);

    public static native float addFloatsInts(
        float f1, float f2, float f3, float f4,
        float f5, float f6, float f7, float f8,
        float f9, float f10, float f11, float f12,
        float f13, float f14, float f15, int a16, int a17);

    public static native double add15doubles(
        double d1, double d2, double d3, double d4,
        double d5, double d6, double d7, double d8,
        double d9, double d10, double d11, double d12,
        double d13, double d14, double d15);

    static void test() throws Exception {
        float sum = TestFloatJNIArgs.add15floats(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                                                 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
        if (sum != 15.0f) {
            throw new Error("Passed 15 times 1.0f to jni function which didn't add them properly: " + sum);
        }

        float sum1 = TestFloatJNIArgs.add10floats(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
        if (sum1 != 10.0f) {
            throw new Error("Passed 10 times 1.0f to jni function which didn't add them properly: " + sum1);
        }

        float sum2 = TestFloatJNIArgs.addFloatsInts(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                                                   1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1, 1);
        if (sum2 != 17.0f) {
            throw new Error("Passed 17 times 1 to jni function which didn't add them properly: " + sum2);
        }

        double dsum = TestFloatJNIArgs.add15doubles(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
                                                      1.0, 1.0, 1.0, 1.0, 1.0, 1.0);
        if (dsum != 15.0) {
            throw new Error("Passed 15 times 1.0 to jni function which didn't add them properly: " + dsum);
        }
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 200; ++i) {
            test();
        }
    }
}
