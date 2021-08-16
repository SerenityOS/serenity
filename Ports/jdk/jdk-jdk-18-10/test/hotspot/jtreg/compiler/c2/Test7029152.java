/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7029152
 * @summary Ideal nodes for String intrinsics miss memory edge optimization
 *
 * @run main/othervm -Xbatch compiler.c2.Test7029152
 */

package compiler.c2;

public class Test7029152 {

    static final String str = "11111xx11111xx1x";
    static int idx = 0;

    static int IndexOfTest(String str) {
        return str.indexOf("11111xx1x");
    }

    public static void main(String args[]) {
        final int ITERS = 2000000;

        for (int i = 0; i < ITERS; i++) {
            idx = IndexOfTest(str);
        }
        System.out.println("IndexOf = " + idx);
    }
}
