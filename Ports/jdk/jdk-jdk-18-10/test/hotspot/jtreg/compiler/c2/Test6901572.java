/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6901572
 * @summary JVM 1.6.16 crash on loops: assert(has_node(i),"")
 *
 * @run main/othervm compiler.c2.Test6901572
 */

package compiler.c2;

public class Test6901572 {

    public static void main(String[] args) {
        for (int i = 0; i < 2; i++)
            NestedLoop();
    }

    public static long NestedLoop() {
        final int n = 50;
        long startTime = System.currentTimeMillis();
        int x = 0;
        for(int a = 0; a < n; a++)
            for(int b = 0; b < n; b++)
                for(int c = 0; c < n; c++)
                    for(int d = 0; d < n; d++)
                        for(int e = 0; e < n; e++)
                            for(int f = 0; f < n; f++)
                                x++;
        long stopTime = System.currentTimeMillis();

        return stopTime - startTime;
    }
}
