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
 *
 */

/*
 * @test
 * @bug 8268360
 * @summary Test node placement when its use is inside infinite loop.
 * @library /test/lib
 * @run main/othervm -XX:CompileCommand=compileonly,compiler.loopopts.TestInfLoopNearUsePlacement::test
 *                   compiler.loopopts.TestInfLoopNearUsePlacement
 */

package compiler.loopopts;

import jdk.test.lib.Utils;

public class TestInfLoopNearUsePlacement {

    static void test() {
        long loa[] = new long[42];

        try {
            for (int i = 0; i < 42; i++) {
                Thread.sleep(1);
                loa[i] = 42L;
            }
        } catch (InterruptedException e) {}

        loa[0] = 1L;
        // Infinite loop: loop's variable is reset on each iteration
        for (int i = 0; i < 21; i++) {
            loa[0] += 1L;
            i = 1;
        }
    }

    public static void main(String[] args) throws Exception {
        // Execute test in own thread because it contains an infinite loop
        Thread thread = new Thread() {
            public void run() {
                for (int i = 0; i < 100; ++i) {
                    test();
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
        // Give thread some time to trigger compilation
        Thread.sleep(Utils.adjustTimeout(5000));
    }
}
