/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase runtime/jbe/subcommon/subcommon01.
 * VM Testbase keywords: [quick, runtime]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.jbe.subcommon.subcommon01.subcommon01
 */

package vm.compiler.jbe.subcommon.subcommon01;

/* -- Common subexpression elimination testing
Using both global and local common subexpressions in methods b5()
and b6() below to test common subexpression.
 */

import java.io.PrintStream;
import java.lang.Exception;

public class subcommon01 {
  static final int LEN = 500;

  public static void main(String args[]) {
    double a[]  = new double[LEN];
    double ao[] = new double[LEN];

    Preopt preOptRun = new Preopt();
    initAr(a);
    preOptRun.b1(a);
    preOptRun.b2(a);
    preOptRun.b3(a);
    preOptRun.b5(a);
    preOptRun.b2(a);
    preOptRun.b6(a);

    Opt optRun = new Opt();
    initAr(ao);
    optRun.b1(ao);
    optRun.b2(ao);
    optRun.b3(ao);
    optRun.b5(ao);
    optRun.b2(ao);
    optRun.b6(ao);

    eCheck(a, ao);
  }

  static int getLen() {
    return LEN;
  }

  static void initAr(double arr[]) {
    double r1, r2, r3;
    for (int r=0; r<LEN; r++) {
      r1 = LEN/(r+1);
      r2 = Math.sqrt(r1*r1);
      r3 = Math.sin(r2);
      arr[r] = r2/r3;
    }
  }

  static void pt(double arr[]) {
    System.out.println();
    for (int k=0; k<10; k++)
      System.out.print("a["+k+"]="+arr[k]);
  }

  static void eCheck(double b1[], double b2[]) {
    for (int k = 0; k < LEN; k++) {
      if (b1[k] != b2[k]) {
        System.out.println("eCheck fails in a["+k+"]");
        System.out.println("a ["+k+"]="+b1[k]);
        System.out.println("ao["+k+"]="+b2[k]);
        System.out.println("Test subcommon01 Failed.");
        throw new Error("Test subcommon01 Failed: eCheck fails in a[" + k + "]. a[" + k + "]=" + b1[k] + " != ao[" + k + "]=" + b2[k]);
      }
    }
    System.out.println("Test subcommon01 Passed.");
  }
}

class A {
  int m = 81;
  int n = 1;
  int i = 0;
  int j = 0;
  double v;
  double x = 1;
  double t3 = 0;
  double t5, t9, t14;
  int t1, t2, t4;
  int t6, t7, t8, t10;
  int t11, t12, t13, t15;

  void b1(double arr[]) {
    i = m-1;
    j = n;
    t1 = 4*n;
    try {
      v = arr[t1];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      System.err.println("Bad input to subcommon01.b1. " +
                         "Expected a smaller number than " + subcommon01.getLen() +
                         ", got: ->" + t1 + "<-");
      System.out.println( e );
      System.exit(1);
    }
    //    System.out.println("Finish b1");
  }

  void b2(double arr[]) {
      t2 = 4*(++i);
      try {
        t3 = arr[t2];
      }
      catch (ArrayIndexOutOfBoundsException e) {
        System.err.println("Bad input to subcommon01.b2. " +
                           "Expected a smaller number than " + subcommon01.getLen() +
                           ", got: ->" + t2 + "<-");
        System.out.println( e );
        System.exit(1);
      }
      //    System.out.println("Finish b2");
  }


  void b3(double arr[]) {
      t4 = 4*(--j);
      try {
        t5 = arr[t4];
      }
      catch (ArrayIndexOutOfBoundsException e) {
        System.err.println("Bad input to subcommon01.b3. " +
                           "Expected a smaller number than " + subcommon01.getLen() +
                           ", got: ->" + t4 + "<-");
        System.out.println( e );
        System.exit(1);
      }
      //    System.out.println("Finish b3");
  }
}


class Preopt extends A {
  // Pre-optimized code
  void b5(double arr[]) {
    t6 = 4*i;
    try {
      x = arr[t6];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      System.err.println("Bad input to subcommon01.b5. " +
                         "Expected a smaller number than " + subcommon01.getLen() +
                         ", got: ->" + t6 + "<-");
      System.out.println( e );
      System.exit(1);
    }
    t7 = 4*i;
    t8 = 4*j;
    try {
      t9 = arr[t8];
      arr[t7] = t9;
      t10 = 4*j;
      arr[t10] = x;
    }
    catch (ArrayIndexOutOfBoundsException e) {
      System.out.println( e );
      System.exit(1);
    }
    //    System.out.println("Finish b5");
  }

  void b6(double arr[]) {
    t11 = 4*i;
    t12 = 4*i;
    t13 = 4*n;
    try {
      x = arr[t11];
      t14 = arr[t13];
      arr[t12] = t14;
      t15 = 4*n;
      arr[t15] = x;
    }
    catch (ArrayIndexOutOfBoundsException e) {
      System.out.println( e );
      System.exit(1);
    }
    //    System.out.println("Finish b6");
  }
}

class Opt extends A {
  // The result of eliminating both global and local common subexpressions
  // from methods b5() and b6() resides in methods bopt5() and bopt6() respectively.
  void b5(double arropt[]) {
    x = t3;
    try {
      arropt[t2] = t5;
      arropt[t4] = x;
    }
    catch (ArrayIndexOutOfBoundsException e) {
      System.out.println( e );
      System.exit(1);
    }
    //    System.out.println("Finish b5");
  }

  void b6(double arropt[]) {
    x = t3;
    try {
      t14 = arropt[t1];
      arropt[t2] = t14;
      arropt[t1] = x;
    }
    catch (ArrayIndexOutOfBoundsException e) {
      System.out.println( e );
      System.exit(1);
    }
    //    System.out.println("Finish b6");
  }
}
