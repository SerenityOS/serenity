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
 * @bug 6312056 4155650 4294891 4904074
 * @summary Reasonable things should happen if mutating while iterating.
 */

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class ConcurrentModification {
    static volatile int passed = 0, failed = 0;

    static void fail(String msg) {
        failed++;
        new AssertionError(msg).printStackTrace();
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
        check(condition, "Assertion failed");
    }

    private static void test(ConcurrentMap<Integer, Integer> m)
    {
        try {
            m.clear();
            check(m.isEmpty());
            m.put(1,2);
            Iterator<Map.Entry<Integer,Integer>> it = m.entrySet().iterator();
            if (it.hasNext()) {
                m.remove(1); // sneaky
                Map.Entry<Integer, Integer> e = it.next();
                check(m.isEmpty());
                check(e.getKey() == 1);
                check(e.getValue() == 2);
            }
        } catch (Throwable t) {unexpected(t);}

        try {
            m.clear();
            check(m.isEmpty());
            m.put(1,2);
            Iterator<Map.Entry<Integer,Integer>> it = m.entrySet().iterator();
            if (it.hasNext()) {
                m.put(1,3); // sneaky
                Map.Entry<Integer, Integer> e = it.next();
                check(e.getKey() == 1);
                check(e.getValue() == 2 || e.getValue() == 3);
                if (m instanceof ConcurrentHashMap) {
                    e.setValue(4);
                    check(m.get(1) == 4);
                }
            }
        } catch (Throwable t) {unexpected(t);}
    }

    public static void main(String[] args) {
        test(new ConcurrentHashMap<Integer,Integer>());
        test(new ConcurrentSkipListMap<Integer,Integer>());

        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new Error("Some tests failed");
    }
}
