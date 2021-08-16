/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8031321
 * @summary Support BMI1 instructions on x86/x64
 *
 * @run main/othervm -Xbatch -XX:-TieredCompilation
 *      -XX:CompileCommand=compileonly,compiler.codegen.BMI1$BMITests::*
 *      compiler.codegen.BMI1
 */

package compiler.codegen;

public class BMI1 {
    private final static int ITERATIONS = 1000000;

    public static void main(String[] args) {
        int ix = 0x01234567;
        int iy = 0x89abcdef;
        MemI imy = new MemI(iy);
        long lx = 0x0123456701234567L;
        long ly = 0x89abcdef89abcdefL;
        MemL lmy = new MemL(ly);

        { // match(Set dst (AndI (XorI src1 minus_1) src2))
            int z = BMITests.andnl(ix, iy);
            for (int i = 0; i < ITERATIONS; i++) {
                int ii = BMITests.andnl(ix, iy);
                if (ii != z) {
                    throw new Error("andnl with register failed");
                }
            }
        }
        { // match(Set dst (AndL (XorL src1 minus_1) src2))
            long z = BMITests.andnq(lx, ly);
            for (int i = 0; i < ITERATIONS; i++) {
                long ll = BMITests.andnq(lx, ly);
                if (ll != z) {
                    throw new Error("andnq with register failed");
                }
            }
        }
        { // match(Set dst (AndI (XorI src1 minus_1) (LoadI src2)))
            int z = BMITests.andnl(ix, imy);
            for (int i = 0; i < ITERATIONS; i++) {
                int ii = BMITests.andnl(ix, imy);
                if (ii != z) {
                    throw new Error("andnl with memory failed");
                }
            }
        }
        { // match(Set dst (AndL (XorL src1 minus_1) (LoadL src2)))
            long z = BMITests.andnq(lx, lmy);
            for (int i = 0; i < ITERATIONS; i++) {
                long ll = BMITests.andnq(lx, lmy);
                if (ll != z) {
                    throw new Error("andnq with memory failed");
                }
            }
        }
        { // match(Set dst (AndI (SubI imm_zero src) src))
            int z = BMITests.blsil(ix);
            for (int i = 0; i < ITERATIONS; i++) {
                int ii = BMITests.blsil(ix);
                if (ii != z) {
                    throw new Error("blsil with register failed");
                }
            }
        }
        { // match(Set dst (AndL (SubL imm_zero src) src))
            long z = BMITests.blsiq(lx);
            for (int i = 0; i < ITERATIONS; i++) {
                long ll = BMITests.blsiq(lx);
                if (ll != z) {
                    throw new Error("blsiq with register failed");
                }
            }
        }
        { // match(Set dst (AndI (SubI imm_zero (LoadI src) ) (LoadI src) ))
            int z = BMITests.blsil(imy);
            for (int i = 0; i < ITERATIONS; i++) {
                int ii = BMITests.blsil(imy);
                if (ii != z) {
                    throw new Error("blsil with memory failed");
                }
            }
        }
        { // match(Set dst (AndL (SubL imm_zero (LoadL src) ) (LoadL src) ))
            long z = BMITests.blsiq(lmy);
            for (int i = 0; i < ITERATIONS; i++) {
                long ll = BMITests.blsiq(lmy);
                if (ll != z) {
                    throw new Error("blsiq with memory failed");
                }
            }
        }

        { // match(Set dst (XorI (AddI src minus_1) src))
            int z = BMITests.blsmskl(ix);
            for (int i = 0; i < ITERATIONS; i++) {
                int ii = BMITests.blsmskl(ix);
                if (ii != z) {
                    throw new Error("blsmskl with register failed");
                }
            }
        }
        { // match(Set dst (XorL (AddL src minus_1) src))
            long z = BMITests.blsmskq(lx);
            for (int i = 0; i < ITERATIONS; i++) {
                long ll = BMITests.blsmskq(lx);
                if (ll != z) {
                    throw new Error("blsmskq with register failed");
                }
            }
        }
        { // match(Set dst (XorI (AddI (LoadI src) minus_1) (LoadI src) ) )
            int z = BMITests.blsmskl(imy);
            for (int i = 0; i < ITERATIONS; i++) {
                int ii = BMITests.blsmskl(imy);
                if (ii != z) {
                    throw new Error("blsmskl with memory failed");
                }
            }
        }
        {  // match(Set dst (XorL (AddL (LoadL src) minus_1) (LoadL src) ) )
            long z = BMITests.blsmskq(lmy);
            for (int i = 0; i < ITERATIONS; i++) {
                long ll = BMITests.blsmskq(lmy);
                if (ll != z) {
                    throw new Error("blsmskq with memory failed");
                }
            }
        }

        { //  match(Set dst (AndI (AddI src minus_1) src) )
            int z = BMITests.blsrl(ix);
            for (int i = 0; i < ITERATIONS; i++) {
                int ii = BMITests.blsrl(ix);
                if (ii != z) {
                    throw new Error("blsrl with register failed");
                }
            }
        }
        { // match(Set dst (AndL (AddL src minus_1) src) )
            long z = BMITests.blsrq(lx);
            for (int i = 0; i < ITERATIONS; i++) {
                long ll = BMITests.blsrq(lx);
                if (ll != z) {
                    throw new Error("blsrq with register failed");
                }
            }
        }
        { // match(Set dst (AndI (AddI (LoadI src) minus_1) (LoadI src) ) )
            int z = BMITests.blsrl(imy);
            for (int i = 0; i < ITERATIONS; i++) {
                int ii = BMITests.blsrl(imy);
                if (ii != z) {
                    throw new Error("blsrl with memory failed");
                }
            }
        }
        { // match(Set dst (AndL (AddL (LoadL src) minus_1) (LoadL src)) )
            long z = BMITests.blsrq(lmy);
            for (int i = 0; i < ITERATIONS; i++) {
                long ll = BMITests.blsrq(lmy);
                if (ll != z) {
                    throw new Error("blsrq with memory failed");
                }
            }
        }

        {
            int z = BMITests.lzcntl(ix);
            for (int i = 0; i < ITERATIONS; i++) {
                int ii = BMITests.lzcntl(ix);
                if (ii != z) {
                    throw new Error("lzcntl failed");
                }
            }
        }
        {
            int z = BMITests.lzcntq(lx);
            for (int i = 0; i < ITERATIONS; i++) {
                int ii = BMITests.lzcntq(lx);
                if (ii != z) {
                    throw new Error("lzcntq failed");
                }
            }
        }

        {
            int z = BMITests.tzcntl(ix);
            for (int i = 0; i < ITERATIONS; i++) {
                int ii = BMITests.tzcntl(ix);
                if (ii != z) {
                    throw new Error("tzcntl failed");
                }
            }
        }
        {
            int z = BMITests.tzcntq(lx);
            for (int i = 0; i < ITERATIONS; i++) {
                int ii = BMITests.tzcntq(lx);
                if (ii != z) {
                    throw new Error("tzcntq failed");
                }
            }
        }
    }

    static class MemI {
        public int x;

        public MemI(int x) {
            this.x = x;
        }
    }

    static class MemL {
        public long x;

        public MemL(long x) {
            this.x = x;
        }
    }

    static class BMITests {
        static int andnl(int src1, int src2) {
            return ~src1 & src2;
        }

        static long andnq(long src1, long src2) {
            return ~src1 & src2;
        }

        static int andnl(int src1, MemI src2) {
            return ~src1 & src2.x;
        }

        static long andnq(long src1, MemL src2) {
            return ~src1 & src2.x;
        }

        static int blsil(int src1) {
            return src1 & -src1;
        }

        static long blsiq(long src1) {
            return src1 & -src1;
        }

        static int blsil(MemI src1) {
            return src1.x & -src1.x;
        }

        static long blsiq(MemL src1) {
            return src1.x & -src1.x;
        }

        static int blsmskl(int src1) {
            return (src1 - 1) ^ src1;
        }

        static long blsmskq(long src1) {
            return (src1 - 1) ^ src1;
        }

        static int blsmskl(MemI src1) {
            return (src1.x - 1) ^ src1.x;
        }

        static long blsmskq(MemL src1) {
            return (src1.x - 1) ^ src1.x;
        }

        static int blsrl(int src1) {
            return (src1 - 1) & src1;
        }

        static long blsrq(long src1) {
            return (src1 - 1) & src1;
        }

        static int blsrl(MemI src1) {
            return (src1.x - 1) & src1.x;
        }

        static long blsrq(MemL src1) {
            return (src1.x - 1) & src1.x;
        }

        static int lzcntl(int src1) {
            return Integer.numberOfLeadingZeros(src1);
        }

        static int lzcntq(long src1) {
            return Long.numberOfLeadingZeros(src1);
        }

        static int tzcntl(int src1) {
            return Integer.numberOfTrailingZeros(src1);
        }

        static int tzcntq(long src1) {
            return Long.numberOfTrailingZeros(src1);
        }
    }
}
