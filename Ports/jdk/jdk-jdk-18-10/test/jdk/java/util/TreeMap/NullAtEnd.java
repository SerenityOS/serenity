/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     5018254
 * @summary Test null-allowing Comparators
 * @author  Martin Buchholz
 */

import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

public class NullAtEnd {
    static volatile int passed = 0, failed = 0;

    static void fail(String msg) {
        failed++;
        new AssertionError(msg).printStackTrace();
    }

    static void pass() {
        passed++;
    }

    static void unexpected(Throwable t) {
        failed++;
        t.printStackTrace();
    }

    static void check(boolean condition, String msg) {
        if (condition)
            passed++;
        else
            fail(msg);
    }

    static void check(boolean condition) {
        check(condition, "Assertion failure");
    }

    private static boolean eq(Object x, Object y) {
        return x == null ? y == null : x.equals(y);
    }

    private static final Comparator<String> NULL_AT_END
        = new Comparator<>() {
            /**
             * Allows for nulls.  Null is greater than anything non-null.
             */
            public int compare(String x, String y) {
                if (x == null && y == null) return 0;
                if (x == null && y != null) return 1;
                if (x != null && y == null) return -1;
                return x.compareTo(y);
            }
        };

    public static void main(String[] args) {
        try {
            SortedMap<String,String> m1 = new TreeMap<>(NULL_AT_END);
            check(eq(m1.put("a", "a"), null));
            check(eq(m1.put("b", "b"), null));
            check(eq(m1.put("c", "c"), null));
            check(eq(m1.put(null, "d"), null));

            SortedMap<String,String> m2 = new TreeMap<>(m1);

            check(eq(m1.lastKey(), null));
            check(eq(m1.get(m1.lastKey()), "d"));
            check(eq(m1.remove(m1.lastKey()), "d"));
            check(eq(m1.lastKey(), "c"));

            check(eq(m2.entrySet().toString(), "[a=a, b=b, c=c, null=d]"));

            SortedMap<String,String> m3 = m2.tailMap("b");

            check(eq(m3.lastKey(), null));
            check(eq(m3.get(m3.lastKey()), "d"));
            check(eq(m3.remove(m3.lastKey()), "d"));
            check(eq(m3.lastKey(), "c"));

        } catch (Throwable t) { unexpected(t); }

        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new Error("Some tests failed");
    }
}
