/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8079136
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run testng SubList
 * @summary Basic functionality of sublists
 * @key randomness
 */

import java.util.AbstractList;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Vector;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

import jdk.test.lib.RandomFactory;


public class SubList extends org.testng.Assert {

    final Random rnd = RandomFactory.getRandom();

    @Test(dataProvider = "modifiable")
    public void testAdd(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        Integer e = rnd.nextInt();
        subList.add(e);
        assertEquals(list.get(to), e);
        assertEquals(subList.size(), to - from + 1);
    }

    @Test(dataProvider = "modifiable",
          expectedExceptions = ConcurrentModificationException.class)
    public void testModAdd(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        list.add(42);
        subList.add(42);
    }

    @Test(dataProvider = "unresizable",
          expectedExceptions = UnsupportedOperationException.class)
    public void testUnmodAdd(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        subList.add(42);
    }

    @Test(dataProvider = "modifiable")
    public void testAddAtPos(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        int i = rnd.nextInt(1 + to - from);
        Integer e = rnd.nextInt();
        subList.add(i, e);
        assertEquals(list.get(from + i), e);
        assertEquals(subList.size(), to - from + 1);
    }

    @Test(dataProvider = "modifiable",
          expectedExceptions = ConcurrentModificationException.class)
    public void testModAddAtPos(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        list.add(42);
        int i = rnd.nextInt(1 + to - from);
        subList.add(i, 42);
    }

    @Test(dataProvider = "unresizable",
          expectedExceptions = UnsupportedOperationException.class)
    public void testUnmodAddAtPos(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        int i = rnd.nextInt(1 + to - from);
        subList.add(i, 42);
    }

    @Test(dataProvider = "modifiable")
    public void testClear(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        subList.clear();
        assertTrue(subList.isEmpty());
        assertEquals(subList.size(), 0);
    }

    @Test(dataProvider = "modifiable",
          expectedExceptions = ConcurrentModificationException.class)
    public void testModClear(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        list.add(42);
        subList.clear();
    }

    @Test(dataProvider = "unresizable",
          expectedExceptions = UnsupportedOperationException.class)
    public void testUnmodClear(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        subList.clear();
    }

    @Test(dataProvider = "all")
    public void testEquals(List<Integer> list, int from, int to) {
        List<Integer> subList1 = list.subList(from, to);
        List<Integer> subList2 = list.subList(from, to);
        assertTrue(subList1.equals(subList2));
        assertEquals(subList1.hashCode(), subList2.hashCode());
        for (int i = 0; i != 16; ++i) {
            int from3 = rnd.nextInt(1 + list.size());
            int to3 = from3 + rnd.nextInt(1 + list.size() - from3);
            boolean equal = (to - from) == (to3 - from3);
            for (int j = 0; j < to - from && j < to3 - from3; ++j)
                equal &= list.get(from + j) == list.get(from3 + j);
            List<Integer> subList3 = list.subList(from3, to3);
            assertEquals(subList1.equals(subList3), equal);
        }
    }

//    @Test(dataProvider = "modifiable",
//          expectedExceptions = ConcurrentModificationException.class)
//    public void testModEquals(List<Integer> list, int from, int to) {
//        List<Integer> subList = list.subList(from, to);
//        list.add(42);
//        subList.equals(subList);
//    }

    @Test(dataProvider = "modifiable",
          expectedExceptions = ConcurrentModificationException.class)
    public void testModHashCode(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        list.add(42);
        subList.hashCode();
    }

    @Test(dataProvider = "all")
    public void testGet(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        for (int i = 0; i < to - from; ++i)
            assertEquals(list.get(from + i), subList.get(i));
    }

    @Test(dataProvider = "modifiable",
          expectedExceptions = ConcurrentModificationException.class)
    public void testModGet(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        list.add(42);
        subList.get(from);
    }

    @Test(dataProvider = "all")
    public void testIndexOf(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        if (from < to) {
            Integer e = list.get(from);
            int j = subList.indexOf(e);
            assertTrue(j == 0);
        }
        for (int i = 0; i < list.size(); ++i) {
            Integer e = list.get(i);
            int j = subList.indexOf(e);
            if (i < from || i >= to) {
                assertTrue(j == -1 || subList.get(j) == e);
            } else {
                assertTrue(j >= 0);
                assertTrue(j <= i - from);
                assertEquals(subList.get(j), e);
            }
        }
        for (int i = 0; i < 16; ++i) {
            Integer r = rnd.nextInt();
            if (list.contains(r)) continue;
            int j = subList.indexOf(r);
            assertTrue(j == -1);
        }
    }

    @Test(dataProvider = "modifiable",
          expectedExceptions = ConcurrentModificationException.class)
    public void testModIndexOf(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        list.add(42);
        subList.indexOf(from);
    }

    @Test(dataProvider = "all")
    public void testIterator(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        Iterator<Integer> it = subList.iterator();
        for (int i = from; i < to; ++i) {
            assertTrue(it.hasNext());
            assertEquals(list.get(i), it.next());
        }
        assertFalse(it.hasNext());
    }

    @Test(dataProvider = "modifiable",
          expectedExceptions = ConcurrentModificationException.class)
    public void testModIteratorNext(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        Iterator<Integer> it = subList.iterator();
        list.add(42);
        it.next();
    }

    @Test(dataProvider = "modifiable")
    public void testIteratorRemove(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        Iterator<Integer> it = subList.iterator();
        for (int i = from; i < to; ++i) {
            assertTrue(it.hasNext());
            assertEquals(list.get(from), it.next());
            it.remove();
        }
        assertFalse(it.hasNext());
        assertTrue(subList.isEmpty());
    }

    @Test(dataProvider = "modifiable",
          expectedExceptions = ConcurrentModificationException.class)
    public void testModIteratorRemove(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        Iterator<Integer> it = subList.iterator();
        it.next();
        list.add(42);
        it.remove();
    }

    @Test(dataProvider = "unresizable",
          expectedExceptions = UnsupportedOperationException.class)
    public void testUnmodIteratorRemove(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        Iterator<Integer> it = subList.iterator();
        it.next();
        it.remove();
    }

    @Test(dataProvider = "all")
    public void testIteratorForEachRemaining(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        for (int k = 0; k < 16; ++k) {
            int r = from + rnd.nextInt(1 + to - from);
            Iterator<Integer> it = subList.iterator();
            for (int i = from; i < to; ++i) {
                assertTrue(it.hasNext());
                if (i == r) {
                    Iterator<Integer> jt = list.listIterator(r);
                    it.forEachRemaining(x ->
                        assertTrue(jt.hasNext() && x == jt.next()));
                    break;
                }
                assertEquals(list.get(i), it.next());
            }
            it.forEachRemaining(x -> fail());
        }
    }

    @Test(dataProvider = "all")
    public void testLastIndexOf(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        if (from < to) {
            Integer e = list.get(to - 1);
            int j = subList.lastIndexOf(e);
            assertTrue(j == to - from - 1);
        }
        for (int i = 0; i < list.size(); ++i) {
            Integer e = list.get(i);
            int j = subList.lastIndexOf(e);
            if (i < from || i >= to) {
                assertTrue(j == -1 || subList.get(j) == e);
            } else {
                assertTrue(j >= 0 && j >= i - from);
                assertEquals(subList.get(j), e);
            }
        }
        for (int i = 0; i < 16; ++i) {
            Integer r = rnd.nextInt();
            if (list.contains(r)) continue;
            int j = subList.lastIndexOf(r);
            assertTrue(j == -1);
        }
    }

    @Test(dataProvider = "modifiable",
          expectedExceptions = ConcurrentModificationException.class)
    public void testModLastIndexOf(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        list.add(42);
        subList.lastIndexOf(42);
    }

    @Test(dataProvider = "unresizable")
    public void testListIterator(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        ListIterator<Integer> it = subList.listIterator();
        for (int i = from; i < to; ++i) {
            assertTrue(it.hasNext());
            assertTrue(it.nextIndex() == i - from);
            assertEquals(list.get(i), it.next());
        }
        assertFalse(it.hasNext());
    }

    @Test(dataProvider = "modifiable",
          expectedExceptions = ConcurrentModificationException.class)
    public void testModListIteratorNext(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        ListIterator<Integer> it = subList.listIterator();
        list.add(42);
        it.next();
    }

    @Test(dataProvider = "modifiable")
    public void testListIteratorSet(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        ListIterator<Integer> it = subList.listIterator();
        for (int i = from; i < to; ++i) {
            assertTrue(it.hasNext());
            assertTrue(it.nextIndex() == i - from);
            assertEquals(list.get(i), it.next());
            Integer e = rnd.nextInt();
            it.set(e);
            assertEquals(list.get(i), e);
        }
        assertFalse(it.hasNext());
    }

    @Test(dataProvider = "modifiable",
          expectedExceptions = ConcurrentModificationException.class)
    public void testModListIteratorSet(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        ListIterator<Integer> it = subList.listIterator();
        it.next();
        list.add(42);
        it.set(42);
    }

    @Test(dataProvider = "unsettable",
          expectedExceptions = UnsupportedOperationException.class)
    public void testUnmodListIteratorSet(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        ListIterator<Integer> it = subList.listIterator();
        it.next();
        it.set(42);
    }

    @Test(dataProvider = "unresizable")
    public void testListIteratorPrevious(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        ListIterator<Integer> it = subList.listIterator(subList.size());
        for (int i = to - 1; i >= from; --i) {
            assertTrue(it.hasPrevious());
            assertTrue(it.previousIndex() == i - from);
            assertEquals(list.get(i), it.previous());
        }
        assertFalse(it.hasPrevious());
    }

    @Test(dataProvider = "modifiable",
          expectedExceptions = ConcurrentModificationException.class)
    public void testModListIteratorPrevious(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        ListIterator<Integer> it = subList.listIterator(to - from);
        list.add(42);
        it.previous();
    }

    @Test(dataProvider = "modifiable")
    public void testListIteratorSetPrevious(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        ListIterator<Integer> it = subList.listIterator(subList.size());
        for (int i = to - 1; i >= from; --i) {
            assertTrue(it.hasPrevious());
            assertTrue(it.previousIndex() == i - from);
            assertEquals(list.get(i), it.previous());
            Integer e = rnd.nextInt();
            it.set(e);
            assertEquals(list.get(i), e);
        }
        assertFalse(it.hasPrevious());
    }

    @Test(dataProvider = "unsettable",
          expectedExceptions = UnsupportedOperationException.class)
    public void testUnmodListIteratorSetPrevious(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        ListIterator<Integer> it = subList.listIterator(to - from);
        it.previous();
        it.set(42);
    }

    @Test(dataProvider = "modifiable")
    public void testListIteratorAdd(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        for (int i = 0; i < 16; ++i) {
            int r = rnd.nextInt(1 + subList.size());
            ListIterator<Integer> it = subList.listIterator(r);
            Integer e = rnd.nextInt();
            it.add(e);
            assertEquals(it.previous(), e);
            assertEquals(list.get(from + r), e);
        }
    }

    @Test(dataProvider = "unresizable",
          expectedExceptions = UnsupportedOperationException.class)
    public void testUnmodListIteratorAdd(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        int r = rnd.nextInt(1 + subList.size());
        ListIterator<Integer> it = subList.listIterator(r);
        it.add(42);
    }

    @Test(dataProvider = "modifiable",
          expectedExceptions = ConcurrentModificationException.class)
    public void testModListIteratorAdd(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        ListIterator<Integer> it = subList.listIterator();
        it.next();
        list.add(42);
        it.add(42);
    }

    @Test(dataProvider = "modifiable")
    public void testListIteratorRemoveNext(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        ListIterator<Integer> it = subList.listIterator();
        for (int i = from; i < to; ++i) {
            assertTrue(it.hasNext());
            assertTrue(it.nextIndex() == 0);
            assertEquals(list.get(from), it.next());
            it.remove();
        }
        assertFalse(it.hasNext());
        assertTrue(subList.isEmpty());
    }

    @Test(dataProvider = "unresizable",
          expectedExceptions = UnsupportedOperationException.class)
    public void testUnmodListIteratorRemoveNext(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        ListIterator<Integer> it = subList.listIterator();
        it.next();
        it.remove();
    }

    @Test(dataProvider = "modifiable",
          expectedExceptions = ConcurrentModificationException.class)
    public void testModListIteratorRemove(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        ListIterator<Integer> it = subList.listIterator();
        it.next();
        list.add(42);
        it.remove();
    }

    @Test(dataProvider = "modifiable")
    public void testListIteratorRemovePrevious(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        ListIterator<Integer> it = subList.listIterator(subList.size());
        for (int i = to - 1; i >= from; --i) {
            assertTrue(it.hasPrevious());
            assertTrue(it.previousIndex() == i - from);
            assertEquals(list.get(i), it.previous());
            it.remove();
        }
        assertFalse(it.hasPrevious());
        assertTrue(subList.isEmpty());
    }

    @Test(dataProvider = "unresizable",
          expectedExceptions = UnsupportedOperationException.class)
    public void testUnmodListIteratorRemovePrevious(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        ListIterator<Integer> it = subList.listIterator(subList.size());
        it.previous();
        it.remove();
    }

    @Test(dataProvider = "modifiable")
    public void testRemove(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        for (int i = 0; i < 16; ++i) {
            if (subList.isEmpty()) break;
            int r = rnd.nextInt(subList.size());
            Integer e = list.get(from + r);
            assertEquals(subList.remove(r), e);
        }
    }

    @Test(dataProvider = "unresizable",
          expectedExceptions = UnsupportedOperationException.class)
    public void testUnmodRemove(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        int r = rnd.nextInt(subList.size());
        subList.remove(r);
    }

    @Test(dataProvider = "modifiable",
          expectedExceptions = ConcurrentModificationException.class)
    public void testModRemove(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        list.add(42);
        subList.remove(0);
    }

    @Test(dataProvider = "modifiable")
    public void testSet(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        for (int i = 0; i < to - from; ++i) {
            Integer e0 = list.get(from + i);
            Integer e1 = rnd.nextInt();
            assertEquals(subList.set(i, e1), e0);
            assertEquals(list.get(from + i), e1);
        }
    }

    @Test(dataProvider = "modifiable",
          expectedExceptions = ConcurrentModificationException.class)
    public void testModSet(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        list.add(42);
        subList.set(0, 42);
    }

    @Test(dataProvider = "all")
    public void testSubList(List<Integer> list, int from, int to) {
        List<Integer> subList = list.subList(from, to);
        for (int i = 0; i < 16 && from < to; ++i) {
            int from1 = rnd.nextInt(to - from);
            int to1 = from1 + 1 + rnd.nextInt(to - from - from1);
            List<Integer> subSubList = subList.subList(from1, to1);
            for (int j = 0; j < to1 - from1; ++j)
                assertEquals(list.get(from + from1 + j), subSubList.get(j));
        }
    }

    /**
     * All kinds of lists
     */
    @DataProvider
    public static Object[][] all() {
        Object[][] l1 = modifiable();
        Object[][] l2 = unresizable();
        Object[][] res = Arrays.copyOf(l1, l1.length + l2.length);
        System.arraycopy(l2, 0, res, l1.length, l2.length);
        return res;
    }

    /**
     * Lists that allow any modifications: resizing and setting values
     */
    @DataProvider
    public static Object[][] modifiable() {
        final List<Integer> c1 = Arrays.asList(42);
        final List<Integer> c9 = Arrays.asList(40, 41, 42, 43, 44, 45, -1,
                Integer.MIN_VALUE, 1000500);

        return new Object[][] {
            {new ArrayList<>(c1), 0, 1},
            {new LinkedList<>(c1), 0, 1},
            {new Vector<>(c1), 0, 1},
            {new ArrayList<>(c1).subList(0, 1), 0, 1},
            {new LinkedList<>(c1).subList(0, 1), 0, 1},
            {new Vector<>(c1).subList(0, 1), 0, 1},
            {Collections.checkedList(new ArrayList<>(c1), Integer.class), 0, 1},
            {Collections.checkedList(new LinkedList<>(c1), Integer.class), 0, 1},
            {Collections.checkedList(new Vector<>(c1), Integer.class), 0, 1},
            {Collections.synchronizedList(new ArrayList<>(c1)), 0, 1},
            {Collections.synchronizedList(new LinkedList<>(c1)), 0, 1},
            {Collections.synchronizedList(new Vector<>(c1)), 0, 1},

            {new ArrayList<>(c9), 2, 5},
            {new LinkedList<>(c9), 2, 5},
            {new Vector<>(c9), 2, 5},
            {new ArrayList<>(c9).subList(1, 8), 1, 4},
            {new LinkedList<>(c9).subList(1, 8), 1, 4},
            {new Vector<>(c9).subList(1, 8), 1, 4},
            {Collections.checkedList(new ArrayList<>(c9), Integer.class), 2, 5},
            {Collections.checkedList(new LinkedList<>(c9), Integer.class), 2, 5},
            {Collections.checkedList(new Vector<>(c9), Integer.class), 2, 5},
            {Collections.synchronizedList(new ArrayList<>(c9)), 2, 5},
            {Collections.synchronizedList(new LinkedList<>(c9)), 2, 5},
            {Collections.synchronizedList(new Vector<>(c9)), 2, 5},
        };
    }

    /**
     * Lists that don't allow resizing, but allow setting values
     */
    @DataProvider
    public static Object[][] unresizable() {
        final List<Integer> c1 = Arrays.asList(42);
        final List<Integer> c9 = Arrays.asList(40, 41, 42, 43, 44, 45, -1,
                Integer.MIN_VALUE, 1000500);

        Object[][] l1 = unsettable();
        Object[][] l2 = {
            {c1, 0, 1},
            {c1.subList(0, 1), 0, 1},
            {Collections.checkedList(c1, Integer.class), 0, 1},
            {Collections.synchronizedList(c1), 0, 1},
            {c9, 0, 4},
            {c9, 4, 6},
            {c9.subList(1, 8), 1, 4},
            {c9.subList(1, 8), 0, 7},
            {Collections.checkedList(c9, Integer.class), 3, 6},
            {Collections.synchronizedList(c9), 3, 5},
        };
        Object[][] res = Arrays.copyOf(l1, l1.length + l2.length);
        System.arraycopy(l2, 0, res, l1.length, l2.length);
        return res;
    }

    /**
     * Lists that don't allow either resizing or setting values
     */
    @DataProvider
    public static Object[][] unsettable() {
        final List<Integer> c1 = Arrays.asList(42);
        final List<Integer> c9 = Arrays.asList(40, 41, 42, 43, 44, 45, -1,
                Integer.MIN_VALUE, 1000500);

        return new Object[][] {
            {new MyList(1), 0, 1},
            {new MyList(1).subList(0, 1), 0, 1},
            {Collections.singletonList(42), 0, 1},
            {Collections.singletonList(42).subList(0, 1), 0, 1},
            {Collections.unmodifiableList(c1), 0, 1},
            {Collections.unmodifiableList(new ArrayList<>(c1)), 0, 1},
            {Collections.unmodifiableList(new LinkedList<>(c1)), 0, 1},
            {Collections.unmodifiableList(new Vector<>(c1)), 0, 1},

            {new MyList(9), 3, 6},
            {new MyList(9).subList(2, 8), 3, 6},
            {Collections.unmodifiableList(c9), 3, 6},
            {Collections.unmodifiableList(new ArrayList<>(c9)), 3, 6},
            {Collections.unmodifiableList(new LinkedList<>(c9)), 3, 6},
            {Collections.unmodifiableList(new Vector<>(c9)), 3, 6},
        };
    }

    static class MyList extends AbstractList<Integer> {
        private int size;
        MyList(int s) { size = s; }
        public Integer get(int index) { return 42; }
        public int size() { return size; }
    }
}
