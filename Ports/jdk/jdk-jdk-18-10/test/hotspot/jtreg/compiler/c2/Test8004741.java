/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8004741
 * @summary Missing compiled exception handle table entry for multidimensional array allocation
 *
 * @requires !vm.graal.enabled
 * @library /test/lib
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xmx128m -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *    -XX:-TieredCompilation -XX:+StressCompiledExceptionHandlers
 *    -XX:+SafepointALot -XX:GuaranteedSafepointInterval=100
 *    -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *    compiler.c2.Test8004741
 *
 * @run main/othervm -Xmx128m -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *    -XX:-TieredCompilation -XX:+StressCompiledExceptionHandlers
 *    -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *    compiler.c2.Test8004741
 */

package compiler.c2;

import sun.hotspot.WhiteBox;

public class Test8004741 extends Thread {

  static int passed = 0;

  /**
   * Loop forever allocating 2-d arrays.
   * Catches and rethrows all exceptions; in the case of ThreadDeath, increments passed.
   * Note that passed is incremented here because this is the exception handler with
   * the smallest scope; we only want to declare success in the case where it is highly
   * likely that the test condition
   * (exception in 2-d array alloc interrupted by ThreadDeath)
   * actually occurs.
   */
  static int[][] test(int a, int b) throws Exception {
    int[][] ar;
    try {
      ar = new int[a][b];
    } catch (ThreadDeath e) {
      System.out.println("test got ThreadDeath");
      passed++;
      throw e;
    }
    return ar;
  }

  /* Cookbook wait-notify to track progress of test thread. */
  Object progressLock = new Object();
  private static final int NOT_STARTED = 0;
  private static final int RUNNING = 1;
  private static final int STOPPING = 2;

  int progressState = NOT_STARTED;

  void toState(int state) {
    synchronized (progressLock) {
      progressState = state;
      progressLock.notify();
    }
  }

  void waitFor(int state) {
    synchronized (progressLock) {
      while (progressState < state) {
        try {
          progressLock.wait();
        } catch (InterruptedException e) {
          throw new Error("unexpected InterruptedException", e);
        }
      }
      if (progressState > state) {
        throw new Error("unexpected test state change, state = " + state + ", progressState = " + progressState);
      }
    }
  }

  /**
   * Loops running test until some sort of an exception or error,
   * expects to see ThreadDeath.
   */
  public void run() {
    try {
      // Print before state change, so that other thread is most likely
      // to see this thread executing calls to test() in a loop.
      System.out.println("thread running");
      toState(RUNNING);
      while (true) {
        // (2,2) (2,10) (2,100) were observed to tickle the bug;
        test(2, 100);
      }
    } catch (ThreadDeath e) {
      // nothing to say, passing was incremented by the test.
    } catch (Throwable e) {
      throw new Error("unexpected Throwable " + e, e);
    }
    toState(STOPPING);
  }

  /**
   * Runs a single trial of the test in a thread.
   * No single trial is definitive, since the ThreadDeath
   * exception might not land in the tested region of code.
   */
  public static void threadTest() throws InterruptedException {
    Test8004741 t = new Test8004741();
    t.start();
    t.waitFor(RUNNING);
    Thread.sleep(100);
    System.out.println("stopping thread");
    t.stop();
    t.waitFor(STOPPING);
    t.join();
  }

  public static void main(String[] args) throws Exception {
    // Warm up "test"
    // t will never be started.
    for (int n = 0; n < 11000; n++) {
      test(2, 100);
    }

    var method = Test8004741.class.getDeclaredMethod("test", int.class, int.class);
    if (!WhiteBox.getWhiteBox().isMethodCompiled(method)) {
        throw new Error("test method didn't get compiled");
    }

    try {
      test(-1, 100);
      throw new AssertionError("Missing NegativeArraySizeException");
    } catch (NegativeArraySizeException e) {
      System.out.println("Saw expected NegativeArraySizeException #1");
    }

    try {
      test(100, -1);
      throw new AssertionError("Missing NegativeArraySizeException");
    } catch (NegativeArraySizeException e) {
      System.out.println("Saw expected NegativeArraySizeException #2");
    }

    /* Test repetitions.  If the test succeeds-mostly, it succeeds,
     * as long as it does not crash (the outcome if the exception range
     * table entry for the array allocation is missing).
     */
    passed = 0;
    int limit = 6;
    while (passed != limit) {
      threadTest();
    }
  }
}
