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
 * @summary converted from VM Testbase jit/FloatingPoint/gen_math/Loops05.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.FloatingPoint.gen_math.Loops05.Loops05
 */

package jit.FloatingPoint.gen_math.Loops05;

import nsk.share.TestFailure;

public class Loops05
{

   static final int N = 100;
   static final double pi = 3.14;

   public static void main (String args[])
   {

        double Error = 0.01;
        double xx[][];
        xx = new double[N][N];

        double x_norma_n, x_norma_0;
        double t = 0;
        double dt = 0.01;
        double tn = 1;

        double r1, r2, r3, r4, r5;


        Loops05 ll;
        ll = new Loops05();


        for(int i = 0; i < N; i++)
        {       for(int j = 0; j < N; j++)
                {
                        r1 = i * i + j * j;
                        r2 = N;
                        r3 = r1/N;
                        r4 = r3/N;
                        xx[i][j] = r4;
                }
        }

        x_norma_0 = ll.Norma(N,xx);
        while(t < tn)
        {
                for(int i = 0; i < N; i++)
                {
                        for(int j = 0; j < N; j++)
                        {
                                double fff = ll.F_function(t, 0.2, 0.6, 10);
                                xx[i][j]= xx[i][j] + fff * dt;
                        }
                }
                t = t + dt;
        }
        x_norma_n = ll.Norma(N,xx);
        double errrr = Math.abs(x_norma_0 - x_norma_n);
        ll.Echeck(errrr,Error);

  }

   public double F_function(double tt, double t1, double t2, double magn)
   {    double fff;
        double r1;

        r1 = Math.sin(2 * pi * tt / (t2 - t1));
        if(tt > t1 && tt < t2) fff = r1 * magn;
        else fff = 0;

        return fff;
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
