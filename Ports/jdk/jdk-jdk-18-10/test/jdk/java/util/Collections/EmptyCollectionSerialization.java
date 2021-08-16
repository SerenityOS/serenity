/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4684279 7129185
 * @summary Empty utility collections should be singletons
 * @author  Josh Bloch
 * @run testng EmptyCollectionSerialization
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Supplier;

import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

public class EmptyCollectionSerialization {
    private static Object patheticDeepCopy(Object o) throws Exception {
        // Serialize
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(o);
        byte[] serializedForm = bos.toByteArray();

        // Deserialize
        InputStream is = new ByteArrayInputStream(serializedForm);
        ObjectInputStream ois = new ObjectInputStream(is);
        return ois.readObject();
    }

    @Test(dataProvider="SerializableSingletons")
    public static void serializableSingletons(String description, Supplier<Object> o) {
        try {
            Object singleton = o.get();
            assertSame(o.get(), singleton, description + ": broken Supplier not returning singleton");
            Object copy = patheticDeepCopy(singleton);
            assertSame(copy, singleton, description + ": " +
                copy.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(copy)) +
                " is not the singleton " +
                singleton.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(singleton)));
        } catch (Exception all) {
            fail(description + ": Unexpected Exception", all);
        }
    }

    @DataProvider(name = "SerializableSingletons", parallel = true)
    public static Iterator<Object[]> navigableMapProvider() {
        return makeSingletons().iterator();
    }

    public static Collection<Object[]> makeSingletons() {
        Object[][] params = {
            {"Collections.EMPTY_LIST",
             (Supplier) () -> Collections.EMPTY_LIST},
            {"Collections.EMPTY_MAP",
             (Supplier) () -> Collections.EMPTY_MAP},
            {"Collections.EMPTY_SET",
             (Supplier) () -> Collections.EMPTY_SET},
            {"Collections.emptyList()",
             (Supplier) () -> Collections.emptyList()},
            {"Collections.emptyMap()",
             (Supplier) () -> Collections.emptyMap()},
            {"Collections.emptySet()",
             (Supplier) () -> Collections.emptySet()},
            {"Collections.emptySortedSet()",
             (Supplier) () -> Collections.emptySortedSet()},
            {"Collections.emptySortedMap()",
             (Supplier) () -> Collections.emptySortedMap()},
            {"Collections.emptyNavigableSet()",
             (Supplier) () -> Collections.emptyNavigableSet()},
            {"Collections.emptyNavigableMap()",
             (Supplier) () -> Collections.emptyNavigableMap()},
        };
        return Arrays.asList(params);
    }
}
