/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase jit/FloatingPoint/gen_math/Summ.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.FloatingPoint.gen_math.Summ.Summ
 */

package jit.FloatingPoint.gen_math.Summ;

// Test on sums/series

import nsk.share.TestFailure;

public class Summ
{
   public static void main (String args[])
   {
        double Error = 1.3488e-06;
        double A1 = 0.5;
        double Decr = 0.5;
        int NN = 20;
        Summ ss;
        ss = new Summ();
        double sum_plus = ss.sum1(NN, Decr);
        double sum_minus = ss.sum1(NN, -Decr);
        double er_plus = 1.0 - sum_plus;
        double er_minus = 1.0 - 3.0 * sum_minus;
        double er = er_plus * er_plus + er_minus * er_minus;
        double er_total = Math.sqrt(er);

        if ( er_total < Error )
                System.out.println("test PASS");
        else
            {
                System.out.println("expected error - 1.3488e-06");
                System.out.println("found - " + er_total);
                throw new TestFailure("test FAIL");
            }
    }


        public double sum1(int nn, double decr)
        {
           double An = 0.5;
           double sum = 0.0;
           for(int i = 1; i<=nn; i++)
           {  sum = sum + An;
              An = An * decr;
           }
           return sum;
        }


}
