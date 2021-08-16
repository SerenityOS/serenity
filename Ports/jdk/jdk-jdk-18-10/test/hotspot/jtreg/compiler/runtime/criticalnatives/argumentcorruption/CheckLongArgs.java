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
 * @bug 8167409
 * @requires (os.arch != "aarch64") & (os.arch != "arm") & (vm.flavor != "zero")
 * @run main/othervm/native -Xcomp -XX:+CriticalJNINatives compiler.runtime.criticalnatives.argumentcorruption.CheckLongArgs
 */
package compiler.runtime.criticalnatives.argumentcorruption;
public class CheckLongArgs {
    static {
        System.loadLibrary("CNCheckLongArgs");
    }
    static native void m1(long a1, long a2, long a3, long a4,  long a5, long a6, long a7, long a8, byte[] result);
    static native void m2(long a1, int[] a2, long a3, int[] a4, long a5, int[] a6, long a7, int[] a8, long a9, byte[] result);
    public static void main(String args[]) throws Exception {
        test();
    }
    private static void test() throws Exception {
        int[] l1 = { 1111, 2222, 3333 };
        int[] l2 = { 4444, 5555, 6666 };
        int[] l3 = { 7777, 8888, 9999 };
        int[] l4 = { 1010, 2020, 3030 };
        byte[] result = { -1 };
        m1(1111111122222222L, 3333333344444444L, 5555555566666666L, 7777777788888888L, 9999999900000000L, 1212121234343434L,
           5656565678787878L, 9090909012121212L, result);
        check(result[0]);
        result[0] = -1;
        m2(1111111122222222L, l1, 3333333344444444L, l2, 5555555566666666L, l3, 7777777788888888L, l4, 9999999900000000L, result);
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
