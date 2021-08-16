/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4904067 7129185
 * @summary Unit test for Collections.checkedSet
 * @author  Josh Bloch
 * @run testng CheckedSetBash
 * @key randomness
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class CheckedSetBash {
    static final int numItr = 100;
    static final int setSize = 100;
    static final Random rnd = new Random();

    @Test(dataProvider = "Supplier<Set<Integer>>")
    public static void testCheckedSet(String description, Supplier<Set<Integer>> supplier) {

        Set<Integer> s1 = supplier.get();
        assertTrue(s1.isEmpty());

        AddRandoms(s1, setSize);

        Set<Integer> s2 = supplier.get();

        assertTrue(s2.isEmpty());

        AddRandoms(s2, setSize);

        Set<Integer> intersection = clone(s1, supplier);
        intersection.retainAll(s2);
        Set<Integer> diff1 = clone(s1, supplier); diff1.removeAll(s2);
        Set<Integer> diff2 = clone(s2, supplier); diff2.removeAll(s1);
        Set<Integer> union = clone(s1, supplier); union.addAll(s2);

        if (diff1.removeAll(diff2))
            fail("Set algebra identity 2 failed");
        if (diff1.removeAll(intersection))
            fail("Set algebra identity 3 failed");
        if (diff2.removeAll(diff1))
            fail("Set algebra identity 4 failed");
        if (diff2.removeAll(intersection))
            fail("Set algebra identity 5 failed");
        if (intersection.removeAll(diff1))
            fail("Set algebra identity 6 failed");
        if (intersection.removeAll(diff1))
            fail("Set algebra identity 7 failed");

        intersection.addAll(diff1); intersection.addAll(diff2);
        if (!intersection.equals(union))
            fail("Set algebra identity 1 failed");

        if (new HashSet(union).hashCode() != union.hashCode())
            fail("Incorrect hashCode computation.");

        Iterator e = union.iterator();
        while (e.hasNext())
            if (!intersection.remove(e.next()))
                fail("Couldn't remove element from copy.");
        if (!intersection.isEmpty())
            fail("Copy nonempty after deleting all elements.");

        e = union.iterator();
        while (e.hasNext()) {
            Object o = e.next();
            if (!union.contains(o))
                fail("Set doesn't contain one of its elements.");
            e.remove();
            if (union.contains(o))
                fail("Set contains element after deletion.");
        }
        if (!union.isEmpty())
            fail("Set nonempty after deleting all elements.");

        s1.clear();
        if (!s1.isEmpty())
            fail("Set nonempty after clear.");
    }

    // Done inefficiently so as to exercise toArray
    static <T> Set<T> clone(Set<T> s, Supplier<Set<T>> supplier) {
        Set<T> clone = supplier.get();
        List<T> arrayList = Arrays.asList((T[]) s.toArray());
        clone.addAll(arrayList);
        if (!s.equals(clone))
            fail("Set not equal to copy.");
        if (!s.containsAll(clone))
            fail("Set does not contain copy.");
        if (!clone.containsAll(s))
            fail("Copy does not contain set.");
        return clone;
    }

    static void AddRandoms(Set s, int n) {
        for (int i = 0; i < n; i++) {
            Integer e = rnd.nextInt(n);

            int preSize = s.size();
            boolean prePresent = s.contains(e);
            boolean added = s.add(e);
            if (!s.contains(e))
                fail("Element not present after addition.");
            if (added == prePresent)
                fail("added == alreadyPresent");
            int postSize = s.size();
            if (added && preSize == postSize)
                fail("Add returned true, but size didn't change.");
            if (!added && preSize != postSize)
                fail("Add returned false, but size changed.");
        }
    }

    @DataProvider(name = "Supplier<Set<Integer>>", parallel = true)
    public static Iterator<Object[]> navigableSetsProvider() {
        ArrayList<Object[]> iters = new ArrayList<>(makeCheckedSets());
        iters.ensureCapacity(numItr * iters.size());
        for (int each=1; each < numItr; each++) {
            iters.addAll(makeCheckedSets());
        }
        return iters.iterator();
    }

    public static Collection<Object[]> makeCheckedSets() {
        Object[][] params = {
            {"Collections.checkedSet(HashSet)",
             (Supplier) () -> Collections.checkedSet(new HashSet(), Integer.class)},
            {"Collections.checkedSet(TreeSet(reverseOrder))",
             (Supplier) () -> Collections.checkedSet(new TreeSet(Collections.reverseOrder()), Integer.class)},
            {"Collections.checkedSet(TreeSet.descendingSet())",
             (Supplier) () -> Collections.checkedSet(new TreeSet().descendingSet(), Integer.class)},
            {"Collections.checkedNavigableSet(TreeSet)",
             (Supplier) () -> Collections.checkedNavigableSet(new TreeSet(), Integer.class)},
            {"Collections.checkedNavigableSet(TreeSet(reverseOrder))",
             (Supplier) () -> Collections.checkedNavigableSet(new TreeSet(Collections.reverseOrder()), Integer.class)},
            {"Collections.checkedNavigableSet(TreeSet.descendingSet())",
             (Supplier) () -> Collections.checkedNavigableSet(new TreeSet().descendingSet(), Integer.class)},
        };
        return Arrays.asList(params);
    }
}
