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
 * @summary converted from VM Testbase jit/FloatingPoint/gen_math/Loops01.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.FloatingPoint.gen_math.Loops01.Loops01
 */

package jit.FloatingPoint.gen_math.Loops01;

import nsk.share.TestFailure;

public class Loops01
{

   static final int N = 500;

   public static void main (String args[])
   {

        double Error = 0.01;

        double xx[][];
        xx = new double[N][N];

        double rn = N;
        double dx = 1/rn;
        double dy = 1/rn;

        double r1, r2, r3, r4, r5;

        Loops01 ll;
        ll = new Loops01();

        for(int i = 0; i < N; i++)
        {       for(int j = 0; j < N; j++)
                {
                        r1 = i * dx;
                        r2 = j * dy;
                        r3 = Math.sqrt(r1 * r1 + r2 * r2);
                        r4 = Math.sin(4 * r1) + Math.cos(4 * r2);
                        r5 = r3 * (2 + r4);
                        xx[i][j] = r5;
                }
        }

        double norma = ll.Norma(N,xx);
        double er = Math.abs(2.5 - norma);
        ll.Echeck(er,Error);

  }

   public double Norma(int nn, double ww[][])
   {
        double nor = 0;
        double r1 = nn;
        double r2 = r1 * r1;
        double r3;

        for(int i = 0; i < nn; i++)
        {
                for(int j = 0; j < nn; j++)
                {       r3 = ww[i][j] * ww[i][j];
                        nor = nor + r3;
                }
        }
        nor = nor/r2;
        return nor;
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
