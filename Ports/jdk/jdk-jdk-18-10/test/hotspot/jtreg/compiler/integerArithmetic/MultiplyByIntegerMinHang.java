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
 * @bug 8213419
 * @summary C2 may hang in MulLNode::Ideal()/MulINode::Ideal() with gcc 8.2.1
 *
 * @run main/othervm -XX:-TieredCompilation -XX:-BackgroundCompilation -XX:-UseOnStackReplacement MultiplyByIntegerMinHang
 *
 */

public class MultiplyByIntegerMinHang {
    public static void main(String[] args) {
        for (int i = 0; i < 20_000; i++) {
            if (test1(0) != 0) {
                throw new RuntimeException("incorrect result");
            }
            if (test1(1) != Integer.MIN_VALUE) {
                throw new RuntimeException("incorrect result");
            }
            if (test1(2) != 0) {
                throw new RuntimeException("incorrect result");
            }
            if (test2(0) != 0) {
                throw new RuntimeException("incorrect result");
            }
            if (test2(1) != Long.MIN_VALUE) {
                throw new RuntimeException("incorrect result");
            }
            if (test2(2) != 0) {
                throw new RuntimeException("incorrect result");
            }
        }
    }

    private static int test1(int v) {
        return v * Integer.MIN_VALUE;
    }

    private static long test2(long v) {
        return v * Long.MIN_VALUE;
    }
}
