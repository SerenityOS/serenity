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

/*
 * @test
 * @bug 8134468
 * @summary test that checks whether an array load falls into the range of an arraycopy is incorrect on 32bits
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                    compiler.arraycopy.TestArrayCopyOverflowInBoundChecks
 */

package compiler.arraycopy;

public class TestArrayCopyOverflowInBoundChecks {

    static byte[] src_array = { 'a', 'b', 'c', 'd', 'e' };

    static byte test(int copy_len) {
        byte[] dst_array = new byte[10];
        System.arraycopy(src_array, 0, dst_array, 1, copy_len);
        return dst_array[1];
    }

    static public void main(String[] args) {
        for (int i = 0; i < 20000; i++) {
            if (test(src_array.length - 1) != src_array[0]) {
                throw new RuntimeException("Test failed");
            }
        }
    }
}
