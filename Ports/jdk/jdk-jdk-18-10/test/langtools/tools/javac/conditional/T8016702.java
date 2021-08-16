/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8016702
 * @summary use of ternary operator in lambda expression gives incorrect results
 */
import java.util.Arrays;
import java.util.List;

public class T8016702 {

    static int assertionCount;

    static void assertTrue(boolean b, String msg) {
        assertionCount++;
        if (!b) {
            throw new AssertionError(msg);
        }
    }

    interface IntFunction<Y> {
        Y m(int x);
    }

    void test(List<Integer> li) {
        map(i -> (i % 2 == 0) ? "" : "i="+i, li);
    }


    @SuppressWarnings("unchecked")
    <R> void map(IntFunction<R> mapper, List<Integer> li) {
        for (int i : li) {
            String res = (String)mapper.m(i);
            assertTrue((i % 2 == 0) ? res.isEmpty() : res.contains("" + i),
                    "i = " + i + " res = " + res);
        }
    }

    public static void main(String[] args) {
        T8016702 tester = new T8016702();
        tester.test(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        assertTrue(assertionCount == 10, "wrong assertion count: " + assertionCount);
    }
}
