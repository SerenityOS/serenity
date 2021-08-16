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
 * @test TestAlwaysPretouch
 * @requires vm.gc.Epsilon
 * @summary Basic sanity test for Epsilon
 * @library /test/lib
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestEpsilonEnabled
 */

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

public class TestEpsilonEnabled {
  public static void main(String[] args) throws Exception {
    if (!isEpsilonEnabled()) {
      throw new IllegalStateException("Debug builds should have Epsilon enabled");
    }
  }

  public static boolean isEpsilonEnabled() {
    for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
      if (bean.getName().contains("Epsilon")) {
        return true;
      }
    }
    return false;
  }
}
