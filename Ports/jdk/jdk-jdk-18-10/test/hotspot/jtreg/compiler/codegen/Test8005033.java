/*
 * Copyright (c) 2012 SAP SE. All rights reserved.
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
 * @bug 8005033
 * @summary On sparcv9, C2's intrinsic for Integer.bitCount(OV) returns wrong result if OV is the result of an operation with int overflow.
 *
 * @run main/othervm -Xcomp
 *      -XX:CompileCommand=compileony,compiler.codegen.Test8005033::testBitCount
 *      compiler.codegen.Test8005033
 * @author Richard Reingruber richard DOT reingruber AT sap DOT com
 */

package compiler.codegen;

public class Test8005033 {
    public static int MINUS_ONE = -1;

    public static void main(String[] args) {
        System.out.println("EXECUTING test.");
        Integer.bitCount(1);   // load class
        int expectedBitCount = 0;
        int calculatedBitCount = testBitCount();
        if (expectedBitCount != calculatedBitCount) {
            throw new InternalError("got " + calculatedBitCount + " but expected " + expectedBitCount);
        }
        System.out.println("SUCCESSFULLY passed test.");
    }

    // testBitCount will be compiled using the Integer.bitCount() intrinsic if possible
    private static int testBitCount() {
        return Integer.bitCount(MINUS_ONE+1);   // -1 + 1 => int overflow
    }
}
