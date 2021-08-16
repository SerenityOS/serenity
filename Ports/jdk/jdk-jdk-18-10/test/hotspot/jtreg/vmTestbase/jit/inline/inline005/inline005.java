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
 * @summary converted from VM Testbase jit/inline/inline005.
 * VM Testbase keywords: [jit, quick]
 * VM Testbase readme:
 * Bug 4079776.  Make inlining of sqrt and abs work with register allocation.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.inline.inline005.inline005
 */

package jit.inline.inline005;

import nsk.share.TestFailure;

public class inline005 {

  private static double fun (double x)
  {
    double t1 = Math.sqrt(x);
    return t1*Math.exp(x);
  }

/***************************************************************/

  private static double fun_correct (double x)
  {
    return Math.sqrt(x)*Math.exp(x);
  }

/***************************************************************/

  public static void main(String[] argv) {
    double x = 31.5;
    if (fun(x) != fun_correct(x)) {
       System.out.println(" Error: result = "+fun(x));
       System.out.println("       must be = "+fun_correct(x));
       throw new TestFailure("TEST FAILED");
    }
    else {
       System.out.println("TEST PASSED");
    }
  }
}
