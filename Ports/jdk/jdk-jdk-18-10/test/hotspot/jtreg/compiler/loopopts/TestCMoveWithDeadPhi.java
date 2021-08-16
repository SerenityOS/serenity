/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8265938
 * @summary Test conditional move optimization with a TOP PhiNode.
 * @library /test/lib
 * @run main/othervm -XX:CompileCommand=compileonly,compiler.loopopts.TestCMoveWithDeadPhi::test
 *                   compiler.loopopts.TestCMoveWithDeadPhi
 */

package compiler.loopopts;

import jdk.test.lib.Utils;

public class TestCMoveWithDeadPhi {

    static void test(boolean b) {
        if (b) {
            long l = 42;
            for (int i = 0; i < 100; i++) {
                if (i < 10) {
                    l++;
                    if (i == 5) {
                        break;
                    }
                }
            }

            // Infinite loop
            for (int j = 0; j < 100; j++) {
                j--;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // Execute test in own thread because it contains an infinite loop
        Thread thread = new Thread() {
            public void run() {
                for (int i = 0; i < 50_000; ++i) {
                    test((i % 2) == 0);
                }
            }
        };
        // Give thread some time to trigger compilation
        thread.setDaemon(true);
        thread.start();
        Thread.sleep(Utils.adjustTimeout(4000));
    }
}
