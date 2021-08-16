/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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
 * @bug 8259641
 * @summary C2: assert(early->dominates(LCA)) failed: early is high enough
 *
 * @run main/othervm -Xcomp -XX:CompileOnly=TestBrokenAntiDependenceWithPhi TestBrokenAntiDependenceWithPhi
 *
 */

public class TestBrokenAntiDependenceWithPhi {

    int a;
    int b;
    byte c;

    long e(int f, int g, long h) {
        int i[] = new int[a];
        double j = 2.74886;
        long k[][] = new long[a][a];
        long l = checkSum(k);
        return l;
    }

    void m() {
        int s, o, p[] = new int[a];
        double d;
        for (d = 5; d < 388; d++) {
            e(b, b, 40418347472393L);
            for (s = 3; s < 66; ++s)
                int1array(a, 9);
        }
        for (o = 6; o > 2; o--)
            p[o] = c;
    }

    public static void main(String[] q) {
        TestBrokenAntiDependenceWithPhi r = new TestBrokenAntiDependenceWithPhi();
        try {
            r.m();
        } catch (ArrayIndexOutOfBoundsException aioobe) {
        }
    }

    public static long checkSum(long[] a) {
        long sum = 0;
        for (int j = 0; j < a.length; j++) {
            sum += (a[j] / (j + 1) + a[j] % (j + 1));
        }
        return sum;
    }

    public static long checkSum(long[][] a) {
        long sum = 0;
        for (int j = 0; j < a.length; j++) {
            sum += checkSum(a[j]);
        }
        return sum;
    }

    public static int[] int1array(int sz, int seed) {
        int[] ret = new int[sz];
        init(ret, seed);
        return ret;
    }

    public static void init(int[] a, int seed) {
        for (int j = 0; j < a.length; j++) {
            a[j] = (j % 2 == 0) ? seed + j : seed - j;
        }
    }

}
