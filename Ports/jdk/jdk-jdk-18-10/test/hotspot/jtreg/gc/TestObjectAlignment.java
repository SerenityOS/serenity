/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc;

/**
 * @test TestObjectAlignment
 * @bug 8021823
 * @summary G1: Concurrent marking crashes with -XX:ObjectAlignmentInBytes>=32 in 64bit VMs
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @run main/othervm gc.TestObjectAlignment -Xmx20M -XX:+ExplicitGCInvokesConcurrent -XX:+IgnoreUnrecognizedVMOptions -XX:ObjectAlignmentInBytes=8
 * @run main/othervm gc.TestObjectAlignment -Xmx20M -XX:+ExplicitGCInvokesConcurrent -XX:+IgnoreUnrecognizedVMOptions -XX:ObjectAlignmentInBytes=16
 * @run main/othervm gc.TestObjectAlignment -Xmx20M -XX:+ExplicitGCInvokesConcurrent -XX:+IgnoreUnrecognizedVMOptions -XX:ObjectAlignmentInBytes=32
 * @run main/othervm gc.TestObjectAlignment -Xmx20M -XX:+ExplicitGCInvokesConcurrent -XX:+IgnoreUnrecognizedVMOptions -XX:ObjectAlignmentInBytes=64
 * @run main/othervm gc.TestObjectAlignment -Xmx20M -XX:+ExplicitGCInvokesConcurrent -XX:+IgnoreUnrecognizedVMOptions -XX:ObjectAlignmentInBytes=128
 * @run main/othervm gc.TestObjectAlignment -Xmx20M -XX:+ExplicitGCInvokesConcurrent -XX:+IgnoreUnrecognizedVMOptions -XX:ObjectAlignmentInBytes=256
 * @run main/othervm gc.TestObjectAlignment -Xmx20M -XX:-ExplicitGCInvokesConcurrent -XX:+IgnoreUnrecognizedVMOptions -XX:ObjectAlignmentInBytes=8
 * @run main/othervm gc.TestObjectAlignment -Xmx20M -XX:-ExplicitGCInvokesConcurrent -XX:+IgnoreUnrecognizedVMOptions -XX:ObjectAlignmentInBytes=16
 * @run main/othervm gc.TestObjectAlignment -Xmx20M -XX:-ExplicitGCInvokesConcurrent -XX:+IgnoreUnrecognizedVMOptions -XX:ObjectAlignmentInBytes=32
 * @run main/othervm gc.TestObjectAlignment -Xmx20M -XX:-ExplicitGCInvokesConcurrent -XX:+IgnoreUnrecognizedVMOptions -XX:ObjectAlignmentInBytes=64
 * @run main/othervm gc.TestObjectAlignment -Xmx20M -XX:-ExplicitGCInvokesConcurrent -XX:+IgnoreUnrecognizedVMOptions -XX:ObjectAlignmentInBytes=128
 * @run main/othervm gc.TestObjectAlignment -Xmx20M -XX:-ExplicitGCInvokesConcurrent -XX:+IgnoreUnrecognizedVMOptions -XX:ObjectAlignmentInBytes=256
 */

public class TestObjectAlignment {

  public static byte[] garbage;

  private static boolean runsOn32bit() {
    return System.getProperty("sun.arch.data.model").equals("32");
  }

  public static void main(String[] args) throws Exception {
    if (runsOn32bit()) {
      // 32 bit VMs do not allow setting ObjectAlignmentInBytes, so there is nothing to test. We still get called.
      return;
    }
    for (int i = 0; i < 10; i++) {
      garbage = new byte[1000];
      System.gc();
    }
  }
}
