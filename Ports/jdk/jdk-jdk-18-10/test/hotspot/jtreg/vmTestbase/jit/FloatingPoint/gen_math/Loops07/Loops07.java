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
 * @summary converted from VM Testbase jit/FloatingPoint/gen_math/Loops07.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.FloatingPoint.gen_math.Loops07.Loops07
 */

package jit.FloatingPoint.gen_math.Loops07;

// Test working with nested loops.
// Code is subject to different types of optimization
// and parallelization - empty loops, dead code, etc.

import nsk.share.TestFailure;

public class Loops07
{

   public static void main (String args[])
   {

        int N = 8;
        double Error = 1;
        double r1, r2, r3, r4, r5, r6;

        double xx[][];
        double z1, z2, z3;

        xx = new double[N][N];

        Loops07 ll;
        ll = new Loops07();

        for(int i = 0; i < N; i++)
        {
           for(int j = 0; j < N; j++)
           {
                r1 = i * i + j * j;
                r2 = Math.sqrt(r1);
                r3 = 1 / r2 ;
                r4 = i + j ;
                r5 = r4 / r3 ;

                xx[i][j] = r5;
           }
        }

// main loop ---------------------------
        z1 = 0;
        for(int m1 = 0; m1 < N; m1++)
        {
           for(int m2 = 0; m2 < N; m2++)
           {
              z1 = z1 - 0.5 * xx[m1][m2];
              for(int m3 = 0; m3 < N; m3++)
              {
                 for(int m4 = 0; m4 < N; m4++)
                 {
                    z1 = z1 + xx[m3][m4];

                    for(int m5 = 0; m5 < N; m5++)
                    {
                       for(int m6 = 0; m6 < N; m6++)
                       {
                          for(int m7 = 0; m7 < N; m7++)
                          {
                                z2 = Math.abs(1 + Math.sin(2 + Math.cos(3 +
                                     Math.sqrt(4 - Math.cos(5)))));

                          }
                       }
                    }
                 }
              }
           }
        }

// ---------------------------------------
        double er = Math.abs(184401 - z1);

        if( er < Error)
                System.out.println("test PASS");
        else
                throw new TestFailure("test FAIL");

   }





}
