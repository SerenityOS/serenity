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
 * @test TestObjects
 * @key randomness
 * @requires vm.gc.Epsilon
 * @summary Epsilon is able to allocate objects, and does not corrupt their state
 * @library /test/lib
 *
 * @run main/othervm -Xmx128m                                        -XX:+UseTLAB -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestObjects
 * @run main/othervm -Xmx128m -Xint                                  -XX:+UseTLAB -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestObjects
 * @run main/othervm -Xmx128m -Xbatch -Xcomp                         -XX:+UseTLAB -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestObjects
 * @run main/othervm -Xmx128m -Xbatch -Xcomp -XX:TieredStopAtLevel=1 -XX:+UseTLAB -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestObjects
 * @run main/othervm -Xmx128m -Xbatch -Xcomp -XX:-TieredCompilation  -XX:+UseTLAB -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestObjects
 *
 * @run main/othervm -Xmx128m                                        -XX:-UseTLAB -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestObjects
 * @run main/othervm -Xmx128m -Xint                                  -XX:-UseTLAB -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestObjects
 * @run main/othervm -Xmx128m -Xbatch -Xcomp                         -XX:-UseTLAB -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestObjects
 * @run main/othervm -Xmx128m -Xbatch -Xcomp -XX:TieredStopAtLevel=1 -XX:-UseTLAB -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestObjects
 * @run main/othervm -Xmx128m -Xbatch -Xcomp -XX:-TieredCompilation  -XX:-UseTLAB -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestObjects
 */

import java.util.Random;
import jdk.test.lib.Utils;

public class TestObjects {
  static int COUNT = Integer.getInteger("count", 1_000_000); // ~24 MB allocation

  static MyObject[] arr;

  public static void main(String[] args) throws Exception {
    Random r = Utils.getRandomInstance();

    arr = new MyObject[COUNT];
    for (int c = 0; c < COUNT; c++) {
      arr[c] = new MyObject(r.nextInt());
    }

    r = new Random(Utils.SEED);
    for (int c = 0; c < COUNT; c++) {
      int expected = r.nextInt();
      int actual = arr[c].id();
      if (expected != actual) {
        throw new IllegalStateException("Failure: expected = " + expected + ", actual = " + actual);
      }
    }
  }

  public static class MyObject {
    int id;
    public MyObject(int id) {
      this.id = id;
    }
    public int id() {
      return id;
    }
  }
}
