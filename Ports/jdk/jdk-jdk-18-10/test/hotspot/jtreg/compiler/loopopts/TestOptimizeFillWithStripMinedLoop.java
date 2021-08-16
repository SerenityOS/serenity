/*
 * Copyright (c) 2021, Huawei Technologies Co., Ltd. All rights reserved.
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
 * @bug 8263352
 * @summary assert(use == polladr) failed: the use should be a safepoint polling
 * @requires vm.flavor == "server"
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:+OptimizeFill
 *                   compiler.loopopts.TestOptimizeFillWithStripMinedLoop
 *
 */

package compiler.loopopts;

public class TestOptimizeFillWithStripMinedLoop {

    class Wrap {
      public int value;
      public Wrap(int v) {
        value = v;
      }
    }

    public static int size = 1024;
    public static int[] ia = new int[size];

    public static void main(String[] args) throws Exception {
      TestOptimizeFillWithStripMinedLoop m = new TestOptimizeFillWithStripMinedLoop();
      m.test();
    }

    public void test() throws Exception {
      for(int i = 0; i < 20_000; i++) {
        Wrap obj = null;
        if (i % 113 != 0) {
          obj = new Wrap(i);
        }
        foo(obj);
      }
    }

    public int foo(Wrap obj) throws Exception {
        boolean condition = false;
        int first = -1;

        if (obj == null) {
          condition = true;
          first = 24;
        }

        for (int i = 0; i < size; i++) {
            ia[i] = condition ? first : obj.value;
        }
        return 0;
    }
}
