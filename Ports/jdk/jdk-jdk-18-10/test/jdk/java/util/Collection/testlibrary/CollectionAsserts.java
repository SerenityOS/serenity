/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @library
 * CollectionAssert -- assertion methods for lambda test cases
 */
public class CollectionAsserts {

    private CollectionAsserts() {
        // no instances
    }

    public static void assertCountSum(Iterable<? super Integer> it, int count, int sum) {
        assertCountSum(it.iterator(), count, sum);
    }

    public static void assertCountSum(Iterator<? super Integer> it, int count, int sum) {
        int c = 0;
        int s = 0;
        while (it.hasNext()) {
            int i = (Integer) it.next();
            c++;
            s += i;
        }

        assertEquals(c, count);
        assertEquals(s, sum);
    }

    public static void assertConcat(Iterator<Character> it, String result) {
        StringBuilder sb = new StringBuilder();
        while (it.hasNext()) {
            sb.append(it.next());
        }

        assertEquals(result, sb.toString());
    }

    public static<T extends Comparable<? super T>> void assertSorted(Iterator<T> i) {
        if (!i.hasNext())
            return;
        T last = i.next();
        while (i.hasNext()) {
            T t = i.next();
            assertTrue(last.compareTo(t) <= 0);
            assertTrue(t.compareTo(last) >= 0);
            last = t;
        }
    }

    public static<T> void assertSorted(Iterator<T> i, Comparator<? super T> comp) {
        if (!i.hasNext())
            return;
        T last = i.next();
        while (i.hasNext()) {
            T t = i.next();
            assertTrue(comp.compare(last, t) <= 0);
            assertTrue(comp.compare(t, last) >= 0);
            last = t;
        }
    }

    public static<T extends Comparable<? super T>> void assertSorted(Iterable<T> iter) {
        assertSorted(iter.iterator());
    }

    public static<T> void assertSorted(Iterable<T> iter, Comparator<? super T> comp) {
        assertSorted(iter.iterator(), comp);
    }

    public static <T> void assertUnique(Iterable<T> iter) {
        assertUnique(iter.iterator());
    }

    public static<T> void assertUnique(Iterator<T> iter) {
        if (!iter.hasNext()) {
            return;
        }

        Set<T> uniq = new HashSet<>();
        while (iter.hasNext()) {
            T each = iter.next();
            assertTrue(!uniq.contains(each));
            uniq.add(each);
        }
    }

    public static<T> void assertContents(Iterable<T> actual, Iterable<T> expected) {
        assertContents(actual, expected, null);
    }

    public static<T> void assertContents(Iterable<T> actual, Iterable<T> expected, String msg) {
        assertContents(actual.iterator(), expected.iterator(), msg);
    }

    public static<T> void assertContents(Iterator<T> actual, Iterator<T> expected) {
        assertContents(actual, expected, null);
    }

    public static<T> void assertContents(Iterator<T> actual, Iterator<T> expected, String msg) {
        List<T> history = new ArrayList<>();

        while (expected.hasNext()) {
            if (!actual.hasNext()) {
                List<T> expectedData = new ArrayList<>(history);
                while (expected.hasNext())
                    expectedData.add(expected.next());
                fail(String.format("%s Premature end of data; expected=%s, found=%s",
                    (msg == null ? "" : msg), expectedData, history));
            }
            T a = actual.next();
            T e = expected.next();
            history.add(a);

            if (!Objects.equals(a, e))
                fail(String.format("%s Data mismatch; preceding=%s, nextExpected=%s, nextFound=%s",
                    (msg == null ? "" : msg), history, e, a));
        }
        if (actual.hasNext()) {
            List<T> rest = new ArrayList<>();
            while (actual.hasNext())
                rest.add(actual.next());
            fail(String.format("%s Unexpected data %s after %s",
                (msg == null ? "" : msg), rest, history));
        }
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static<T> void assertContents(Iterator<T> actual, T... expected) {
        assertContents(actual, Arrays.asList(expected).iterator());
    }

    public static<T extends Comparable<? super T>> void assertContentsUnordered(Iterable<T> actual, Iterable<T> expected) {
        assertContentsUnordered(actual, expected, null);
    }

    public static<T extends Comparable<? super T>> void assertContentsUnordered(Iterable<T> actual, Iterable<T> expected, String msg) {
        List<T> allExpected = new ArrayList<>();
        for (T t : expected) {
            allExpected.add(t);
        }

        for (T t : actual) {
            assertTrue(allExpected.remove(t), msg + " element '" + String.valueOf(t) + "' not found");
        }

        assertTrue(allExpected.isEmpty(), msg + "expected contained additional elements");
    }

    static <T> void assertSplitContents(Iterable<Iterable<T>> splits, Iterable<T> list) {
        Iterator<Iterable<T>> mI = splits.iterator();
        Iterator<T> pI = null;
        Iterator<T> lI = list.iterator();

        while (lI.hasNext()) {
            if (pI == null)
                pI = mI.next().iterator();
            while (!pI.hasNext()) {
                if (!mI.hasNext()) {
                    break;
                }
                else {
                    pI = mI.next().iterator();
                }
            }
            assertTrue(pI.hasNext());
            T pT = pI.next();
            T lT = lI.next();
            assertEquals(pT, lT);
        }

        if (pI != null) {
            assertTrue(!pI.hasNext());
        }

        while (mI.hasNext()) {
            pI = mI.next().iterator();
            assertTrue(!pI.hasNext());
        }
    }
}
