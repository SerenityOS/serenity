/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/*
 * @test
 * @bug 6904367
 * @summary IdentityHashMap reallocates storage when inserting expected
 *          number of elements
 * @modules java.base/java.util:open
 * @run testng Capacity
 * @key randomness
 */

@Test
public class Capacity {
    static final Field tableField;
    static final Random random = new Random();
    static final Object[][] sizesData;

    @DataProvider(name="sizes", parallel = true)
    public Object[][] sizesToTest() { return sizesData; }

    static {
        try {
            tableField = IdentityHashMap.class.getDeclaredField("table");
            tableField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new LinkageError("table", e);
        }

        ArrayList<Object[]> sizes = new ArrayList<>();
        for (int size = 0; size < 200; size++)
            sizes.add(new Object[] { size });

        // some numbers known to demonstrate bug 6904367
        for (int size : new int[] {682, 683, 1365, 2730, 2731, 5461})
            sizes.add(new Object[] { size });

        // a few more random sizes to try
        for (int i = 0; i != 128; i++)
            sizes.add(new Object[] { random.nextInt(5000) });

        sizesData = sizes.toArray(new Object[0][]);
    }

    static int capacity(IdentityHashMap<?,?> map) {
        try {
            return ((Object[]) tableField.get(map)).length / 2;
        } catch (Throwable t) {
            throw new LinkageError("table", t);
        }
    }

    static void assertCapacity(IdentityHashMap<?,?> map,
                               int expectedCapacity) {
        assertEquals(capacity(map), expectedCapacity);
    }

    static void growUsingPut(IdentityHashMap<Object,Object> map,
                             int elementsToAdd) {
        for (int i = 0; i < elementsToAdd; i++)
            map.put(new Object(), new Object());
    }

    static void growUsingPutAll(IdentityHashMap<Object,Object> map,
                                int elementsToAdd) {
        IdentityHashMap<Object,Object> other = new IdentityHashMap<>();
        growUsingPut(other, elementsToAdd);
        map.putAll(other);
    }

    static void growUsingRepeatedPutAll(IdentityHashMap<Object,Object> map,
                                        int elementsToAdd) {
        for (int i = 0; i < elementsToAdd; i++)
            map.putAll(Collections.singletonMap(new Object(),
                                                new Object()));
    }

    /**
     * Checks that expected number of items can be inserted into
     * the map without resizing of the internal storage
     */
    @Test(dataProvider = "sizes")
    public void canInsertExpectedItemsWithoutResizing(int size)
        throws Throwable {
        // First try growing using put()
        IdentityHashMap<Object,Object> m = new IdentityHashMap<>(size);
        int initialCapacity = capacity(m);
        growUsingPut(m, size);
        assertCapacity(m, initialCapacity);

        // Doubling from the expected size will cause exactly one
        // resize, except near minimum capacity.
        if (size > 1) {
            growUsingPut(m, size);
            assertCapacity(m, 2 * initialCapacity);
        }

        // Try again, growing with putAll()
        m = new IdentityHashMap<>(size);
        initialCapacity = capacity(m);
        growUsingPutAll(m, size);
        assertCapacity(m, initialCapacity);

        // Doubling from the expected size will cause exactly one
        // resize, except near minimum capacity.
        if (size > 1) {
            growUsingPutAll(m, size);
            assertCapacity(m, 2 * initialCapacity);
        }
    }

    /**
     * Given the expected size, computes such a number N of items that
     * inserting (N+1) items will trigger resizing of the internal storage
     */
    static int threshold(int size) throws Throwable {
        IdentityHashMap<Object,Object> m = new IdentityHashMap<>(size);
        int initialCapacity = capacity(m);
        while (capacity(m) == initialCapacity)
            growUsingPut(m, 1);
        return m.size() - 1;
    }

    /**
     * Checks that inserting (threshold+1) item causes resizing
     * of the internal storage
     */
    @Test(dataProvider = "sizes")
    public void passingThresholdCausesResize(int size) throws Throwable {
        final int threshold = threshold(size);
        IdentityHashMap<Object,Object> m = new IdentityHashMap<>(threshold);
        int initialCapacity = capacity(m);

        growUsingPut(m, threshold);
        assertCapacity(m, initialCapacity);

        growUsingPut(m, 1);
        assertCapacity(m, 2 * initialCapacity);
    }

    /**
     * Checks that 4 methods of requiring capacity lead to the same
     * internal capacity, unless sized below default capacity.
     */
    @Test(dataProvider = "sizes")
    public void differentGrowthPatternsResultInSameCapacity(int size)
        throws Throwable {
        if (size < 21)          // 21 is default maxExpectedSize
            return;

        IdentityHashMap<Object,Object> m;
        m = new IdentityHashMap<Object,Object>(size);
        int capacity1 = capacity(m);

        m = new IdentityHashMap<>();
        growUsingPut(m, size);
        int capacity2 = capacity(m);

        m = new IdentityHashMap<>();
        growUsingPutAll(m, size);
        int capacity3 = capacity(m);

        m = new IdentityHashMap<>();
        growUsingRepeatedPutAll(m, size);
        int capacity4 = capacity(m);

        if (capacity1 != capacity2 ||
            capacity2 != capacity3 ||
            capacity3 != capacity4)
            throw new AssertionError("Capacities not equal: "
                                     + capacity1 + " "
                                     + capacity2 + " "
                                     + capacity3 + " "
                                     + capacity4);
    }

    public void defaultExpectedMaxSizeIs21() {
        assertCapacity(new IdentityHashMap<Long,Long>(), 32);
        assertCapacity(new IdentityHashMap<Long,Long>(21), 32);
    }

    public void minimumCapacityIs4() {
        assertCapacity(new IdentityHashMap<Long,Long>(0), 4);
        assertCapacity(new IdentityHashMap<Long,Long>(1), 4);
        assertCapacity(new IdentityHashMap<Long,Long>(2), 4);
        assertCapacity(new IdentityHashMap<Long,Long>(3), 8);
    }

    @Test(enabled = false)
    /** needs too much memory to run normally */
    public void maximumCapacityIs2ToThe29() {
        assertCapacity(new IdentityHashMap<Long,Long>(Integer.MAX_VALUE),
                       1 << 29);
    }
}
