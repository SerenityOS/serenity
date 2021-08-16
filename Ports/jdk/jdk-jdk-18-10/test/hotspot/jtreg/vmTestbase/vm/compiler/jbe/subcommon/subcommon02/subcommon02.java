/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase runtime/jbe/subcommon/subcommon02.
 * VM Testbase keywords: [runtime]
 * VM Testbase comments: 7190319
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.jbe.subcommon.subcommon02.subcommon02
 */

package vm.compiler.jbe.subcommon.subcommon02;

/* -- Common subexpression elimination testing
   Using global common subexpression in method fopt() to calculate x**n.
 */
import java.io.*;

public class subcommon02 {
    int LEN = 5000;
    int WIDTH = 20;
    int ngrt10000 = 0; // number of elements > 10,000
    int ngrtO10000 = 0;
    int ngrt1000 = 0; // number of elements > 1,000
    int ngrtO1000 = 0;
    int ngrt100 = 0; // number of elements > 100
    int ngrtO100 = 0;
    int nsmet100 = 0; // number of elements <= 100
    int nsmetO100 = 0;
    double a[][] = new double[LEN][WIDTH];
    double aopt[][] = new double[LEN][WIDTH];

    public static void main(String args[]) {
        subcommon02 sce = new subcommon02();

        sce.f();
        sce.fopt();
        if (sce.eCheck()) {
            System.out.println("Test subcommon02 Passed.");
        } else {
            throw new Error("Test subcommon02 Failed.");
        }
    }


    double nPower(int x, int pwr) {
        return Math.pow(x, pwr); // x**pwr
    }

    // non-optimized version
    void f() {
        for (int x = 0; x < LEN; x++) {
            for (int n = 0; n < WIDTH; n++) {
                if (nPower(x, n) > 10000) {
                    a[x][n] = nPower(x, n);
                    ngrt10000++;
                }
                else if (nPower(x, n) > 1000) {
                    a[x][n] = nPower(x, n);
                    ngrt1000++;
                }
                else if (nPower(x, n) > 100) {
                    a[x][n] = nPower(x, n);
                    ngrt100++;
                }
                else {
                    a[x][n] = nPower(x, n);
                    nsmet100++;
                }
            }
        }
    }

    // hand-optimized version
    void fopt() {
        for (int x = 0; x < LEN; x++) {
            for (int n = 0; n < WIDTH; n++) {
                double tmp = nPower(x, n);

                aopt[x][n] = tmp;
                if (tmp > 10000)
                    ngrtO10000++;
                else if (tmp > 1000)
                    ngrtO1000++;
                else if (tmp > 100)
                    ngrtO100++;
                else
                    nsmetO100++;
            }
        }
    }

    // Compare non-optimized and hand-optimized results
    boolean eCheck() {
        boolean r = true;

        for (int i = 0; i < LEN; i++) {
            for (int j = 0; j < WIDTH; j++) {
              // if (a[i][j] != aopt[i][j]) {
              if (ulpDiff(a[i][j], aopt[i][j]) > 1) {
                    System.out.println("Bad result: a["+i+","+j+"]="+a[i][j]+"; aopt["+i+","+j+"]="+aopt[i][j]);
                    r = false;
                }
            }
        }

        if ((ngrt10000!=ngrtO10000) || (ngrt1000!=ngrtO1000) || (ngrt100!=ngrtO100) || (nsmetO100!=nsmetO100)) {
            System.out.println("Bad result: number of elements found is not matching");
            r = false;
        }
        return r;
    }

    /**
     * Paired-down nextAfter routine
     */
    public  static double nextAfter(double base, double direction) {
        //first check for NaN values
        if (Double.isNaN(base) || Double.isNaN(direction)) {
            // return a NaN dervied from the input NaN(s)
            return base + direction;
        } else if (base == direction) {
            return base;
        } else  {
            long doppelganger;
            double result=0.0;

            doppelganger = Double.doubleToLongBits(base + 0.0);
            if (direction > base) //next greater value
            {
                if (doppelganger >= 0 )
                    result = Double.longBitsToDouble(++doppelganger);
                else
                    result = Double.longBitsToDouble(--doppelganger);
            } else if (direction < base) { // calculate next lesser value
                if (doppelganger > 0)
                    result = Double.longBitsToDouble(--doppelganger);
                else if (doppelganger < 0)
                    result = Double.longBitsToDouble(++doppelganger);
                else
                    /*
                     * doppelganger==0L, result is -MIN_VALUE
                     *
                     * The transition from zero (implicitly
                     * positive) to the smallest negative
                     * signed magnitude value must be done
                     * explicitly.
                     */
                    result = -Double.MIN_VALUE;
            }

            return result;
        }
    }

    /*
     * return ulp of a floating-point value
     */
    static double ulp(double d) {
        d = Math.abs(d);        // don't worry about negative numbers

        if(Double.isNaN(d))
            return Double.NaN;
        else if(Double.isInfinite(d))
            return Double.POSITIVE_INFINITY;
        else {
            // can't represent (Double.MAX_VALUE + ulp) so special case it
            if(d == Double.MAX_VALUE)
                return 1.9958403095347198E292; // 2^971
            else
                return nextAfter(d, Double.POSITIVE_INFINITY) - d;
        }
    }


    /*
     * return signed difference in ulps between two floating-point
     * values.  ulpDiff(NaN, NaN) is zero.
     */
    static double ulpDiff(double ref, double test) {
        double ulp;
        // assume ref is "correct" value

        // Infinity, NaN handling
        if(Double.isInfinite(ref)) {
            if(ref == test)
                return 0.0;
            else
                return Double.POSITIVE_INFINITY;
        } else  if(Double.isNaN(ref)) {
            if(Double.isNaN(test))
                return 0.0;
            else
                return Double.NaN;
        }
        else {
            ulp = ulp(ref);
            // the expression below can overflow
            return (test - ref) / ulp;
        }
    }

}
