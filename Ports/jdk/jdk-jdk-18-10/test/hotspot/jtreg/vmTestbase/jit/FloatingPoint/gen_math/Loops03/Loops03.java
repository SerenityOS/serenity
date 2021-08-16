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
 * @key randomness
 *
 * @summary converted from VM Testbase jit/FloatingPoint/gen_math/Loops03.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.FloatingPoint.gen_math.Loops03.Loops03
 */

package jit.FloatingPoint.gen_math.Loops03;

import nsk.share.TestFailure;

public class Loops03
{

   static final int N = 100000;

   public static void main (String args[])
   {


        double x;
        double r1, r2, r3, r4, r5, r6, r7;
        double Error = 0.001;


        Loops03 ll;
        ll = new Loops03();

        int i = 1;
        double sum = 0;
        double prod = 1;
        while(i < N)
        {       r1 = ll.Random_arg(i);
                r3 = Math.sin(r1);
                r4 = Math.cos(r1);
                r5 = r3 * r3 + r4 * r4;
                r6 = i + i;
                r7 = r6 * r6;
                sum = sum + r5/r7;

                r2 = ll.Random_arg(i);
                r3 = Math.sin(r1);
                r4 = Math.cos(r1);
                r5 = r3 * r3 + r4 * r4;
                r6 = i + i;
                r7 = r6 * r6;
                prod = prod * (1 + r5/r7);
                i++;
        }
        double er1 = Math.abs(sum - 0.411);
        double er2 = Math.abs(prod - 1.465);
        double errrr = Math.sqrt(er1 * er1 + er2 * er2);


        ll.Echeck(errrr,Error);

  }

// method below return double random number in interval
//              (0, int nn)

   public double Random_arg(int nn)
   {
        double rr;
        rr = Math.random();
        double rn = nn;
        double ru = rr * rn;
        return ru;

   }

   public void Echeck(double er, double ER)
   {

        if( er < ER)
                System.out.println("test PASS");
        else
        {
                System.out.println("expected error: " + ER);
                System.out.println("   found error: " + er);
                throw new TestFailure("test FAIL");
        }

   }





}
