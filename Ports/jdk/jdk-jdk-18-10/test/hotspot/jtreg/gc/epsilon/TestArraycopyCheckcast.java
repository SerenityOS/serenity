/*
 * Copyright (c) 2019, Red Hat, Inc. All rights reserved.
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
 * @test TestArraycopyCheckcast
 * @requires vm.gc.Epsilon
 * @summary Epsilon is able to handle checkcasted array copies
 * @library /test/lib
 * @bug 8215724
 *
 * @run main/othervm -Xmx1g                                        -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestArraycopyCheckcast
 * @run main/othervm -Xmx1g -Xint                                  -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestArraycopyCheckcast
 * @run main/othervm -Xmx1g -Xbatch -Xcomp                         -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestArraycopyCheckcast
 * @run main/othervm -Xmx1g -Xbatch -Xcomp -XX:TieredStopAtLevel=1 -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestArraycopyCheckcast
 * @run main/othervm -Xmx1g -Xbatch -Xcomp -XX:-TieredCompilation  -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC gc.epsilon.TestArraycopyCheckcast
 */

public class TestArraycopyCheckcast {

  static int COUNT = Integer.getInteger("count", 1000);

  public static void main(String[] args) throws Exception {
    Object[] src = new Object[COUNT];
    Object[] dst = new B[COUNT];

    // Test 1. Copy nulls, should succeed
    try {
      System.arraycopy(src, 0, dst, 0, COUNT);
    } catch (ArrayStoreException e) {
      throw new IllegalStateException("Should have completed");
    }

    // Test 2. Copying incompatible type, should fail
    for (int c = 0; c < COUNT; c++) {
      src[c] = new A();
    }

    try {
      System.arraycopy(src, 0, dst, 0, COUNT);
      throw new IllegalStateException("Should have failed with ArrayStoreException");
    } catch (ArrayStoreException e) {
      // Expected
    }

    // Test 3. Copying compatible type, should succeeded
    for (int c = 0; c < COUNT; c++) {
      src[c] = new C();
    }

    try {
      System.arraycopy(src, 0, dst, 0, COUNT);
    } catch (ArrayStoreException e) {
      throw new IllegalStateException("Should have completed");
    }

    for (int c = 0; c < COUNT; c++) {
      if (src[c] != dst[c]) {
        throw new IllegalStateException("Copy failed at index " + c);
      }
    }
  }

  static class A {}
  static class B extends A {}
  static class C extends B {}
}
