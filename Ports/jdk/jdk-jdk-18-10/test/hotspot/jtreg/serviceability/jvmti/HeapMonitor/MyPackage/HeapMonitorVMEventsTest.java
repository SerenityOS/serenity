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

import java.util.ArrayList;
import java.util.List;

// Graal is not tested here due to Graal not supporting DisableIntrinsic.
/**
 * @test
 * @summary Verifies that when the VM event is sent, sampled events are also collected.
 * @build Frame HeapMonitor
 * @compile HeapMonitorVMEventsTest.java
 * @requires vm.jvmti
 * @requires !vm.graal.enabled
 * @run main/othervm/native -XX:+UnlockDiagnosticVMOptions
 *                          -XX:DisableIntrinsic=_clone
 *                          -agentlib:HeapMonitorTest MyPackage.HeapMonitorVMEventsTest
 */

public class HeapMonitorVMEventsTest implements Cloneable {
  private static native int vmEvents();
  private static final int ITERATIONS = 1 << 15;
  private static final Object[] array = new Object[ITERATIONS];

  private static void cloneObjects(int iterations) {
    HeapMonitorVMEventsTest object = new HeapMonitorVMEventsTest();
    for (int i = 0; i < iterations; i++) {
      try {
        array[i] = object.clone();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  private static void checkDifference(int first, int second) {
    double diff = Math.abs(first - second) * 100;
    diff /= first;

    // Accept a 10% error interval: with objects being allocated: this allows a bit of room in
    // case other items are getting allocated during the test.
    if (diff > 10) {
      throw new RuntimeException("Error interval is over the accepted interval: " + diff
          + ": " + first + " , " + second);
    }
  }

  private static void compareSampledAndVM() {
    HeapMonitor.resetEventStorage();
    cloneObjects(ITERATIONS);

    int onlySampleCount = HeapMonitor.sampledEvents();

    HeapMonitor.enableVMEvents();
    HeapMonitor.resetEventStorage();
    if (!HeapMonitor.eventStorageIsEmpty()) {
      throw new RuntimeException("Storage is not empty after reset.");
    }

    cloneObjects(ITERATIONS);

    int sampleCount = HeapMonitor.sampledEvents();
    int vmCount = vmEvents();

    System.err.println("Obtained: " + onlySampleCount + " - " + sampleCount + " - "  + vmCount);
    checkDifference(onlySampleCount, sampleCount);
    checkDifference(onlySampleCount, vmCount);
  }

  public static void main(String[] args) {
    if (!HeapMonitor.eventStorageIsEmpty()) {
      throw new RuntimeException("Storage is not empty at test start...");
    }

    HeapMonitor.sampleEverything();
    compareSampledAndVM();
  }
}
