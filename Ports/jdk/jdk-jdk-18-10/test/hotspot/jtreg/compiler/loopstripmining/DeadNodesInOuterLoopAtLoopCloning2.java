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

/**
 * @test
 * @bug 8234350
 * @summary loop unrolling breaks when outer strip mined loop contains dead node
 *
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileOnly=DeadNodesInOuterLoopAtLoopCloning2 DeadNodesInOuterLoopAtLoopCloning2
 *
 */

public class DeadNodesInOuterLoopAtLoopCloning2 {
    public static void vMeth(boolean b, int i, float f) {
        int i1=-4, i2=14, i16=10;
        byte by=116;

        for (i1 = 323; i1 > 3; i1 -= 2) {
            if (i2 != 0) {
                return;
            }
            for (i16 = 1; i16 < 10; i16++) {
                i2 = by;
                i += (i16 - i2);
                i2 -= i16;
                if (b) {
                    i = by;
                }
                i2 *= 20;
            }
        }
    }

    public static void main(String[] strArr) {
        DeadNodesInOuterLoopAtLoopCloning2 _instance = new DeadNodesInOuterLoopAtLoopCloning2();
        for (int i = 0; i < 10; i++ ) {
            _instance.vMeth(true, 168, -125.661F);
        }
    }
}
