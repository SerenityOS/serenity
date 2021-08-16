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
 * @bug 6897150
 * @summary Hotspot optimises away a valid loop
 *
 * @run main compiler.c2.Test6897150
 */

package compiler.c2;

// Should be compiled with javac from JDK1.3 to get bytecode which shows the problem.
public class Test6897150 {
    public static void main(String[] args) {
        // This works
        loopAndPrint(Integer.MAX_VALUE -1);
        // This doesn't
        loopAndPrint(Integer.MAX_VALUE);
    }

    static void verify(int max, int a) {
        if ( a != (max - 1)) {
            System.out.println("Expected: " + (max - 1));
            System.out.println("Actual  : " + a);
            System.exit(97);
        }
    }
    static void loopAndPrint(int max) {
        int a = -1;
        int i = 1;
        for (; i < max; i++) {
            a = i;
        }
        verify(max, a);
    }
}

