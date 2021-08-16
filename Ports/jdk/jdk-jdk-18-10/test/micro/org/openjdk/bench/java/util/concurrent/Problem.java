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
package org.openjdk.bench.java.util.concurrent;


/**
 * Generic problem for concurrency tests.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class Problem {

    /*
     * Implementation notes:
     *
     * This problem makes its bidding to confuse loop unrolling and CSE, and as such break loop optimizations.
     * Should loop optimizations be allowed, the performance with different (l, r) could change non-linearly.
     */

    private final int[] data;
    private final int size;

    public Problem(int size) {
        this.size = size;
        data = new int[size];
    }

    public long solve() {
        return solve(0, size);
    }

    public long solve(int l, int r) {
        long sum = 0;
        for (int c = l; c < r; c++) {
            int v = hash(data[c]);
            if (filter(v)) {
                sum += v;
            }
        }
        return sum;
    }

    public int size() {
        return size;
    }

    public static int hash(int x) {
        x ^= (x << 21);
        x ^= (x >>> 31);
        x ^= (x << 4);
        return x;
    }

    public static boolean filter(int i) {
        return ((i & 0b101) == 0);
    }

}
