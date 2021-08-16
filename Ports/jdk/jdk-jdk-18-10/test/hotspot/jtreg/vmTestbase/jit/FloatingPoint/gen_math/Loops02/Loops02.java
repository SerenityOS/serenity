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
 * @summary converted from VM Testbase jit/FloatingPoint/gen_math/Loops02.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.FloatingPoint.gen_math.Loops02.Loops02
 */

package jit.FloatingPoint.gen_math.Loops02;

// Test working with  loops and random functions.
import nsk.share.TestFailure;

public class Loops02
{

   static final int N = 300;

   public static void main (String args[])
   {


        double x;
        double r1, r2, r3;

        double rn = N;
        double dx = 1/rn;
        double Min_count = rn/2;

        Loops02 ll;
        ll = new Loops02();

        x = 0.5;
        int count = 0;
        do
        {
                r1 = Math.random();
                r2 = Math.random();
                x = x + dx * (r1 - r2);
                count++;

        } while(x > 0 && x < 1);

        ll.Echeck(Min_count,count);

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
