/*
 * Copyright 2016 Google, Inc.  All Rights Reserved.
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
 * @bug 8146568
 * @summary brittle white box test of internal array management
 * @modules java.base/java.util:open
 * @run testng ArrayManagement
 */

import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ArrayManagement {
    static final int DEFAULT_CAPACITY = 10;
    static final Field ELEMENT_DATA;
    static final Field MODCOUNT;
    static final SplittableRandom rnd = new SplittableRandom();

    static {
        try {
            ELEMENT_DATA = ArrayList.class.getDeclaredField("elementData");
            ELEMENT_DATA.setAccessible(true);
            MODCOUNT = AbstractList.class.getDeclaredField("modCount");
            MODCOUNT.setAccessible(true);
        } catch (ReflectiveOperationException huh) {
            throw new AssertionError(huh);
        }
    }

    static Object[] elementData(ArrayList<?> list) {
        try {
            return (Object[]) ELEMENT_DATA.get(list);
        } catch (ReflectiveOperationException huh) {
            throw new AssertionError(huh);
        }
    }

    static int modCount(ArrayList<?> list) {
        try {
            return MODCOUNT.getInt(list);
        } catch (ReflectiveOperationException huh) {
            throw new AssertionError(huh);
        }
    }

    static int capacity(ArrayList<?> list) {
        return elementData(list).length;
    }

    static int newCapacity(int oldCapacity) {
        return oldCapacity + (oldCapacity >> 1);
    }

    static void ensureCapacity(ArrayList<Object> list, int capacity) {
        int oldCapacity = capacity(list);
        int oldModCount = modCount(list);
        list.ensureCapacity(capacity);
        assertTrue(capacity(list) >= capacity || capacity(list) == 0);
        assertEquals(modCount(list),
                     (capacity(list) == oldCapacity)
                     ? oldModCount
                     : oldModCount + 1);
    }

    static List<Object> singletonList() {
        return Collections.singletonList(Boolean.TRUE);
    }

    /** Opportunistically randomly test various add operations. */
    static void addOneElement(ArrayList<Object> list) {
        int size = list.size();
        int modCount = modCount(list);
        switch (rnd.nextInt(4)) {
        case 0: assertTrue(list.add(Boolean.TRUE)); break;
        case 1: list.add(size, Boolean.TRUE); break;
        case 2: assertTrue(list.addAll(singletonList())); break;
        case 3: assertTrue(list.addAll(size, singletonList())); break;
        default: throw new AssertionError();
        }
        assertEquals(modCount(list), modCount + 1);
        assertEquals(list.size(), size + 1);
    }

    @Test public void defaultCapacity() {
        ArrayList<Object> list = new ArrayList<>();
        assertEquals(capacity(new ArrayList<Object>()), 0);
        for (int i = 0; i < DEFAULT_CAPACITY; i++) {
            addOneElement(list);
            assertEquals(capacity(list), DEFAULT_CAPACITY);
        }
        addOneElement(list);
        assertEquals(capacity(list), newCapacity(DEFAULT_CAPACITY));
    }

    @Test public void defaultCapacityEnsureCapacity() {
        ArrayList<Object> list = new ArrayList<>();
        for (int i = 0; i <= DEFAULT_CAPACITY; i++) {
            ensureCapacity(list, i);     // no-op!
            assertEquals(capacity(list), 0);
        }
        for (int i = 0; i < DEFAULT_CAPACITY; i++) {
            addOneElement(list);
            assertEquals(capacity(list), DEFAULT_CAPACITY);
        }
        addOneElement(list);
        assertEquals(capacity(list), newCapacity(DEFAULT_CAPACITY));
        {
            int capacity = capacity(list);
            ensureCapacity(list, capacity + 1);
            assertEquals(capacity(list), newCapacity(capacity));
        }
        {
            int capacity = capacity(list);
            ensureCapacity(list, 3 * capacity);
            assertEquals(capacity(list), 3 * capacity);
        }
    }

    @Test public void ensureCapacityBeyondDefaultCapacity() {
        ArrayList<Object> list = new ArrayList<>();
        list.ensureCapacity(DEFAULT_CAPACITY + 1);
        assertEquals(capacity(list), DEFAULT_CAPACITY + 1);
        for (int i = 0; i < DEFAULT_CAPACITY + 1; i++) {
            addOneElement(list);
            assertEquals(capacity(list), DEFAULT_CAPACITY + 1);
        }
        addOneElement(list);
        assertEquals(capacity(list), newCapacity(DEFAULT_CAPACITY + 1));
    }

    @Test public void explicitZeroCapacity() {
        ArrayList<Object> list = new ArrayList<>(0);
        assertEquals(capacity(list), 0);
        addOneElement(list);
        assertEquals(capacity(list), 1);
        addOneElement(list);
        assertEquals(capacity(list), 2);
        addOneElement(list);
        assertEquals(capacity(list), 3);
        addOneElement(list);
        assertEquals(capacity(list), 4);
        addOneElement(list);
        assertEquals(capacity(list), 6);
        addOneElement(list);
        assertEquals(capacity(list), 6);
        addOneElement(list);
        assertEquals(capacity(list), 9);
        list.clear();
        assertEquals(capacity(list), 9);
    }

    @Test public void explicitLargeCapacity() {
        int n = DEFAULT_CAPACITY * 3;
        ArrayList<Object> list = new ArrayList<>(n);
        assertEquals(capacity(list), n);
        ensureCapacity(list, 0);
        ensureCapacity(list, n);
        for (int i = 0; i < n; i++) addOneElement(list);
        assertEquals(capacity(list), n);

        addOneElement(list);
        assertEquals(capacity(list), newCapacity(n));
    }

    @Test public void emptyArraysAreShared() {
        assertSame(elementData(new ArrayList<Object>()),
                   elementData(new ArrayList<Object>()));
        assertSame(elementData(new ArrayList<Object>(0)),
                   elementData(new ArrayList<Object>(0)));
    }

    @Test public void emptyArraysDifferBetweenDefaultAndExplicit() {
        assertNotSame(elementData(new ArrayList<Object>()),
                      elementData(new ArrayList<Object>(0)));
    }

    @Test public void negativeCapacity() {
        for (int capacity : new int[] { -1, Integer.MIN_VALUE }) {
            try {
                new ArrayList<Object>(capacity);
                fail("should throw");
            } catch (IllegalArgumentException success) {}
        }
    }
}
