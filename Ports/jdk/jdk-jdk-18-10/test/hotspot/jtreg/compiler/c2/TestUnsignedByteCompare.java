/*
 * Copyright (c) 2018, Red Hat Inc. All rights reserved.
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
 * @bug 8204479
 * @summary Bitwise AND on byte value sometimes produces wrong result
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:-TieredCompilation
 *      -XX:-UseOnStackReplacement -XX:-BackgroundCompilation -Xcomp -XX:-Inline
 *      compiler.c2.TestUnsignedByteCompare
 */

package compiler.c2;

public class TestUnsignedByteCompare {

    static int p, n;

    static void report(byte[] ba, int i, boolean failed) {
        // Enable for debugging:
        // System.out.println((failed ? "Failed" : "Passed") + " with: " + ba[i] + " at " + i);
    }

    static void m1(byte[] ba) {
        for (int i = 0; i < ba.length; i++) {
            if ((ba[i] & 0xFF) < 0x10) {
               p++;
               report(ba, i, true);
            } else {
               n++;
               report(ba, i, false);
            }
        }
    }

    static void m2(byte[] ba) {
        for (int i = 0; i < ba.length; i++) {
            if (((ba[i] & 0xFF) & 0x80) < 0) {
               p++;
               report(ba, i, true);
            } else {
               n++;
               report(ba, i, false);
            }
        }
    }

    static public void main(String[] args) {
        final int tries = 1_000;
        final int count = 1_000;

        byte[] ba = new byte[count];

        for (int i = 0; i < count; i++) {
            int v = -(i % 126 + 1);
            ba[i] = (byte)v;
        }

        for (int t = 0; t < tries; t++) {
            m1(ba);
            if (p != 0) {
                throw new IllegalStateException("m1 error: p = " + p + ", n = " + n);
            }
        }

        for (int t = 0; t < tries; t++) {
            m2(ba);
            if (p != 0) {
                throw new IllegalStateException("m2 error: p = " + p + ", n = " + n);
            }
        }
    }
}
