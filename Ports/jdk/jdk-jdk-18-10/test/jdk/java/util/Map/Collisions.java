/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7126277
 * @run testng/othervm -Dtest.map.collisions.shortrun=true Collisions
 * @summary Ensure Maps behave well with lots of hashCode() collisions.
 */
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

import org.testng.annotations.Test;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class Collisions extends MapWithCollisionsProviders {

    @Test(dataProvider = "mapsWithObjects")
    void testIntegerIteration(String desc, Supplier<Map<IntKey, IntKey>> ms, IntKey val) {
        Map<IntKey, IntKey> map = ms.get();
        int mapSize = map.size();

        BitSet all = new BitSet(mapSize);
        for (Map.Entry<IntKey, IntKey> each : map.entrySet()) {
            assertFalse(all.get(each.getKey().getValue()), "Iteration: key already seen");
            all.set(each.getKey().getValue());
        }

        all.flip(0, mapSize);
        assertTrue(all.isEmpty(), "Iteration: some keys not visited");

        for (IntKey each : map.keySet()) {
            assertFalse(all.get(each.getValue()), "Iteration: key already seen");
            all.set(each.getValue());
        }

        all.flip(0, mapSize);
        assertTrue(all.isEmpty(), "Iteration: some keys not visited");

        int count = 0;
        for (IntKey each : map.values()) {
            count++;
        }

        assertEquals(map.size(), count,
                String.format("Iteration: value count matches size m%d != c%d", map.size(), count));
    }

    @Test(dataProvider = "mapsWithStrings")
    void testStringIteration(String desc, Supplier<Map<String, String>> ms, String val) {
        Map<String, String> map = ms.get();
        int mapSize = map.size();

        BitSet all = new BitSet(mapSize);
        for (Map.Entry<String, String> each : map.entrySet()) {
            String key = each.getKey();
            boolean longKey = key.length() > 5;
            int index = key.hashCode() + (longKey ? mapSize / 2 : 0);
            assertFalse(all.get(index), "key already seen");
            all.set(index);
        }

        all.flip(0, mapSize);
        assertTrue(all.isEmpty(), "some keys not visited");

        for (String each : map.keySet()) {
            boolean longKey = each.length() > 5;
            int index = each.hashCode() + (longKey ? mapSize / 2 : 0);
            assertFalse(all.get(index), "key already seen");
            all.set(index);
        }

        all.flip(0, mapSize);
        assertTrue(all.isEmpty(), "some keys not visited");

        int count = 0;
        for (String each : map.values()) {
            count++;
        }

        assertEquals(map.size(), mapSize,
                String.format("value count matches size m%d != k%d", map.size(), mapSize));
    }

    @Test(dataProvider = "mapsWithObjectsAndStrings")
    void testRemove(String desc, Supplier<Map<Object, Object>> ms, Object val) {
        Map<Object, Object> map = ms.get();
        Object[] keys = map.keySet().toArray();

        for (int i = 0; i < keys.length; i++) {
            Object each = keys[i];
            assertNotNull(map.remove(each),
                    String.format("remove: %s[%d]%s", desc, i, each));
        }

        assertTrue(map.size() == 0 && map.isEmpty(),
                String.format("remove: map empty. size=%d", map.size()));
    }

    @Test(dataProvider = "mapsWithObjectsAndStrings")
    void testKeysIteratorRemove(String desc, Supplier<Map<Object, Object>> ms, Object val) {
        Map<Object, Object> map = ms.get();

        Iterator<Object> each = map.keySet().iterator();
        while (each.hasNext()) {
            Object t = each.next();
            each.remove();
            assertFalse(map.containsKey(t), String.format("not removed: %s", each));
        }

        assertTrue(map.size() == 0 && map.isEmpty(),
                String.format("remove: map empty. size=%d", map.size()));
    }

    @Test(dataProvider = "mapsWithObjectsAndStrings")
    void testValuesIteratorRemove(String desc, Supplier<Map<Object, Object>> ms, Object val) {
        Map<Object, Object> map = ms.get();

        Iterator<Object> each = map.values().iterator();
        while (each.hasNext()) {
            Object t = each.next();
            each.remove();
            assertFalse(map.containsValue(t), String.format("not removed: %s", each));
        }

        assertTrue(map.size() == 0 && map.isEmpty(),
                String.format("remove: map empty. size=%d", map.size()));
    }

    @Test(dataProvider = "mapsWithObjectsAndStrings")
    void testEntriesIteratorRemove(String desc, Supplier<Map<Object, Object>> ms, Object val) {
        Map<Object, Object> map = ms.get();

        Iterator<Map.Entry<Object, Object>> each = map.entrySet().iterator();
        while (each.hasNext()) {
            Map.Entry<Object, Object> t = each.next();
            Object key = t.getKey();
            Object value = t.getValue();
            each.remove();
            assertTrue((map instanceof IdentityHashMap) || !map.entrySet().contains(t),
                    String.format("not removed: %s", each));
            assertFalse(map.containsKey(key),
                    String.format("not removed: %s", each));
            assertFalse(map.containsValue(value),
                    String.format("not removed: %s", each));
        }

        assertTrue(map.size() == 0 && map.isEmpty(),
                String.format("remove: map empty. size=%d", map.size()));
    }

}
