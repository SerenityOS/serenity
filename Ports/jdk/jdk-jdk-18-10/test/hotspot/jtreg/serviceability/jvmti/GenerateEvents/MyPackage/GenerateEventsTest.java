/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8222072
 * @summary Send CompiledMethodLoad events only to the environment requested it with GenerateEvents
 * @requires vm.jvmti
 * @compile GenerateEventsTest.java
 * @run main/othervm/native -agentlib:GenerateEvents1 -agentlib:GenerateEvents2 MyPackage.GenerateEventsTest
 */

package MyPackage;

public class GenerateEventsTest {
  static native void agent1GenerateEvents();
  static native void agent2SetThread(Thread thread);
  static native boolean agent1FailStatus();
  static native boolean agent2FailStatus();

  public static void main(String[] args) {
      agent2SetThread(Thread.currentThread());
      agent1GenerateEvents(); // Re-generate CompiledMethodLoad events
      if (agent1FailStatus()|| agent2FailStatus()) {
         throw new RuntimeException("GenerateEventsTest failed!");
      }
      System.out.println("GenerateEventsTest passed!");
  }
}
