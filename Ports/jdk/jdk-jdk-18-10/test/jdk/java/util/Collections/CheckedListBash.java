/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4904067
 * @summary Unit test for Collections.checkedList
 * @author  Josh Bloch
 * @key randomness
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

public class CheckedListBash {
    static Random rnd = new Random();

    public static void main(String[] args) {
        int numItr = 100;
        int listSize = 100;

        for (int i=0; i<numItr; i++) {
            List s1 = newList();
            AddRandoms(s1, listSize);

            List s2 = newList();
            AddRandoms(s2, listSize);

            List intersection = clone(s1); intersection.retainAll(s2);
            List diff1 = clone(s1); diff1.removeAll(s2);
            List diff2 = clone(s2); diff2.removeAll(s1);
            List union = clone(s1); union.addAll(s2);

            if (diff1.removeAll(diff2))
                fail("List algebra identity 2 failed");
            if (diff1.removeAll(intersection))
                fail("List algebra identity 3 failed");
            if (diff2.removeAll(diff1))
                fail("List algebra identity 4 failed");
            if (diff2.removeAll(intersection))
                fail("List algebra identity 5 failed");
            if (intersection.removeAll(diff1))
                fail("List algebra identity 6 failed");
            if (intersection.removeAll(diff1))
                fail("List algebra identity 7 failed");

            intersection.addAll(diff1); intersection.addAll(diff2);
            if (!(intersection.containsAll(union) &&
                  union.containsAll(intersection)))
                fail("List algebra identity 1 failed");

            Iterator e = union.iterator();
            while (e.hasNext())
                intersection.remove(e.next());
            if (!intersection.isEmpty())
                fail("Copy nonempty after deleting all elements.");

            e = union.iterator();
            while (e.hasNext()) {
                Object o = e.next();
                if (!union.contains(o))
                    fail("List doesn't contain one of its elements.");
                e.remove();
            }
            if (!union.isEmpty())
                fail("List nonempty after deleting all elements.");

            s1.clear();
            if (s1.size() != 0)
                fail("Clear didn't reduce size to zero.");

            s1.addAll(0, s2);
            if (!(s1.equals(s2) && s2.equals(s1)))
                fail("addAll(int, Collection) doesn't work.");
            // Reverse List
            for (int j=0, n=s1.size(); j<n; j++)
                s1.set(j, s1.set(n-j-1, s1.get(j)));
            // Reverse it again
            for (int j=0, n=s1.size(); j<n; j++)
                s1.set(j, s1.set(n-j-1, s1.get(j)));
            if (!(s1.equals(s2) && s2.equals(s1)))
                fail("set(int, Object) doesn't work");
        }

        List s = newList();
        for (int i=0; i<listSize; i++)
            s.add(new Integer(i));
        if (s.size() != listSize)
            fail("Size of [0..n-1] != n");

        List even = clone(s);
        Iterator it = even.iterator();
        while (it.hasNext())
            if (((Integer)it.next()).intValue() % 2 == 1)
                it.remove();
        it = even.iterator();
        while (it.hasNext())
            if (((Integer)it.next()).intValue() % 2 == 1)
                fail("Failed to remove all odd nubmers.");

        List odd = clone(s);
        for (int i=0; i<(listSize/2); i++)
            odd.remove(i);
        for (int i=0; i<(listSize/2); i++)
            if (((Integer)odd.get(i)).intValue() % 2 != 1)
                fail("Failed to remove all even nubmers.");

        List all = clone(odd);
        for (int i=0; i<(listSize/2); i++)
            all.add(2*i, even.get(i));
        if (!all.equals(s))
            fail("Failed to reconstruct ints from odds and evens.");

        all = clone(odd);
        ListIterator itAll = all.listIterator(all.size());
        ListIterator itEven = even.listIterator(even.size());
        while (itEven.hasPrevious()) {
            itAll.previous();
            itAll.add(itEven.previous());
            itAll.previous(); // ???
        }
        itAll = all.listIterator();
        while (itAll.hasNext()) {
            Integer i = (Integer)itAll.next();
            itAll.set(new Integer(i.intValue()));
        }
        itAll = all.listIterator();
        it = s.iterator();
        while (it.hasNext())
            if (it.next()==itAll.next())
                fail("Iterator.set failed to change value.");
        if (!all.equals(s))
            fail("Failed to reconstruct ints with ListIterator.");

        it = all.listIterator();
        int i=0;
        while (it.hasNext()) {
            Object o = it.next();
            if (all.indexOf(o) != all.lastIndexOf(o))
                fail("Apparent duplicate detected.");
            if (all.subList(i,   all.size()).indexOf(o) != 0 ||
                all.subList(i+1, all.size()).indexOf(o) != -1)
                fail("subList/indexOf is screwy.");
            if (all.subList(0,i+1).lastIndexOf(o) != i)
                fail("subList/lastIndexOf is screwy.");
            i++;
        }

        List l = newList();
        AddRandoms(l, listSize);
        Integer[] ia = (Integer[]) l.toArray(new Integer[0]);
        if (!l.equals(Arrays.asList(ia)))
            fail("toArray(Object[]) is hosed (1)");
        ia = new Integer[listSize];
        Integer[] ib = (Integer[]) l.toArray(ia);
        if (ia != ib || !l.equals(Arrays.asList(ia)))
            fail("toArray(Object[]) is hosed (2)");
        ia = new Integer[listSize+1];
        ia[listSize] = new Integer(69);
        ib = (Integer[]) l.toArray(ia);
        if (ia != ib || ia[listSize] != null
            || !l.equals(Arrays.asList(ia).subList(0, listSize)))
            fail("toArray(Object[]) is hosed (3)");

    }

    // Done inefficiently so as to exercise toArray
    static List clone(List s) {
        List a = Arrays.asList(s.toArray());
        if (s.hashCode() != a.hashCode())
            fail("Incorrect hashCode computation.");

        List clone = newList();
        clone.addAll(a);
        if (!s.equals(clone))
            fail("List not equal to copy.");
        if (!s.containsAll(clone))
            fail("List does not contain copy.");
        if (!clone.containsAll(s))
            fail("Copy does not contain list.");

        return clone;
    }

    static List newList() {
        List s =  Collections.checkedList(new ArrayList(), Integer.class);
        if (!s.isEmpty())
            fail("New instance non empty.");
        return s;
    }

    static void AddRandoms(List s, int n) {
        for (int i = 0; i < n; i++) {
            Integer e = rnd.nextInt(n);

            int preSize = s.size();
            if (!s.add(e))
                fail("Add failed.");
            int postSize = s.size();
            if (postSize - preSize != 1)
                fail("Add didn't increase size by 1.");
        }
    }

    static void fail(String s) {
        throw new RuntimeException(s);
    }
}
