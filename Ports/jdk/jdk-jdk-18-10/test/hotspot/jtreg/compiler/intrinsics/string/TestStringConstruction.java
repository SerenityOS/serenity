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
 * @bug 8142303
 * @summary Tests handling of invalid array indices in C2 intrinsic if explicit range check in Java code is not inlined.
 *
 * @run main/othervm
 *      -XX:CompileCommand=inline,java.lang.String::*
 *      -XX:CompileCommand=inline,java.lang.StringUTF16::*
 *      -XX:CompileCommand=exclude,java.lang.String::checkBoundsOffCount
 *      compiler.intrinsics.string.TestStringConstruction
 */

package compiler.intrinsics.string;

public class TestStringConstruction {

    public static void main(String[] args) {
        char[] chars = new char[42];
        for (int i = 0; i < 10_000; ++i) {
            test(chars);
        }
    }

    private static String test(char[] chars) {
        try {
            // The constructor calls String::checkBoundsOffCount(-1, 42) to perform
            // range checks on offset and count. If this method is not inlined, C2
            // does not know about the explicit range checks and does not cut off the
            // dead code. As a result, -1 is fed as offset into the StringUTF16.compress
            // intrinsic which is replaced by TOP and causes a failure in the matcher.
            return new String(chars, -1 , 42);
        } catch (Exception e) {
            return "";
        }
    }
}

