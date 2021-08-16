/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8004051
 * @bug 8005722
 * @summary assert(_oprs_len[mode] < maxNumberOfOperands) failed: array overflow
 *
 * @run main/othervm -Xcomp compiler.c1.Test8004051
 */

package compiler.c1;

public class Test8004051 {
    public static void main(String[] argv) {
        Object o = new Object();
        fillPrimRect(1.1f, 1.2f, 1.3f, 1.4f,
                     o, o,
                     1.5f, 1.6f, 1.7f, 1.8f,
                     2.0f, 2.1f, 2.2f, 2.3f,
                     2.4f, 2.5f, 2.6f, 2.7f,
                     100, 101);
        System.out.println("Test passed, test did not assert");
    }

    static boolean fillPrimRect(float x, float y, float w, float h,
                                Object rectTex, Object wrapTex,
                                float bx, float by, float bw, float bh,
                                float f1, float f2, float f3, float f4,
                                float f5, float f6, float f7, float f8,
                                int i1, int i2 ) {
        System.out.println(x + " " + y + " " + w + " " + h + " " +
                           bx + " " + by + " " + bw + " " + bh);
        return true;
    }
}
