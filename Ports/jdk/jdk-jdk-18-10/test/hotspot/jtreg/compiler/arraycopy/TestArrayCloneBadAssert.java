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
 * @bug 8073792
 * @summary assert broken when array size becomes known during igvn
 *
 * @run main/othervm -Xcomp
 *      -XX:CompileCommand=compileonly,compiler.arraycopy.TestArrayCloneBadAssert::m
 *      compiler.arraycopy.TestArrayCloneBadAssert
 */

package compiler.arraycopy;

public class TestArrayCloneBadAssert {

    static final int[] array = new int[5];

    static int[] m(int[] arr) {
        int i = 0;
        for (; i < 2; i++) {
        }
        if (i == 2) {
            arr = array;
        }
        return arr.clone();
    }

    static public void main(String[] args) {
        int[] arr = new int[5];
        m(arr);
    }
}
