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

/*
 * @test
 * @bug 8261137
 * @requires vm.flavor == "server"
 * @summary Verify that box object identity matches after deoptimization When it is eliminated.
 * @library /test/lib
 *
 * @run main/othervm -Xbatch compiler.c2.TestIdentityWithEliminateBoxInDebugInfo
 */
package compiler.c2;

import jdk.test.lib.Asserts;

public class TestIdentityWithEliminateBoxInDebugInfo {
    interface TestF {
        void apply(boolean condition);
    }

    public static void helper(TestF f) {
      // warmup
      for(int i = 0; i < 100000; i++) {
        f.apply(true);
      }
      // deoptimize
      f.apply(false);
    }

    public static void runTest() throws Exception {
        helper((c) -> {
            Integer a = Integer.valueOf(42);
            Integer b = Integer.valueOf(-42);
            if (!c) {
                Asserts.assertTrue(a == Integer.valueOf(42));
                Asserts.assertTrue(b == Integer.valueOf(-42));
            }
        });

        helper((c) -> {
            long highBitsOnly = 2L << 40;
            Long a = Long.valueOf(42L);
            Long b = Long.valueOf(-42L);
            Long h = Long.valueOf(highBitsOnly);
            if (!c) {
                Asserts.assertTrue(a == Long.valueOf(42L));
                Asserts.assertTrue(b == Long.valueOf(-42L));
                Asserts.assertFalse(h == Long.valueOf(highBitsOnly));
            }
        });

        helper((c) -> {
            Character a = Character.valueOf('a');
            Character b = Character.valueOf('Z');
            if (!c) {
                Asserts.assertTrue(a == Character.valueOf('a'));
                Asserts.assertTrue(b == Character.valueOf('Z'));
            }
        });

        helper((c) -> {
            Short a = Short.valueOf((short)42);
            Short b = Short.valueOf((short)-42);
            if (!c) {
                Asserts.assertTrue(a == Short.valueOf((short)42));
                Asserts.assertTrue(b == Short.valueOf((short)-42));
            }
        });

        helper((c) -> {
            Byte a = Byte.valueOf((byte)42);
            Byte b = Byte.valueOf((byte)-42);
            if (!c) {
                Asserts.assertTrue(a == Byte.valueOf((byte)42));
                Asserts.assertTrue(b == Byte.valueOf((byte)-42));
            }
        });

        helper((c) -> {
            Boolean a = Boolean.valueOf(true);
            Boolean b = Boolean.valueOf(false);
            if (!c) {
                Asserts.assertTrue(a == Boolean.valueOf(true));
                Asserts.assertTrue(b == Boolean.valueOf(false));
            }
        });
    }

    public static void main(String[] args) throws Exception {
        runTest();
    }
}
