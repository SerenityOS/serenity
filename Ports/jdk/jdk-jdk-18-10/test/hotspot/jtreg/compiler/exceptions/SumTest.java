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
 * @bug 8066900
 * @summary FP registers are not properly restored by C1 when handling exceptions
 *
 * @run main/othervm -Xbatch compiler.exceptions.SumTest
 */

package compiler.exceptions;

public class SumTest {
    private static class Sum {

        double[] sums;

        /**
         * Construct empty Sum
         */
        public Sum() {
            sums = new double[0];
        }

        /**
         * Return the sum of all numbers added to this Sum
         *
         * @return the sum
         */
        final public double getSum() {
            double sum = 0;
            for (final double s : sums) {
                sum += s;
            }

            return sum;
        }

        /**
         * Add a new number to this Sum
         *
         * @param a number to be added.
         */
        final public void add(double a) {
            try {
                sums[sums.length] = -1; // Cause IndexOutOfBoundsException
            } catch (final IndexOutOfBoundsException e) {
                final double[] oldSums = sums;
                sums = new double[oldSums.length + 1]; // Extend sums
                System.arraycopy(oldSums, 0, sums, 0, oldSums.length);
                sums[oldSums.length] = a; // Append a
            }
        }
    }

    public static void main(String[] args) throws Exception {
        final Sum sum = new Sum();
        for (int i = 1; i <= 10000; ++i) {
            sum.add(1);
            double ii = sum.getSum();
            if (i != ii) {
                throw new Exception("Failure: computed = " + ii + ", expected = " + i);
            }
        }
    }

}

