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
 * @summary converted from VM Testbase jit/FloatingPoint/gen_math/Filtering.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.FloatingPoint.gen_math.Filtering.Filtering
 */

package jit.FloatingPoint.gen_math.Filtering;

import nsk.share.TestFailure;

public class Filtering
{
   static int N = 1000;
   static double xx[];
   static double yy[];

   public static void main (String args[])
   {

        double Error = 0.0001;

        xx = new double[N];
        yy = new double[N];


        double r1, r2, r3, r4;

        double rn = N;
        double dx = 1/rn;
        double A = 0.5;
        double B = 0.75;

        for(int i = 0; i < N; i++)
        {       r1 = i * dx;
                xx[i] = A * Math.sin(3 * r1) + B * Math.cos(3 * r1);
        }

        Filtering ff;
        ff = new Filtering();

        double normaX = ff.Norma1(N,xx);

        ff.Filter1(N);
        double norma1 = ff.Norma1(N,yy);

        ff.Filter2(N);
        double norma2 = ff.Norma1(N,yy);

        ff.Filter3(N);
        double norma3 = ff.Norma1(N,yy);

        r4 = (norma1 * norma1 + norma2 * norma2 + norma3 * norma3) / 3 ;
        r4 = Math.sqrt(r4);
        double errrr = Math.abs(r4 - normaX);

        ff.Echeck(errrr,Error);

  }

   public double Norma1(int nn, double uu[])
   {
        double nor = 0;
        double r1 = nn;
        double r2;
        for(int i = 0; i < nn; i++)
        {       r2 = uu[i] * uu[i];
                nor = nor + r2;
        }
        nor = nor / r1;
        return nor;
   }


   public void Filter1 (int nn)
   {    yy[0] = xx[0];
        yy[nn - 1] = xx[nn - 1];
        for(int i = 1; i < nn - 1; i++)
        {
                yy[i] = 0.5 * (xx[i - 1] + xx[i + 1]);
        }
   }


   public void Filter2 (int nn)
   {    yy[0] = xx[0];
        yy[nn - 1] = xx[nn - 1];
        for(int i = 1; i < nn - 1; i++)
        {
                yy[i] = 0.25 * (xx[i - 1] + 2 * xx[i] + xx[i + 1]);
        }

   }

   public void Filter3 (int nn)
   {    yy[0] = xx[0];
        yy[nn - 1] = xx[nn - 1];
        yy[1] = 0.5 * (xx[0] + xx[2]);
        yy[nn - 2] = 0.5 * (xx[nn - 1] + xx[nn - 3]);
        for(int i = 2; i < nn - 2; i++)
        {
                yy[i] = 0.1 * (xx[i - 2] + 2 * xx[i - 1] + 4 * xx[i] +
                        2 * xx[i + 1] + xx[i + 2]);
        }

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
