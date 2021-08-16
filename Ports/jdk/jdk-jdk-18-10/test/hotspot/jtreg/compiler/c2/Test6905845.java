/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6905845
 * @summary Server VM improperly optimizing away loop.
 *
 * @run main/timeout=480 compiler.c2.Test6905845
 */

package compiler.c2;

public class Test6905845 {

    public static void main(String[] args) {
        for (int asdf = 0; asdf < 5; asdf++) {
            //test block
            {
                StringBuilder strBuf1 = new StringBuilder(65);
                long start = System.currentTimeMillis();
                int count = 0;

                for (int i = Integer.MIN_VALUE; i < (Integer.MAX_VALUE - 80); i += 79) {
                    strBuf1.append(i);
                    count++;
                    strBuf1.delete(0, 65);
                }

                System.out.println(count);
                if (count != 54366674) {
                    System.out.println("wrong count: " + count + ", should be 54366674");
                    System.exit(97);
                }
            }
            //test block
            {
                StringBuilder strBuf1 = new StringBuilder(65);
                long start = System.currentTimeMillis();
                int count = 0;

                for (int i = Integer.MIN_VALUE; i < (Integer.MAX_VALUE - 80); i += 79) {
                    strBuf1.append(i);
                    count++;
                    strBuf1.delete(0, 65);
                }

                System.out.println(count);
                if (count != 54366674) {
                    System.out.println("wrong count: " + count + ", should be 54366674");
                    System.exit(97);
                }
            }
        }
    }
}

