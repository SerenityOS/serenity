/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.arguments;

/*
 * @test TestNewSizeThreadIncrease
 * @bug 8144527
 * @summary Tests argument processing for NewSizeThreadIncrease
 * @library /test/lib
 * @library /
 * @requires vm.gc.Serial
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver gc.arguments.TestNewSizeThreadIncrease
 */


import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;


// Range of NewSizeThreadIncrease is 0 ~ max_uintx.
// Total of 5 threads will be created (1 GCTest thread and 4 TestThread).
public class TestNewSizeThreadIncrease {
  static final String VALID_VALUE = "2097152"; // 2MB

  // This value will make an overflow of 'thread count * NewSizeThreadIncrease' at DefNewGeneration::compute_new_size().
  // = (max_uintx / 5) + 1, = (18446744073709551615 / 5) + 1
  static String INVALID_VALUE_1 = "3689348814741910324";

  // This string is contained when compute_new_size() expands or shrinks.
  static final String LOG_NEWSIZE_CHANGED = "New generation size ";

  public static void main(String[] args) throws Exception {
    if (Platform.is32bit()) {
      // (max_uintx / 5) + 1, 4294967295/5 + 1
      INVALID_VALUE_1 = "858993460";
    }

    // New size will be applied as NewSizeThreadIncrease is small enough to expand.
    runNewSizeThreadIncreaseTest(VALID_VALUE, true);

    // New size will be ignored as 'thread count * NewSizeThreadIncrease' overflows.
    runNewSizeThreadIncreaseTest(INVALID_VALUE_1, false);
  }

  static void runNewSizeThreadIncreaseTest(String expectedValue, boolean isNewsizeChanged) throws Exception {
    ProcessBuilder pb = GCArguments.createJavaProcessBuilder("-XX:+UseSerialGC",
                                                             "-Xms96M",
                                                             "-Xmx128M",
                                                             "-XX:NewRatio=2",
                                                             "-Xlog:gc+heap+ergo=debug",
                                                             "-XX:NewSizeThreadIncrease="+expectedValue,
                                                             GCTest.class.getName());
    OutputAnalyzer output = new OutputAnalyzer(pb.start());

    output.shouldHaveExitValue(0);

    if (isNewsizeChanged) {
      output.shouldContain(LOG_NEWSIZE_CHANGED);
    } else {
      output.shouldNotContain(LOG_NEWSIZE_CHANGED);
    }
  }

  static class GCTest {

    static final int MAX_THREADS_COUNT = 4;
    static TestThread threads[] = new TestThread[MAX_THREADS_COUNT];

    public static void main(String[] args) {

      System.out.println("Creating garbage");

      for (int i=0; i<MAX_THREADS_COUNT; i++) {
        threads[i] = new TestThread();
        threads[i].start();
      }

      System.gc();

      for (int i=0; i<MAX_THREADS_COUNT; i++) {
        threads[i].stopRunning();
      }

      System.out.println("Done");
    }

    private static class TestThread extends Thread {

      volatile boolean isRunning = true;

      public void run() {
        while (isRunning == true) {
          try {
            Thread.sleep(10);
          } catch (Throwable t) {
          }
        }
      }

      public void stopRunning() {
        isRunning = false;
      }
    }
  }
}
