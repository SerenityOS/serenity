/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/* @test

 * @run main/native compiler.calls.TestDirtyInt
 */

package compiler.calls;

public class TestDirtyInt {
    static {
        System.loadLibrary("TestDirtyInt");
    }

    native static int test(int v);

    static int compiled(int v) {
        return test(v<<2);
    }

    static public void main(String[] args) {
        for (int i = 0; i < 20000; i++) {
            int res = compiled(Integer.MAX_VALUE);
            if (res != 0x42) {
                throw new RuntimeException("Test failed");
            }
        }
    }
}
