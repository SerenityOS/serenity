/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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
 * @bug 8268672
 * @summary C2: assert(!loop->is_member(u_loop)) failed: can be in outer loop or out of both loops only
 *
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileOnly=TestPinnedNodeInInnerLoop TestPinnedNodeInInnerLoop
 *
 */

public class TestPinnedNodeInInnerLoop {
    boolean b;
    double d;
    int iArr[];

    public static void main(String[] args) {
        TestPinnedNodeInInnerLoop t = new TestPinnedNodeInInnerLoop();
        for (int i = 0; i < 10; i++) {
            t.test();
        }
    }

    void test() {
        int e = 4, f = -51874, g = 7, h = 0;

        for (; f < 3; ++f) {
        }
        while (++g < 2) {
            if (b) {
                d = h;
            } else {
                iArr[g] = e;
            }
        }
        System.out.println(g);
    }
}
