/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6955504 6992121
 * @summary Test the StringBuilder.ensureCapacity() with negative minimumCapacity
 *    and append() method with negative length input argument.
 *    Also, test the StringBuffer class.
 */

import java.util.ArrayList;
import java.util.Vector;

public class EnsureCapacity {
    public static void main(String[] args) {
        testStringBuilder();
        testStringBuffer();
    }

    private static void checkCapacity(int before, int after) {
        if (before != after) {
            throw new RuntimeException("capacity is expected to be unchanged: " +
                "before=" + before + " after=" + after);
        }
    }

    private static void testStringBuilder() {
        StringBuilder sb = new StringBuilder("abc");
        int cap = sb.capacity();

        // test if negative minimumCapacity
        sb.ensureCapacity(Integer.MIN_VALUE);
        checkCapacity(cap, sb.capacity());

        try {
            char[] str = {'a', 'b', 'c', 'd'};
            // test if negative length
            sb.append(str, 0, Integer.MIN_VALUE + 10);
            throw new RuntimeException("IndexOutOfBoundsException not thrown");
        } catch (IndexOutOfBoundsException ex) {
        }
    }

    private static void testStringBuffer() {
        StringBuffer sb = new StringBuffer("abc");
        int cap = sb.capacity();

        // test if negative minimumCapacity
        sb.ensureCapacity(Integer.MIN_VALUE);
        checkCapacity(cap, sb.capacity());

        try {
            char[] str = {'a', 'b', 'c', 'd'};
            // test if negative length
            sb.append(str, 0, Integer.MIN_VALUE + 10);
            throw new RuntimeException("IndexOutOfBoundsException not thrown");
        } catch (IndexOutOfBoundsException ex) {
        }
    }
}
