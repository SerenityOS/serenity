/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @compile HeapMonitorArrayAllSampledTest.java
 * @run main/othervm/native -agentlib:HeapMonitorTest MyPackage.HeapMonitorArrayAllSampledTest
 */

public class HeapMonitorArrayAllSampledTest {

  // Do 1000 iterations and expect maxIteration samples.
  private static final int maxIteration = 1000;
  private static int array[];

  private static void allocate(int size) {
    for (int j = 0; j < maxIteration; j++) {
      array = new int[size];
    }
  }

  public static void main(String[] args) {
    int sizes[] = {1000, 10000, 100000, 1000000};

    HeapMonitor.sampleEverything();

    for (int currentSize : sizes) {
      System.out.println("Testing size " + currentSize);

      HeapMonitor.resetEventStorage();
      allocate(currentSize);

      // 10% error ensures a sanity test without becoming flaky.
      // Flakiness is due to the fact that this test is dependent on the sampling interval, which is a
      // statistical geometric variable around the sampling interval. This means that the test could be
      // unlucky and not achieve the mean average fast enough for the test case.
      if (!HeapMonitor.statsHaveExpectedNumberSamples(maxIteration, 10)) {
        throw new RuntimeException("Statistics should show about " + maxIteration + " samples.");
      }
    }
  }
}
