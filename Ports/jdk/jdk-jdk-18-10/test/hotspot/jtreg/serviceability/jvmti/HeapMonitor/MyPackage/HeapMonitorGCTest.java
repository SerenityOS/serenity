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

import java.util.List;

/**
 * @test
 * @build Frame HeapMonitor
 * @summary Verifies the default GC with the Heap Monitor event system.
 * @compile HeapMonitorGCTest.java
 * @requires vm.gc == "G1" | vm.gc == "null"
 * @requires vm.jvmti
 * @run main/othervm/native -agentlib:HeapMonitorTest MyPackage.HeapMonitorGCTest
 */

/**
 * This test is checking that various GCs work as intended: events are sent, forcing GC works, etc.
 */
public class HeapMonitorGCTest {
  public static void main(String[] args) {
    Frame[] frames = HeapMonitor.allocateAndCheckFrames();

    HeapMonitor.forceGarbageCollection();

    if (!HeapMonitor.garbageContains(frames)) {
      throw new RuntimeException("Forcing GC did not work, not a single object was collected.");
    }
  }
}
