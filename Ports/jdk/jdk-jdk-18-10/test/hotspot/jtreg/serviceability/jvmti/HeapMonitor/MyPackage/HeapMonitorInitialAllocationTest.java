/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Google and/or its affiliates. All rights reserved.
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
 * @summary Verifies the JVMTI Heap Monitor does not always sample the first object.
 * @requires vm.jvmti
 * @build Frame HeapMonitor
 * @compile HeapMonitorInitialAllocationTest.java
 * @run main/othervm/native -agentlib:HeapMonitorTest MyPackage.HeapMonitorInitialAllocationTest
 */
public class HeapMonitorInitialAllocationTest {
  public static void main(String[] args) throws Exception {
    final int numThreads = 500;

    // Allocate the threads first to not risk sampling.
    Thread threads[] = new Thread[numThreads];
    for (int i = 0; i < numThreads ; i++) {
      threads[i] = new Thread(new Task(), "Task " + i);
    }

    HeapMonitor.setSamplingInterval(1024 * 1024);
    HeapMonitor.enableSamplingEvents();

    for (int i = 0; i < numThreads ; i++) {
      threads[i].start();
    }

    for (int i = 0; i < numThreads ; i++) {
      threads[i].join();
    }

    HeapMonitor.disableSamplingEvents();

    int sampledEvents = HeapMonitor.sampledEvents();
    if (sampledEvents > numThreads / 2) {
      throw new RuntimeException(
          "Sampling the initial allocation too many times: " + sampledEvents);
    }
  }

  private static class Task implements Runnable {
    @Override
    public void run() {
      new EmptyObject();
    }
  }

  private static class EmptyObject {
  }
}
