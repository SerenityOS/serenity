/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2019, Google and/or its affiliates. All rights reserved.
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

package MyPackage;

/**
 * @test
 * @build Frame HeapMonitor
 * @summary Verifies the JVMTI Heap Monitor interval when allocating arrays.
 * @requires vm.jvmti
 * @compile HeapMonitorStatArrayCorrectnessTest.java
 * @run main/othervm/native -agentlib:HeapMonitorTest MyPackage.HeapMonitorStatArrayCorrectnessTest
 */

public class HeapMonitorStatArrayCorrectnessTest {

  private static final int maxCount = 10;
  // Do 200000 iterations and expect maxIteration / multiplier samples.
  private static final int maxIteration = 200_000;
  private static int array[];

  // 15% error ensures a sanity test without becoming flaky.
  // Flakiness is due to the fact that this test is dependent on the sampling interval, which is a
  // statistical geometric variable around the sampling interval. This means that the test could be
  // unlucky and not achieve the mean average fast enough for the test case.
  private static final int acceptedErrorPercentage = 15;

  private static void allocate(int size) {
    for (int j = 0; j < maxIteration; j++) {
      array = new int[size];
    }
  }

  public static void main(String[] args) {
    int sizes[] = {1000, 10000, 100000};
    double expected = 0;
    int count = 0;

    for (int currentSize : sizes) {
      System.out.println("Testing size " + currentSize);

      HeapMonitor.resetEventStorage();
      if (!HeapMonitor.eventStorageIsEmpty()) {
        throw new RuntimeException("Should not have any events stored yet.");
      }

      for (count = 1; count < maxCount; count++) {
        // 111 is as good a number as any.
        final int samplingMultiplier = 111;
        HeapMonitor.setSamplingInterval(samplingMultiplier * currentSize);

        HeapMonitor.enableSamplingEvents();

        allocate(currentSize);

        HeapMonitor.disableSamplingEvents();

        // For simplifications, we ignore the array memory usage for array internals (with the array
        // sizes requested, it should be a negligible oversight).
        //
        // That means that with maxIterations, the loop in the method allocate requests:
        //    maxIterations * currentSize * 4 bytes (4 for integers)
        //
        // Via the enable sampling, the code requests a sample every samplingMultiplier * currentSize bytes.
        //
        // Therefore, the expected sample number is:
        //   count * (maxIterations * currentSize * 4) / (samplingMultiplier * currentSize);
        //   (count because we can do this multiple times in order to converge).
        expected = maxIteration * count;
        expected *= 4;
        expected /= samplingMultiplier;

        if (HeapMonitor.statsHaveExpectedNumberSamples((int) expected, acceptedErrorPercentage)) {
          break;
        }
      }

      // If we failed maxCount times, throw the exception.
      if (count == maxCount) {
        throw new RuntimeException("Statistics should show about " + expected + " samples; "
            + " but have " + HeapMonitor.sampledEvents() + " instead for the size "
            + currentSize);
      }
    }
  }
}
