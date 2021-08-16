/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7005594
 * @summary Array overflow not handled correctly with loop optimzations
 *
 * @run main/othervm -Xcomp
                     -XX:CompileOnly=compiler.c2.Test7005594::test
                     compiler.c2.Test7005594
 */

package compiler.c2;

public class Test7005594 {
    static int test(byte a[]){
        int result = 0;
        for (int i = 1; i < a.length; i += Integer.MAX_VALUE) {
            result += a[i];
        }
        return result;
    }

    public static void main(String [] args){
        try {
            int result = test(new byte[2]);
            throw new AssertionError("Expected ArrayIndexOutOfBoundsException was not thrown");
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Expected " + e + " was thrown");
        }
    }
}
