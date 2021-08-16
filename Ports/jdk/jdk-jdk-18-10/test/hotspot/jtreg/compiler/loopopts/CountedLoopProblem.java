/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8072753
 * @summary Inner loop induction variable increment occurs before compare which causes integer overflow
 * @run main/othervm compiler.loopopts.CountedLoopProblem
 *
 */

package compiler.loopopts;

import java.util.Random;

public class CountedLoopProblem {
    public static void main(String[] args) throws Exception {
        Random r = new Random(42);
        int x = 0;
        try {
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < 1000000; ++i) {
                int v = Math.abs(r.nextInt());
                sb.append('+').append(v).append('\n');
                x += v;
                // To trigger the problem we must OSR in the following loop
                // To make the problem 100% reproducible run with -XX:-TieredCompilation -XX:OSROnlyBCI=62
                while(x < 0) x += 1000000000;
                sb.append('=').append(x).append('\n');
            }
            if (sb.toString().hashCode() != 0xaba94591) {
                throw new Exception("Unexpected result");
            }
        } catch(OutOfMemoryError e) {
            // small heap, ignore
        }
    }
}

