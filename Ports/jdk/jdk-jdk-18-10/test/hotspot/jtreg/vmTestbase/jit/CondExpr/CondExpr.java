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
// test the conditional expressions


/*
 * @test
 *
 * @summary converted from VM Testbase jit/CondExpr.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.CondExpr.CondExpr
 */

package jit.CondExpr;

import nsk.share.TestFailure;

public class CondExpr {

  public static void trace (String s, int res) {
    System.out.println("test result for " + s + " is " + res);
  }

  public static int test_int1(int arg) { return (arg==10) ? 1 : 2;  }

  public static int test_int(int arg) { return test_int1(arg) + test_int1(arg+1); }

  public static long test_long1(long arg) { return (arg==10) ? 1l : 2l;  }

  public static int test_long(long arg) { return (int)(test_long1(arg) + test_long1(arg+1)); }

  public static float test_float1(float arg) { return (arg==10.0f) ? 1.0f : 2.0f;  }

  public static int test_float(float arg) { return (int)(test_float1(arg) + test_float1(arg+1)); }

  public static double test_double1(double arg) { return (arg==10.0) ? 1.0 : 2.0;  }

  public static int test_double(double arg) { return (int)(test_double1(arg) + test_double1(arg+1)); }


  public static int nested_test_int1(int arg) {
    return (arg>1) ? ((arg==10) ? 1 : 2) : ((arg==-10) ? 3: 4);
  }



  public static int nested_test_int (int arg) {
    return (nested_test_int1 (arg) + nested_test_int1 (arg+1) + nested_test_int1 (-arg) + nested_test_int1 (-arg-1)); }

  public static void main(String s[]) {
    System.out.println ("Testing conditional expressions (srm 10/22)");
    boolean correct = true;
    int res = 0;
    res = test_int(10);        trace("test_int", res);
    correct = correct & ( res == 3);
    res = test_long(10l);      trace("test_long", res);
    correct = correct & ( res == 3);
    res = test_float(10.0f);   trace("test_float", res);
    correct = correct & ( res == 3);
    res = test_double(10.0);   trace("test_double", res);
    correct = correct & ( res == 3);

    res = nested_test_int(10); trace("nested_test_int", res);
     correct = correct & ( res == 10);

    if (correct) System.out.println("Correct!");
    else throw new TestFailure("ERRROR in conditional expressions");
  }
}
