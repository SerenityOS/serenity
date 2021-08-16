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
 * @summary converted from VM Testbase jit/wide/wide01.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.wide.wide01.wide01
 */

package jit.wide.wide01;

import nsk.share.TestFailure;

/*
     Check for intermediate results that are too wide.
     The wide.java test will fail if the the result of the
     expression (f0+f24) is maintained in greater-than-double precision
     or if the result of the expression (d0+d53) is maintained in
     greater-than-double precision.
*/

strictfp public class wide01
{
   public static void main(String[] arg) {
       float  f1 = Float.MAX_VALUE;
       float  f2 = Float.MAX_VALUE;
       double d1 = Double.MAX_VALUE;
       double d2 = Double.MAX_VALUE;

       float f = f1 * f2;
       if(f == Float.POSITIVE_INFINITY)
           System.out.println("Float test PASSES.");
       else
           throw new TestFailure("Float test FAILS");

       double d = d1 * d2;
       if(d == Double.POSITIVE_INFINITY)
           System.out.println("Double test PASSES.");
       else
           throw new TestFailure("Double test FAILS");

   }
}
