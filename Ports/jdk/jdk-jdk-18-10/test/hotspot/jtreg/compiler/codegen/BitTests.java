/*
 * Copyright (c) 2015, Red Hat, Inc. All rights reserved.
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
 * @bug 8144028
 * @summary Use AArch64 bit-test instructions in C2
 *
 * @run main/othervm -Xbatch -XX:-TieredCompilation
 *      -XX:CompileCommand=dontinline,compiler.codegen.BitTests::*
 *      compiler.codegen.BitTests
 * @run main/othervm -Xbatch -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *      compiler.codegen.BitTests
 * @run main/othervm -Xbatch -XX:+TieredCompilation
 *      compiler.codegen.BitTests
 */

package compiler.codegen;

// Try to ensure that the bit test instructions TBZ/TBNZ, TST/TSTW
// don't generate incorrect code.  We can't guarantee that C2 will use
// bit test instructions for this test and it's not a bug if it
// doesn't.  However, these test cases are ideal candidates for each
// of the instruction forms.
public class BitTests {

    private final XorShift r = new XorShift();

    private final long increment(long ctr) {
        return ctr + 1;
    }

    private final int increment(int ctr) {
        return ctr + 1;
    }

    private final long testIntSignedBranch(long counter) {
        if ((int) r.nextLong() < 0) {
            counter = increment(counter);
        }
        return counter;
    }

    private final long testLongSignedBranch(long counter) {
        if (r.nextLong() < 0) {
            counter = increment(counter);
        }
        return counter;
    }

    private final long testIntBitBranch(long counter) {
        if (((int) r.nextLong() & (1 << 27)) != 0) {
            counter = increment(counter);
        }
        if (((int) r.nextLong() & (1 << 27)) != 0) {
            counter = increment(counter);
        }
        return counter;
    }

    private final long testLongBitBranch(long counter) {
        if ((r.nextLong() & (1l << 50)) != 0) {
            counter = increment(counter);
        }
        if ((r.nextLong() & (1l << 50)) != 0) {
            counter = increment(counter);
        }
        return counter;
    }

    private final long testLongMaskBranch(long counter) {
        if (((r.nextLong() & 0x0800000000l) != 0)) {
            counter++;
        }
        return counter;
    }

    private final long testIntMaskBranch(long counter) {
        if ((((int) r.nextLong() & 0x08) != 0)) {
            counter++;
        }
        return counter;
    }

    private final long testLongMaskBranch(long counter, long mask) {
        if (((r.nextLong() & mask) != 0)) {
            counter++;
        }
        return counter;
    }

    private final long testIntMaskBranch(long counter, int mask) {
        if ((((int) r.nextLong() & mask) != 0)) {
            counter++;
        }
        return counter;
    }

    private final long step(long counter) {
        counter = testIntSignedBranch(counter);
        counter = testLongSignedBranch(counter);
        counter = testIntBitBranch(counter);
        counter = testLongBitBranch(counter);
        counter = testIntMaskBranch(counter);
        counter = testLongMaskBranch(counter);
        counter = testIntMaskBranch(counter, 0x8000);
        counter = testLongMaskBranch(counter, 0x800000000l);
        return counter;
    }


    private final long finalBits = 3;

    private long bits = 7;

    public static void main(String[] args) {
        BitTests t = new BitTests();

        long counter = 0;
        for (int i = 0; i < 10000000; i++) {
            counter = t.step((int) counter);
        }
        if (counter != 50001495) {
            System.err.println("FAILED: counter = " + counter + ", should be 50001495.");
            System.exit(97);
        }
        System.out.println("PASSED");
    }

    // Marsaglia's xor-shift generator, used here because it is
    // reproducible across all Java implementations.  It is also very
    // fast.
    static class XorShift {

        private long y;

        XorShift() {
            y = 2463534242l;
        }

        public long nextLong() {
            y ^= (y << 13);
            y ^= (y >>> 17);
            return (y ^= (y << 5));

        }
    }
}
