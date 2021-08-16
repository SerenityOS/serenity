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
 * @summary converted from VM Testbase jit/Arrays/ArrayBounds.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.Arrays.ArrayBounds.ArrayBounds
 */

package jit.Arrays.ArrayBounds;

/*
SCCS ID : @(#)ArrayBounds.java  1.2 02/07/16
*/
/* The intent of this Java program is to expose Virtual Machines that
   make illegal array bounds check removal optimizations. */

/* There are a variety of potential semantic problems with array bounds
   checking.

   One source of potential bounds-check problems is a compiler that
   "hoist"s bounds checks outside of loops.  This may either either
   because an array access is loop invariant, or because the compiler
   is clever enough to determine a loop-invariant "sufficient
   condition" that guarantees the safety of the safety of one or more
   accesses within the loop.

   While this is a good approach, it has a variety of possible
   pitfalls:

     1) If a hoisted test fails, you can't just raise a bounds-check
        exception when the test fails; the loop may have had global
        side effects before the exception was supposed to have been
        raised.  [prematureExceptionTest]

     2) A hoisted test may itself generate an exception (such as a
        null pointer exception).  Again, this must not be reflected
        prematurely if the loop has global side effects that should be
        observed.

     3) An implementation might fail to be conservative enough about
        the possible side effects of a method call within the loop.
        For example, when an array being accessed within a loop is
        accessed as a static or instance variable, then any call must
        be assumed to possibly modify the array variable itself (in
        the absence of pretty clever proof techniques!)  So no hoisted
        predicate mentioning the array length (as most will) can be
        loop-invariant.

     4) In some implementations, the code generated for a bounds-check
        implicitly does a null check as well.  For example, it may
        access an array length field in the object header.  On systems
        where the 0-th page is protected, this might ensure an OS
        signal for a null array pointer.  But if a bounds check is
        elided, the generated code might only access the null pointer
        at a large offset, beyond the protected page, and fail to
        produce a null pointer exception.

     5) Multi-dimensional arrays are annoying for bounds-check removal.
        If a loop over "i" accesses "a[i][j]" (where "j" is invariant in
        the loop, then "a[i]" is not loop-invariant; it changes every
        iteration.  Even if a two-dimensional array were "rectangular"
        when a loop begins, nothing guarantees that another thread
        won't update some "a[k]" during the loop.  If the compiler
        hoisted some bounds check comparing "j" to the presumed length
        of all the "a[k]", and a shorter array were written to some
        "a[k]" during the loop, then we might miss a necessary array
        bounds exception.
*/

import nsk.share.TestFailure;


public class ArrayBounds {
  private static int global;

  private static int[] aplIota(int n) {
    int[] a = new int[n];
    for (int j = 0; j < n; j++) a[j] = j;
    return a;
  }

  private static int prematureExceptionWork(int[] a, int n) {
    global = 0;
    int sum = 0;
    try {
      for (int i = 0; i < n; i++) {
        global++; sum += a[i];
      }
    } catch (ArrayIndexOutOfBoundsException t) {}
    return sum;
  }
  private static void prematureException() {
    int[] a = aplIota(10);
    int sum = prematureExceptionWork(a, 11);
    if (global != 11 || sum != 45) {
      throw new TestFailure("Premature exception test failed.");
    }
  }

  private static class Foo {
    int[] a;
  }

  private static int exceptionInHoistedPredWork(Foo f, int n) {
    global = 0;
    int sum = 0;
    try {
      for (int i = 0; i < n; i++) {
        global++; sum += f.a[i];
      }
    } catch (NullPointerException t) {}
    return sum;
  }
  private static void exceptionInHoistedPred() {
    int sum = exceptionInHoistedPredWork(null, 10);
    if (global != 1 || sum != 0) {
      throw new TestFailure("Premature exception test failed.");
    }
  }

  private static void changeLength(Foo f, int n) {
    int[] a = aplIota(n);
    f.a = a;
  }
  private static int arraySideEffectWork(Foo f, int n) {
    int sum = 0;
    try {
      for (int i = 0; i < n; i++) {
        sum += f.a[i];
        if (i == 0) changeLength(f, 5);
      }
    } catch (ArrayIndexOutOfBoundsException t) {}
    return sum;
  }
  private static void arraySideEffect() {
    int[] a = aplIota(10);
    Foo f = new Foo(); f.a = a;
    int sum = arraySideEffectWork(f, 10);
    if (sum != 10) {
      throw new TestFailure("Array side effect test failed (" + sum + ")");
    }
  }

  private static boolean nullArrayWork(int[] a, int n) {
    int sum = 0;
    global = 0;
    boolean x = false;
    try {
      for (int i = 0; i < n; i++) {
        global++; sum += a[i];
      }
    } catch (NullPointerException t) {
      x = true;
    }
    return x;
  }
  private static void nullArray() {
     /* 30000 should be larger than most pages sizes! */
    if (!nullArrayWork(null, 30000) || global != 1) {
      throw new TestFailure("nullArray test failed.");
    }
  }

  private static int[][] aa = new int[10][20];
  static {
    for (int i = 0; i < 10; i++) aa[i] = aplIota(20);
  }
  private static class ArrayMutator extends Thread {
    int[][] aa; int newN;
    ArrayMutator(int[][] aa, int newN) {
      super();
      this.aa = aa; this.newN = newN;
    }
    public void run() {
      aa[1] = aplIota(newN);
    }
  }

  private static int array2DWork(int[][] aa, int m, int n) {
    int sum = 0;
    global = 0;
    try {
      for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
          global++; sum += aa[i][j];
          if (i == 0 && j == 0) {
            Thread t = new ArrayMutator(aa, n/2);
            try {
              t.start();
              t.join();
            } catch (InterruptedException x) {}
          }
        }
      }
    } catch (ArrayIndexOutOfBoundsException t) {}
    return sum;
  }
  private static void array2D() {
    int sum = array2DWork(aa, aa.length, aa[0].length);
    if (sum != 19*20/2 + 9*10/2 || global != 20 + 10 + 1) {
      throw new TestFailure("array2D test failed (sum = " + sum +
                         "; global = " + global + ")");
    }
  }

  public static void main(String[] args) {
    exceptionInHoistedPred();
    prematureException();
    arraySideEffect();
    nullArray();
    array2D();
  }
}
