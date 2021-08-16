/*
 * Copyright (c) 2018, Red Hat, Inc. All rights reserved.
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
 * @bug 8194914
 * @summary "node not on backedge" assert in OuterStripMinedLoopNode::adjust_strip_mined_loop
 *
 * @run main/othervm -XX:-TieredCompilation -XX:+IgnoreUnrecognizedVMOptions -XX:-BackgroundCompilation -XX:+UseCountedLoopSafepoints -XX:LoopStripMiningIter=1000 BackedgeNodeWithOutOfLoopControl
 *
 */

public class BackedgeNodeWithOutOfLoopControl {

  public static void accessArrayVariables(int[] array, int i) {
    for (int j = 0; j < 100000; j++) {
      array[i-2]++;
      array[i-1]++;
    }
  }

  public void test() {
    int[] array = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    for (int i = 0; i < 2000; i++) {
        accessArrayVariables(array, 5);
    }
  }

  public static void main(String [] args) {
      BackedgeNodeWithOutOfLoopControl aa = new BackedgeNodeWithOutOfLoopControl();
      aa.test();
  }
}
