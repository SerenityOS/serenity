/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Google and/or its affiliates. All rights reserved.
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
 * @summary Verifies the JVMTI Heap Monitor sampling via object allocation.
 * @requires vm.jvmti
 * @compile HeapMonitorStatObjectCorrectnessTest.java
 * @run main/othervm/native -agentlib:HeapMonitorTest MyPackage.HeapMonitorStatObjectCorrectnessTest
 */

/** This test is checking the object allocation path works with heap sampling. */
public class HeapMonitorStatObjectCorrectnessTest {

  // Do 400000 iterations and expect maxIteration / multiplier samples.
  private static final int maxIteration = 400_000;
  private static BigObject obj;

  // 15% error ensures a sanity test without becoming flaky.
  // Flakiness is due to the fact that this test is dependent on the sampling interval, which is a
  // statistical geometric variable around the sampling interval. This means that the test could be
  // unlucky and not achieve the mean average fast enough for the test case.
  private static final int acceptedErrorPercentage = 15;

  private static void allocate() {
    emptyStorage();

    HeapMonitor.enableSamplingEvents();
    // Instead of relying on the allocation loop to fill and retire the TLAB, which might not happen,
    // use System.gc() to retire the TLAB and ensure sampling happens
    System.gc();
    for (int j = 0; j < maxIteration; j++) {
      obj = new BigObject();
    }
    HeapMonitor.disableSamplingEvents();
  }

  private static void testBigAllocationInterval() {
    final int sizeObject = 1400;

    // 111 is as good a number as any.
    final int samplingMultiplier = 111;
    HeapMonitor.setSamplingInterval(samplingMultiplier * sizeObject);

    allocate();

    // For simplifications, the code is allocating:
    //   (BigObject size) * maxIteration.
    //
    // We ignore the class memory usage apart from field memory usage for BigObject. BigObject
    // allocates 250 long, so 2000 bytes, so whatever is used for the class is negligible.
    //
    // That means that with maxIterations, the loop in the method allocate requests:
    //    maxIterations * 2000 bytes.
    //
    // Via the enable sampling, the code requests a sample every samplingMultiplier * sizeObject bytes.
    //
    // Therefore, the expected sample number is:
    //   (maxIterations * sizeObject) / (samplingMultiplier * sizeObject);
    //
    // Which becomes:
    //   maxIterations / samplingMultiplier
    double expected = maxIteration;
    expected /= samplingMultiplier;

    if (!HeapMonitor.statsHaveExpectedNumberSamples((int) expected, acceptedErrorPercentage)) {
      throw new RuntimeException("Statistics should show about " + expected + " samples.");
    }
  }

  private static void emptyStorage() {
    HeapMonitor.resetEventStorage();

    if (!HeapMonitor.eventStorageIsEmpty()) {
      throw new RuntimeException("Statistics should be null to begin with.");
    }
  }

  private static void testEveryAllocationSampled() {
    // 0 means sample every allocation.
    HeapMonitor.setSamplingInterval(0);

    allocate();

    double expected = maxIteration;

    if (!HeapMonitor.statsHaveExpectedNumberSamples((int) expected, acceptedErrorPercentage)) {
      throw new RuntimeException("Statistics should show about " + expected + " samples.");
    }
  }

  public static void main(String[] args) {
    testBigAllocationInterval();
    testEveryAllocationSampled();
  }

  /**
   * Big class on purpose to just be able to ignore the class memory space overhead.
   *
   * Class contains 175 long fields, so 175 * 8 = 1400 bytes.
   */
  private static class BigObject {
    private long a0_0, a0_1, a0_2, a0_3, a0_4, a0_5, a0_6, a0_7, a0_8, a0_9;
    private long a1_0, a1_1, a1_2, a1_3, a1_4, a1_5, a1_6, a1_7, a1_8, a1_9;
    private long a2_0, a2_1, a2_2, a2_3, a2_4, a2_5, a2_6, a2_7, a2_8, a2_9;
    private long a3_0, a3_1, a3_2, a3_3, a3_4, a3_5, a3_6, a3_7, a3_8, a3_9;
    private long a4_0, a4_1, a4_2, a4_3, a4_4, a4_5, a4_6, a4_7, a4_8, a4_9;
    private long a5_0, a5_1, a5_2, a5_3, a5_4, a5_5, a5_6, a5_7, a5_8, a5_9;
    private long a6_0, a6_1, a6_2, a6_3, a6_4, a6_5, a6_6, a6_7, a6_8, a6_9;
    private long a7_0, a7_1, a7_2, a7_3, a7_4, a7_5, a7_6, a7_7, a7_8, a7_9;
    private long a8_0, a8_1, a8_2, a8_3, a8_4, a8_5, a8_6, a8_7, a8_8, a8_9;
    private long a9_0, a9_1, a9_2, a9_3, a9_4, a9_5, a9_6, a9_7, a9_8, a9_9;
    private long a10_0, a10_1, a10_2, a10_3, a10_4, a10_5, a10_6, a10_7, a10_8, a10_9;
    private long a11_0, a11_1, a11_2, a11_3, a11_4, a11_5, a11_6, a11_7, a11_8, a11_9;
    private long a12_0, a12_1, a12_2, a12_3, a12_4, a12_5, a12_6, a12_7, a12_8, a12_9;
    private long a13_0, a13_1, a13_2, a13_3, a13_4, a13_5, a13_6, a13_7, a13_8, a13_9;
    private long a14_0, a14_1, a14_2, a14_3, a14_4, a14_5, a14_6, a14_7, a14_8, a14_9;
    private long a15_0, a15_1, a15_2, a15_3, a15_4, a15_5, a15_6, a15_7, a15_8, a15_9;
    private long a16_0, a16_1, a16_2, a16_3, a16_4, a16_5, a16_6, a16_7, a16_8, a16_9;
    private long a17_0, a17_1, a17_2, a17_3, a17_4;
  }
}
