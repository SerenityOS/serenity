/*
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/**
 * Misc utilities in JSR166 performance tests
 */
class LoopHelpers {

    // Some mindless computation to do between synchronizations...

    /**
     * generates 32 bit pseudo-random numbers.
     * Adapted from http://www.snippets.org
     */
    public static int compute1(int x) {
        int lo = 16807 * (x & 0xFFFF);
        int hi = 16807 * (x >>> 16);
        lo += (hi & 0x7FFF) << 16;
        if ((lo & 0x80000000) != 0) {
            lo &= 0x7fffffff;
            ++lo;
        }
        lo += hi >>> 15;
        if (lo == 0 || (lo & 0x80000000) != 0) {
            lo &= 0x7fffffff;
            ++lo;
        }
        return lo;
    }

    /**
     *  Computes a linear congruential random number a random number
     *  of times.
     */
    public static int compute2(int x) {
        int loops = (x >>> 4) & 7;
        while (loops-- > 0) {
            x = (x * 2147483647) % 16807;
        }
        return x;
    }

    public static class BarrierTimer implements Runnable {
        public volatile long startTime;
        public volatile long endTime;
        public void run() {
            long t = System.nanoTime();
            if (startTime == 0)
                startTime = t;
            else
                endTime = t;
        }
        public void clear() {
            startTime = 0;
            endTime = 0;
        }
        public long getTime() {
            return endTime - startTime;
        }
    }

    public static String rightJustify(long n) {
        // There's probably a better way to do this...
        String field = "         ";
        String num = Long.toString(n);
        if (num.length() >= field.length())
            return num;
        StringBuilder b = new StringBuilder(field);
        b.replace(b.length()-num.length(), b.length(), num);
        return b.toString();
    }

}
