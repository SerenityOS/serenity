/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8246774
 * @summary superclass fields in the stream should be discarded
 * @library /test/lib
 * @modules jdk.compiler
 * @compile AssignableFrom.java Point.java DefaultValues.java SuperStreamFields.java
 * @run testng SuperStreamFieldsTest
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static org.testng.Assert.assertEquals;

/**
 *  Tests that superclass fields in the stream are discarded.
 */
public class SuperStreamFieldsTest extends AbstractTest {

    @DataProvider(name = "plainInstances")
    public Object[][] plainInstances() {
        return new Object[][] {
            new Object[] { newPlainSuperStreamFields("cat", new int[] { 1 },    1)   },
            new Object[] { newPlainSuperStreamFields("dog", new int[] { 2, 3 }, 4)   },
            new Object[] { newPlainSuperStreamFields("rat", new int[] { 5, 6 }, 7)   },
        };
    }

    /** Serializes non-record (plain) instance, deserializes as a record. */
    @Test(dataProvider = "plainInstances")
    public void testPlainToRecord(SuperStreamFields objToSerialize) throws Exception {
        assert !objToSerialize.getClass().isRecord();
        out.println("serialize   : " + objToSerialize);
        byte[] bytes = serialize(objToSerialize);
        SuperStreamFields objDeserialized = deserializeAsRecord(bytes);
        assert objDeserialized.getClass().isRecord();
        out.println("deserialized: " + objDeserialized);

        assertEquals(objToSerialize.str(), objDeserialized.str());
        assertEquals(objToSerialize.x(),   objDeserialized.x());
        assertEquals(objToSerialize.y(),   objDeserialized.y());
    }

    @DataProvider(name = "recordInstances")
    public Object[][] recordInstances() {
        return new Object[][] {
            new Object[] { newRecordSuperStreamFields("goat",   new int[] { 56 },     66)   },
            new Object[] { newRecordSuperStreamFields("rabbit", new int[] { 22, 75 }, 77)   },
            new Object[] { newRecordSuperStreamFields("mouse",  new int[] { 12, 18 }, 88)   },
        };
    }

    /** Serializes record instance, deserializes as non-record (plain). */
    @Test(dataProvider = "recordInstances")
    public void testRecordToPlain(SuperStreamFields objToSerialize) throws Exception {
        assert objToSerialize.getClass().isRecord();
        out.println("serialize   : " + objToSerialize);
        byte[] bytes = serialize(objToSerialize);
        SuperStreamFields objDeserialized = deserializeAsPlain(bytes);
        assert !objDeserialized.getClass().isRecord();
        out.println("deserialized: " + objDeserialized);

        assertEquals(objToSerialize.str(), objDeserialized.str());
        assertEquals(objToSerialize.x(),   objDeserialized.x());
        assertEquals(objToSerialize.y(),   objDeserialized.y());

    }

    static SuperStreamFields newPlainSuperStreamFields(String str, int[] x, int y) {
        try {
            Class<?> c = Class.forName("SuperStreamFieldsImpl", true, plainLoader);
            var obj = (SuperStreamFields) c.getConstructor(String.class, int[].class, int.class)
                                           .newInstance(str, x, y);
            assert !obj.getClass().isRecord();
            return obj;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    static SuperStreamFields newRecordSuperStreamFields(String str, int[] x, int y) {
        try {
            Class<?> c = Class.forName("SuperStreamFieldsImpl", true, recordLoader);
            var obj = (SuperStreamFields) c.getConstructor(String.class, int[].class, int.class)
                                           .newInstance(str, x, y);
            assert obj.getClass().isRecord();
            return obj;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
