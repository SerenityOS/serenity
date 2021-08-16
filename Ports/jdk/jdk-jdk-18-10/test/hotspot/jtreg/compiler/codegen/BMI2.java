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
 * @summary Support BMI2 instructions on x86/x64
 *
 * @run main/othervm -Xbatch -XX:-TieredCompilation -XX:+UnlockDiagnosticVMOptions -XX:+AbortVMOnCompilationFailure
 *      -XX:CompileCommand=dontinline,compiler.codegen.BMI2$BMITests::*
 *      compiler.codegen.BMI2
 */

package compiler.codegen;

public class BMI2 {
    private final static int ITERATIONS = 30000;

    // match(Set dst (ConvI2L (AndI src1 mask)))
    public static void testBzhiI2L(int ix) {
        long[] goldv = new long[16];
        for (int i = 0; i <= 15; i++) {
              goldv[i] = BMITests.bzhiI2L(ix, i);
        }
        for (int i2 = 0; i2 < ITERATIONS; i2++) {
             for (int i = 0; i <= 15; i++) {
                  long v = BMITests.bzhiI2L(ix, i);
                  if (v != goldv[i]) {
                          throw new Error(returnBzhiI2LErrMessage (goldv[i], v));
                  }
             }
       }
    }

    private static String returnBzhiI2LErrMessage (long value, long value2) {
        return "bzhi I2L with register failed, uncompiled result: " + value + " does not match compiled result: " + value2;
    }

    static class BMITests {

        static long bzhiI2L(int src1, int src2) {

            switch(src2) {
                case 0:
                    return (long)(src1 & 0x1);
                case 1:
                    return (long)(src1 & 0x3);
                case 2:
                    return (long)(src1 & 0x7);
                case 3:
                    return (long)(src1 & 0xF);
                case 4:
                    return (long)(src1 & 0x1F);
                case 5:
                    return (long)(src1 & 0x3F);
                case 6:
                    return (long)(src1 & 0x7F);
                case 7:
                    return (long)(src1 & 0xFF);
                case 8:
                    return (long)(src1 & 0x1FF);
                case 9:
                    return (long)(src1 & 0x3FF);
                case 10:
                    return (long)(src1 & 0x7FF);
                case 11:
                    return (long)(src1 & 0xFFF);
                case 12:
                    return (long)(src1 & 0x1FFF);
                case 13:
                    return (long)(src1 & 0x3FFF);
                case 14:
                    return (long)(src1 & 0x7FFF);
                case 15:
                    return (long)(src1 & 0xFFFF);
                default:
                    return (long)(src1 & 0xFFFF);
            }
        }
    }

    public static void main(String[] args) {
        testBzhiI2L(0);
        testBzhiI2L(1);
    }
}
