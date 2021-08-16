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
 * @summary converted from VM Testbase jit/FloatingPoint/gen_math/Loops04.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.FloatingPoint.gen_math.Loops04.Loops04
 */

package jit.FloatingPoint.gen_math.Loops04;

// Test working with nested loops.
import nsk.share.TestFailure;

public class Loops04
{

   public static void main (String args[])
   {

        int N = 25;

        double Error = 0.01;

        double xx[];
        double yy[];
        double zz[];

        xx = new double[N];
        yy = new double[N];
        zz = new double[N];

        Loops04 ll;
        ll = new Loops04();

        for(int i = 0; i < N; i++)
        {
                xx[i] = i;
                yy[i] = i + 1;
                zz[i] = Math.max(xx[i],yy[i]);
        }

        double x_max = 0; double x_min = 0;
        double y_max = 0; double y_min = 0;
        double z_max = 0; double z_min = 0;

        for(int i = 1; i< N - 1; i++)
        {
           xx[i] = 1;
           for(int j = 1; j < N - 1; j++)
           {
                yy[j] = 2;
                for(int k = 1; k< N - 1; k++)
                {
                   zz[k] = 3;
                   for(int n = 1; n < N - 1; n++)
                   {
                        xx[i] = 0.5 * (yy[i - 1] + yy[i + 1]);
                        yy[j] = 0.5 * (zz[j - 1] + zz[j + 1]);
                        zz[k] = 0.5 * (xx[k - 1] + xx[k + 1]);

                        x_max = Math.max(x_max, xx[i]);
                        x_min = Math.min(x_min, xx[i]);
                        y_max = Math.max(y_max, yy[j]);
                        y_min = Math.min(y_min, yy[j]);
                        z_max = Math.max(z_max, zz[k]);
                        z_min = Math.min(z_min, zz[k]);

                   }
                }
           }
        }

        double xy_max = Math.max(x_max,y_max);
        double xy_min = Math.min(x_min,y_min);

        double xy_MaxMin = xy_max - xy_min;

        System.out.println(xy_MaxMin);

        double er = Math.abs(xy_MaxMin - 23.6407);

        ll.Echeck(er,Error);

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
