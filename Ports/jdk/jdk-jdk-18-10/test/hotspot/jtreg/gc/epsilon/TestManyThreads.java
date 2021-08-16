/*
 * Copyright (c) 2017, 2018, Red Hat, Inc. All rights reserved.
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

package gc.epsilon;

/**
 * @test TestManyThreads
 * @requires vm.gc.Epsilon
 * @summary Test allocations from many threads
 *
 * @run main/othervm -Xmx128m -Xss512k                                        -XX:-UseTLAB -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestManyThreads
 * @run main/othervm -Xmx128m -Xss512k -Xint                                  -XX:-UseTLAB -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestManyThreads
 * @run main/othervm -Xmx128m -Xss512k -Xbatch -Xcomp                         -XX:-UseTLAB -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestManyThreads
 * @run main/othervm -Xmx128m -Xss512k -Xbatch -Xcomp -XX:TieredStopAtLevel=1 -XX:-UseTLAB -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestManyThreads
 * @run main/othervm -Xmx128m -Xss512k -Xbatch -Xcomp -XX:-TieredCompilation  -XX:-UseTLAB -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestManyThreads
 *
 * @run main/othervm -Xmx128m -Xss512k                                        -XX:+UseTLAB -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestManyThreads
 * @run main/othervm -Xmx128m -Xss512k -Xint                                  -XX:+UseTLAB -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestManyThreads
 * @run main/othervm -Xmx128m -Xss512k -Xbatch -Xcomp                         -XX:+UseTLAB -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestManyThreads
 * @run main/othervm -Xmx128m -Xss512k -Xbatch -Xcomp -XX:TieredStopAtLevel=1 -XX:+UseTLAB -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestManyThreads
 * @run main/othervm -Xmx128m -Xss512k -Xbatch -Xcomp -XX:-TieredCompilation  -XX:+UseTLAB -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestManyThreads
 */

import java.util.concurrent.atomic.*;

public class TestManyThreads {

  static int COUNT = Integer.getInteger("count", 128);  // 128 * 4M max tlabs = 512M, would overflow without TLAB sizing

  static volatile Object sink;
  static volatile Throwable failed;
  static final AtomicInteger allocated = new AtomicInteger();

  public static void workload() {
    try {
      sink = new Object();
      allocated.incrementAndGet();
      Thread.sleep(3600 * 1000);
    } catch (Throwable e) {
      failed = e;
    }
  }

  public static void main(String[] args) throws Throwable {
    for (int c = 0; c < COUNT; c++) {
      Thread t = new Thread(TestManyThreads::workload);
      t.setDaemon(true);
      t.start();
    }

    while ((failed == null) && (allocated.get() != COUNT)) {
        Thread.sleep(100);
    }

    if (failed != null) {
      throw failed;
    }
  }

}
