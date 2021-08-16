/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, Google and/or its affiliates. All rights reserved.
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
 * @summary Verifies the JVMTI Heap Monitor sampling interval average.
 * @build Frame HeapMonitor
 * @compile HeapMonitorStatIntervalTest.java
 * @requires vm.jvmti
 * @requires vm.compMode != "Xcomp"
 * @run main/othervm/native -agentlib:HeapMonitorTest MyPackage.HeapMonitorStatIntervalTest
 */

public class HeapMonitorStatIntervalTest {
  private static boolean testIntervalOnce(int interval, boolean throwIfFailure) {
    HeapMonitor.resetEventStorage();
    HeapMonitor.setSamplingInterval(interval);

    HeapMonitor.enableSamplingEvents();

    int allocationTotal = 10 * 1024 * 1024;
    int allocationIterations = 20;

    double actualCount = 0;
    for (int i = 0; i < allocationIterations; i++) {
      HeapMonitor.resetEventStorage();
      HeapMonitor.allocateSize(allocationTotal);
      actualCount += HeapMonitor.getEventStorageElementCount();
    }

    HeapMonitor.disableSamplingEvents();

    double expectedCount = allocationTotal * allocationIterations / interval;

    double error = Math.abs(actualCount - expectedCount);
    double errorPercentage = error / expectedCount * 100;

    boolean success = (errorPercentage < 15.0);
    System.out.println("Interval: " + interval + ", throw if failure: " + throwIfFailure
        + " - Expected count: " + expectedCount + ", allocationIterations: " + allocationIterations
        + ", actualCount: " + actualCount + " -> " + success);

    if (!success && throwIfFailure) {
      throw new RuntimeException("Interval average over 15% for interval " + interval + " -> "
          + actualCount + ", " + expectedCount);
    }

    return success;
  }


  private static void testInterval(int interval) {
    // Test the interval twice, it can happen that the test is "unlucky" and the interval just goes above
    // the 15% mark. So try again to squash flakiness.
    // Flakiness is due to the fact that this test is dependent on the sampling interval, which is a
    // statistical geometric variable around the sampling interval. This means that the test could be
    // unlucky and not achieve the mean average fast enough for the test case.
    if (!testIntervalOnce(interval, false)) {
      testIntervalOnce(interval, true);
    }
  }

  public static void main(String[] args) {
    int[] tab = {1024, 8192};

    HeapMonitor.calculateOneElementSize();

    for (int intervalIdx = 0; intervalIdx < tab.length; intervalIdx++) {
      testInterval(tab[intervalIdx]);
    }
  }
}
