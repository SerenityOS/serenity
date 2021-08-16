/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Alibaba Group Holding Limited. All Rights Reserved.
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
 * @bug 8183543
 * @summary C2 compilation often fails on aarch64 with "failed spill-split-recycle sanity check"
 *
 * @library /test/lib
 *
 * @build sun.hotspot.WhiteBox
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xbatch
 *                   -XX:-Inline
 *                   -XX:-TieredCompilation
 *                   -XX:+PreserveFramePointer
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   compiler.regalloc.TestC2IntPressure
 */

package compiler.regalloc;

import sun.hotspot.WhiteBox;

public class TestC2IntPressure {

  static volatile int vol_f;

  static void not_inlined() {
    // Do nothing
  }

  static int test(TestC2IntPressure arg) {
    TestC2IntPressure a = arg;
    int res = 0;
    not_inlined();
    res = a.vol_f;
    return res;
  }

  public static void main(String args[]) {
    TestC2IntPressure arg = new TestC2IntPressure();
    for (int i = 0; i < 10000; i++) {
      test(arg);
    }
    try {
      var method = TestC2IntPressure.class.getDeclaredMethod("test", TestC2IntPressure.class);
      if (!WhiteBox.getWhiteBox().isMethodCompiled(method)) {
        throw new Error("test method didn't get compiled");
      }
    } catch (NoSuchMethodException e) {
      throw new Error("TESTBUG : " + e, e);
    }
  }
}

