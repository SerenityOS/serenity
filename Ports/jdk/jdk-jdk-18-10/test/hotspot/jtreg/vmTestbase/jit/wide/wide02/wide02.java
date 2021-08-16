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
 * @summary converted from VM Testbase jit/wide/wide02.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.wide.wide02.wide02
 */

package jit.wide.wide02;

import nsk.share.TestFailure;

/*
     Check for intermediate results that are too wide.
     The too-wide.java test will fail if the the result of the
     expression (f0+f24) is maintained in greater-than-float precision
     or if the result of the expression (d0+d53) is maintained in
     greater-than-double precision.
*/
public class wide02
{
   static double twoto(int n) {
       double res = 1.0;
       while(n > 0) {
           res *= 2.0;
           n -= 1;
       }
       return res;
   }

   public static void main(String[] arg) {
       float f0 = (float) twoto(0);
       float f24 = (float) twoto(24);
       if(f0 + f24 + f0 == f24)
           System.out.println("Float test PASSES.");
       else
           throw new TestFailure("Float test FAILS");

       double d0 = twoto(0);
       double d53 = twoto(53);
       if(d0 + d53 + d0 == d53)
           System.out.println("Double test PASSES.");
       else
           throw new TestFailure("Double test FAILS");
   }
}
