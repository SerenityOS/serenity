/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7177306
 * @summary Regression: unchecked method call does not erase return type
 */
import java.util.List;

public class T7177306d {

    static int assertionCount = 0;

    static void assertTrue(boolean cond) {
        if (!cond) {
            throw new AssertionError();
        }
        assertionCount++;
    }

    static <T, S extends List<T>> void m(List<? super T> arg1, S arg2, Class<Object> arg3) { assertTrue(false); }
    static void m(Object o1, Object o2, Object o3) { assertTrue(true); }

    static void test(List<Integer> li, List<String> ls, Class c) {
        m(li, ls, c);
    }

    public static void main(String[] args) {
        test(null, null, null);
        assertTrue(assertionCount == 1);
    }
}
