/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8167408
 * @requires (os.arch != "aarch64") & (os.arch != "arm") & (vm.flavor != "zero")
 * @run main/othervm/native -Xcomp -XX:+CriticalJNINatives compiler.runtime.criticalnatives.lookup.LookUp
 */
package compiler.runtime.criticalnatives.lookup;
public class LookUp {
    static {
        System.loadLibrary("CNLookUp");
    }
    static native void m1(byte a1, long a2, char a3, int a4,  float a5, double a6, byte[] result);
    static native void m2(int a1, int[] a2, long a3, long[] a4, float a5,float[] a6, double a7, double[] a8, byte result[]);
    public static void main(String args[]) throws Exception {
        test();
    }
    private static void test() throws Exception {
        int[] l1 = { 1111, 2222, 3333 };
        long[] l2 = { 4444L, 5555L, 6666L };
        float[] l3 = { 7777.0F, 8888.0F, 9999.0F };
        double[] l4 = { 4545.0D, 5656.0D, 6767.0D };
        byte[] result = { -1 };
        m1((byte)0xA, 4444444455555555L, 'A', 12345678, 343434.0F, 6666666677777777.0D, result);
        check(result[0]);
        result[0] = -1;
        m2(12345678, l1, 4444444455555555L, l2, 343434.0F, l3, 6666666677777777.0D, l4, result);
        check(result[0]);
    }
    private static void check(byte result) throws Exception {
        if (result != 2) {
            if (result == 1) {
              throw new Exception("critical native arguments mismatch");
            }
            throw new Exception("critical native lookup failed");
        }
    }
}
