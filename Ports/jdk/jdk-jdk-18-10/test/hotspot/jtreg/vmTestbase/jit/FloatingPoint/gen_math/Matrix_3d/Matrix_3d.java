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
 * @summary converted from VM Testbase jit/FloatingPoint/gen_math/Matrix_3d.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.FloatingPoint.gen_math.Matrix_3d.Matrix_3d
 */

package jit.FloatingPoint.gen_math.Matrix_3d;

// Test working with 3D matrix.
// There are no data dependences in code.

import nsk.share.TestFailure;

public class Matrix_3d
{

   public static void main (String args[])
   {

        int N = 50;
        double Error = 0.001;
        double r1, r2, r3, r4, r5, r6;

        double xxx[][][];
        double yyy[][][];
        double zzz[][][];

        xxx = new double[N][N][N];
        yyy = new double[N][N][N];
        zzz = new double[N][N][N];

        Matrix_3d mm;
        mm = new Matrix_3d();

        for(int i = 0; i < N; i++)
        {
           for(int j = 0; j < N; j++)
           {
              for(int k = 0; k < N; k++)
              {
                 r1 = i;
                 r2 = Math.sin(r1);
                 r3 = j;
                 r4 = Math.cos(r3);
                 r5 = k;
                 r6 = Math.sqrt(r5);
                 xxx[i][j][k] = r6 * (r2 * r2 + r4 * r4);
              }
           }
        }

        for(int i = 0; i < N; i++)
        {
           for(int j = 0; j < N; j++)
           {
              for(int k = 0; k < N; k++)
              {
                 yyy[i][j][k] = xxx[k][j][i];
                 zzz[i][j][k] = xxx[k][i][j];
              }
           }
        }



        double trace_xxx = mm.trace_matrix(N,xxx);
        double trace_yyy = mm.trace_matrix(N,yyy);
        double trace_zzz = mm.trace_matrix(N,zzz);

        double trace_3d = trace_xxx + trace_yyy + trace_zzz;

        double er = Math.abs(105 - trace_3d);

        if( er < Error)
                System.out.println("test PASS");
        else
                throw new TestFailure("test FAIL");
   }


  public double trace_matrix(int nn, double www[][][])
   {    double trace = 0;
        for(int i = 0; i < nn; i++)
        {
           trace = trace + www[i][i][i] *  www[i][i][i];
        }
        return Math.sqrt(trace);

   }



}
