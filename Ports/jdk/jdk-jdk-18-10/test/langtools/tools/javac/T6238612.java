/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary com.sun.tools.util.List.toArray violates Collection spec
 * @modules jdk.compiler/com.sun.tools.javac.util
 */

import com.sun.tools.javac.util.List;

public class T6238612 {
    public static void main(String... args) {
        new T6238612().test();
    }

    boolean error = false;

    // exercise the List.toArray method for a variety of lists
    void test() {
        test(List.<String>nil());
        test(List.of("a"));
        test(List.of("a", "b", "c"));
        test(List.of("a", "b", "c", "d", "e", "f"));
        if (error)
            throw new Error("test failed");
    }

    // given a list, exercise the List.toArray method for a variety of arrays
    void test(List<String> list) {
        int n = list.size();
        if (n > 0)
            test(list, new String[0]);

        if (n > 1)
            test(list, new String[n - 1]);

        test(list, new String[n]);
        test(list, new String[n + 1]);
        test(list, new String[n + 5]);
    }

    // test the List.toArray method for a particular list and array
    void test(List<String> list, String[] array) {
        String[] result = list.toArray(array);

        if (result.length < list.size()) {
            error("returned array is too small; expected: " + list.size() + ", found: " + result.length);
            return;
        }

        if (list.size() <= array.length && result != array)
            error("new array wrongly created");

        int i = 0;
        for (String s: list)
            check(result, i++, s);

        if (i < result.length)
            check(result, i, null);
    }

    // check a specific element of an array
    void check(String[] array, int i, String expected) {
        if (!equal(array[i], expected))
                error("element " + i + " incorrect; expected: " + str(expected) + ", found: " + str(array[i]));
    }

    // check if two strings are both null or are equal
    boolean equal(String s1, String s2) {
        return (s1 == null ? s2 == null : s1.equals(s2));
    }

    String str(String s) {
        return (s == null ? "null" : '"' + s + '"');
    }

    void error(String message) {
        System.err.println(message);
        error = true;
    }
}
