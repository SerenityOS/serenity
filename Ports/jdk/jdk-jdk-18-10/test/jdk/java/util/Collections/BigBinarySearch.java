/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5045582
 * @summary binarySearch of Collections larger than 1<<30
 * @author Martin Buchholz
 */

import java.util.AbstractList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;

public class BigBinarySearch {

    // Allows creation of very "big" collections without using too
    // many real resources
    static class SparseIntegerList
        extends AbstractList<Integer>
        implements RandomAccess
    {
        private Map<Integer,Integer> m = new HashMap<>();

        public Integer get(int i) {
            if (i < 0) throw new IndexOutOfBoundsException(""+i);
            Integer v = m.get(i);
            return (v == null) ? Integer.valueOf(0) : v;
        }

        public int size() {
            return Collections.max(m.keySet()) + 1;
        }

        public Integer set(int i, Integer v) {
            if (i < 0) throw new IndexOutOfBoundsException(""+i);
            Integer ret = get(i);
            if (v == 0)
                m.remove(i);
            else
                m.put(i, v);
            return ret;
        }
    }

    /** Checks that binarySearch finds an element where we got it. */
    private static void checkBinarySearch(List<Integer> l, int i) {
        try { equal(i, Collections.binarySearch(l, l.get(i))); }
        catch (Throwable t) { unexpected(t); }
    }

    /** Checks that binarySearch finds an element where we got it. */
    private static void checkBinarySearch(List<Integer> l, int i,
                                          Comparator<Integer> comparator) {
        try { equal(i, Collections.binarySearch(l, l.get(i), comparator)); }
        catch (Throwable t) { unexpected(t); }
    }

    private static void realMain(String[] args) throws Throwable {
        final int n = (1<<30) + 47;

        System.out.println("binarySearch(List<Integer>, Integer)");
        List<Integer> big = new SparseIntegerList();
        big.set(  0, -44);
        big.set(  1, -43);
        big.set(n-2,  43);
        big.set(n-1,  44);
        int[] ints = { 0, 1, n-2, n-1 };
        Comparator<Integer> reverse = Collections.reverseOrder();
        Comparator<Integer> natural = Collections.reverseOrder(reverse);

        for (int i : ints) {
            checkBinarySearch(big, i);
            checkBinarySearch(big, i, null);
            checkBinarySearch(big, i, natural);
        }
        for (int i : ints)
            big.set(i, - big.get(i));
        for (int i : ints)
            checkBinarySearch(big, i, reverse);
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
