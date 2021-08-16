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
package org.openjdk.bench.java.util.stream.tasks.PrimesFilter;

import java.util.ArrayList;
import java.util.List;

public class PrimesProblem {

    /**
     * Factors n into its prime factors. If n is prime,
     * the result is a list of length 1 containing n.
     *
     * @param n the number to be factored
     * @return a list of prime factors
     */
    static List<Long> factor(long n) {
        List<Long> flist = new ArrayList<>();

        while (n % 2L == 0) {
            flist.add(2L);
            n /= 2L;
        }

        long divisor = 3L;
        while (n > 1L) {
            long quotient = n / divisor;
            if (n % divisor == 0) {
                flist.add(divisor);
                n = quotient;
            } else if (quotient > divisor) {
                divisor += 2L;
            } else {
                flist.add(n);
                break;
            }
        }

        return flist;
    }

    /**
     * Tests whether n is prime.
     *
     * @param n the number to be tested
     * @return true if n is prime, false if n is composite
     */
    public static boolean isPrime(long n) {
        List<Long> factors = factor(n);
        return (factors.size() == 1);
    }


}
