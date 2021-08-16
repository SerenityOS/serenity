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

/*
 * @test
 * @bug 8011200
 * @run testng BasicSerialization
 * @summary Ensure Maps can be serialized and deserialized.
 * @author Mike Duigou
 */
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import static org.testng.Assert.fail;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;

public class BasicSerialization {

    enum IntegerEnum {

        e0, e1, e2, e3, e4, e5, e6, e7, e8, e9,
        e10, e11, e12, e13, e14, e15, e16, e17, e18, e19,
        e20, e21, e22, e23, e24, e25, e26, e27, e28, e29,
        e30, e31, e32, e33, e34, e35, e36, e37, e38, e39,
        e40, e41, e42, e43, e44, e45, e46, e47, e48, e49,
        e50, e51, e52, e53, e54, e55, e56, e57, e58, e59,
        e60, e61, e62, e63, e64, e65, e66, e67, e68, e69,
        e70, e71, e72, e73, e74, e75, e76, e77, e78, e79,
        e80, e81, e82, e83, e84, e85, e86, e87, e88, e89,
        e90, e91, e92, e93, e94, e95, e96, e97, e98, e99,
        EXTRA_KEY;
        public static final int SIZE = values().length;
    };
    private static final int TEST_SIZE = IntegerEnum.SIZE - 1;
    /**
     * Realized keys ensure that there is always a hard ref to all test objects.
     */
    private static final IntegerEnum[] KEYS = new IntegerEnum[TEST_SIZE];
    /**
     * Realized values ensure that there is always a hard ref to all test
     * objects.
     */
    private static final String[] VALUES = new String[TEST_SIZE];

    static {
        IntegerEnum[] keys = IntegerEnum.values();
        for (int each = 0; each < TEST_SIZE; each++) {
            KEYS[each] = keys[each];
            VALUES[each] = keys[each].name();
        }
    }
    private static final IntegerEnum EXTRA_KEY = IntegerEnum.EXTRA_KEY;
    private static final String EXTRA_VALUE = IntegerEnum.EXTRA_KEY.name();

    public static <K, V> Map<K, V> mapClone(Map<K, V> map) {
        Method cloneMethod;

        try {
            cloneMethod = map.getClass().getMethod("clone", new Class[]{});
        } catch (NoSuchMethodException | SecurityException all) {
            cloneMethod = null;
        }

        if (null != cloneMethod) {
            try {
                Map<K, V> result = (Map<K, V>)cloneMethod.invoke(map, new Object[]{});
                return result;
            } catch (Exception all) {
                fail("clone() failed " + map.getClass().getSimpleName(), all);
                return null;
            }
        } else {
            Constructor<? extends Map> copyConstructor;
            try {
                copyConstructor = (Constructor<? extends Map>)map.getClass().getConstructor(new Class[]{Map.class});

                Map<K, V> result = (Map<K, V>)copyConstructor.newInstance(new Object[]{map});

                return result;
            } catch (Exception all) {
                return serialClone(map);
            }
        }
    }

    @Test(dataProvider = "Map<IntegerEnum,String>")
    public void testSerialization(String description, Map<IntegerEnum, String> map) {
        Object foo = new Object();

        Map<IntegerEnum, String> clone = mapClone(map);
        Map<IntegerEnum, String> serialClone = serialClone(map);

        assertEquals(map, map, description + ":should equal self");
        assertEquals(clone, map, description + ":should equal clone");
        assertEquals(map, clone, description + ": should equal orginal map");
        assertEquals(serialClone, map, description + ": should equal deserialized clone");
        assertEquals(map, serialClone, description + ": should equal original map");
        assertEquals(serialClone, clone, description + ": deserialized clone should equal clone");
        assertEquals(clone, serialClone, description + ": clone should equal deserialized clone");

        assertFalse(map.containsKey(EXTRA_KEY), description + ":unexpected key");
        assertFalse(clone.containsKey(EXTRA_KEY), description + ":unexpected key");
        assertFalse(serialClone.containsKey(EXTRA_KEY), description + ":unexpected key");
        map.put(EXTRA_KEY, EXTRA_VALUE);
        clone.put(EXTRA_KEY, EXTRA_VALUE);
        serialClone.put(EXTRA_KEY, EXTRA_VALUE);
        assertTrue(map.containsKey(EXTRA_KEY), description + ":missing key");
        assertTrue(clone.containsKey(EXTRA_KEY), description + ":missing key");
        assertTrue(serialClone.containsKey(EXTRA_KEY), description + ":missing key");
        assertSame(map.get(EXTRA_KEY), EXTRA_VALUE, description + ":wrong value");
        assertSame(clone.get(EXTRA_KEY), EXTRA_VALUE, description + ":wrong value");
        assertSame(serialClone.get(EXTRA_KEY), EXTRA_VALUE, description + ":wrong value");

        assertEquals(map, map, description + ":should equal self");
        assertEquals(clone, map, description + ":should equal clone");
        assertEquals(map, clone, description + ": should equal orginal map");
        assertEquals(serialClone, map, description + ": should equal deserialized clone");
        assertEquals(map, serialClone, description + ": should equal original map");
        assertEquals(serialClone, clone, description + ": deserialized clone should equal clone");
        assertEquals(clone, serialClone, description + ": clone should equal deserialized clone");
    }

    static byte[] serializedForm(Object obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(obj);
            return baos.toByteArray();
        } catch (IOException e) {
            fail("Unexpected Exception", e);
            return null;
        }
    }

    static Object readObject(byte[] bytes) throws IOException, ClassNotFoundException {
        InputStream is = new ByteArrayInputStream(bytes);
        return new ObjectInputStream(is).readObject();
    }

    @SuppressWarnings("unchecked")
    static <T> T serialClone(T obj) {
        try {
            return (T)readObject(serializedForm(obj));
        } catch (IOException | ClassNotFoundException e) {
            fail("Unexpected Exception", e);
            return null;
        }
    }

    @DataProvider(name = "Map<IntegerEnum,String>", parallel = true)
    private static Iterator<Object[]> makeMaps() {
        return Arrays.asList(
            // empty
            new Object[]{"HashMap", new HashMap()},
            new Object[]{"LinkedHashMap", new LinkedHashMap()},
            new Object[]{"Collections.checkedMap(HashMap)", Collections.checkedMap(new HashMap(), IntegerEnum.class, String.class)},
            new Object[]{"Collections.synchronizedMap(HashMap)", Collections.synchronizedMap(new HashMap())},
            // null hostile
            new Object[]{"EnumMap", new EnumMap(IntegerEnum.class)},
            new Object[]{"Hashtable", new Hashtable()},
            new Object[]{"TreeMap", new TreeMap()},
            new Object[]{"ConcurrentHashMap", new ConcurrentHashMap()},
            new Object[]{"ConcurrentSkipListMap", new ConcurrentSkipListMap()},
            new Object[]{"Collections.checkedMap(ConcurrentHashMap)", Collections.checkedMap(new ConcurrentHashMap(), IntegerEnum.class, String.class)},
            new Object[]{"Collections.synchronizedMap(EnumMap)", Collections.synchronizedMap(new EnumMap(IntegerEnum.class))},
            // filled
            new Object[]{"HashMap", fillMap(new HashMap())},
            new Object[]{"LinkedHashMap", fillMap(new LinkedHashMap())},
            new Object[]{"Collections.checkedMap(HashMap)", Collections.checkedMap(fillMap(new HashMap()), IntegerEnum.class, String.class)},
            new Object[]{"Collections.synchronizedMap(HashMap)", Collections.synchronizedMap(fillMap(new HashMap()))},
            // null hostile
            new Object[]{"EnumMap", fillMap(new EnumMap(IntegerEnum.class))},
            new Object[]{"Hashtable", fillMap(new Hashtable())},
            new Object[]{"TreeMap", fillMap(new TreeMap())},
            new Object[]{"ConcurrentHashMap", fillMap(new ConcurrentHashMap())},
            new Object[]{"ConcurrentSkipListMap", fillMap(new ConcurrentSkipListMap())},
            new Object[]{"Collections.checkedMap(ConcurrentHashMap)", Collections.checkedMap(fillMap(new ConcurrentHashMap()), IntegerEnum.class, String.class)},
            new Object[]{"Collections.synchronizedMap(EnumMap)", Collections.synchronizedMap(fillMap(new EnumMap(IntegerEnum.class)))}).iterator();
    }

    private static Map<IntegerEnum, String> fillMap(Map<IntegerEnum, String> result) {
        for (int each = 0; each < TEST_SIZE; each++) {
            result.put(KEYS[each], VALUES[each]);
        }

        return result;
    }
}
