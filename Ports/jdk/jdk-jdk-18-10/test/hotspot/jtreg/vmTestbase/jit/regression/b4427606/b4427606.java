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
 * @bug 4427606
 *
 * @summary converted from VM Testbase jit/regression/b4427606.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.regression.b4427606.b4427606
 */

package jit.regression.b4427606;

import nsk.share.TestFailure;

/*
 *  This is a reproducible case for a few bugs reported in merlin.
 *  mainly: 4427606
 */
public class b4427606 {
  private static final int NUM_ITERATIONS = 1000000;

  public static void main(String[] args) {
    new b4427606().run();
  }

  public void run() {
    new DivByZero().start();
    new NeverDivByZero().start();
  }

  class DivByZero extends Thread {
    public void run() {
      long source = 1L;
      long iter = NUM_ITERATIONS;
      while (--iter > 0) {
        try {
          long ignore = source % zero;
          throw new RuntimeException("Should Not Reach Here....");
        } catch (java.lang.ArithmeticException ax) {
        } catch (RuntimeException rx) {
          rx.printStackTrace();
          throw new TestFailure("Test failed.");
        }
      }
    }
  }

  class NeverDivByZero extends Thread {
    public void run() {
      long source = 1L;
      long iter = NUM_ITERATIONS;
      while (--iter > 0) {
        try {
         long ignore = source % notzero;
        } catch (java.lang.ArithmeticException ax) {
          ax.printStackTrace();
          throw new TestFailure("Test failed.");
        }
      }
    }
  }
  long zero = 0;
  long notzero = 10;
}
