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
 * @summary Test for subtype stream field value assign-ability
 * @library /test/lib
 * @modules jdk.compiler
 * @compile AssignableFrom.java Point.java
 *          DefaultValues.java SuperStreamFields.java
 * @run testng AssignableFromTest
 */

import java.math.BigDecimal;
import java.math.BigInteger;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static org.testng.Assert.assertEquals;

/**
 * Basic test to check that stream field values that are not the exact
 * declared param/field type, but assignable to a declared supertype,
 * are bound/assigned correctly.
 */
public class AssignableFromTest extends AbstractTest {

    @DataProvider(name = "plainInstances")
    public Object[][] plainInstances() {
        return new Object[][] {
            new Object[] { newPlainAssignableFrom(Byte.valueOf((byte)11))   },
            new Object[] { newPlainAssignableFrom(Short.valueOf((short)22)) },
            new Object[] { newPlainAssignableFrom(Integer.valueOf(33))      },
            new Object[] { newPlainAssignableFrom(Long.valueOf(44))         },
            new Object[] { newPlainAssignableFrom(BigDecimal.valueOf(55))   },
            new Object[] { newPlainAssignableFrom(BigInteger.valueOf(66))   },
        };
    }

    /** Serialize non-record (plain) instances, deserialize as a record. */
    @Test(dataProvider = "plainInstances")
    public void testPlainToRecord(AssignableFrom objToSerialize) throws Exception {
        assert !objToSerialize.getClass().isRecord();
        out.println("serialize   : " + objToSerialize);
        byte[] bytes = serialize(objToSerialize);
        AssignableFrom objDeserialized = deserializeAsRecord(bytes);
        assert objDeserialized.getClass().isRecord();
        out.println("deserialized: " + objDeserialized);

        assertEquals(objToSerialize.number(), objDeserialized.number());
        assertEquals(objDeserialized.number(), objToSerialize.number());
        assertEquals(objDeserialized.number().getClass(), objDeserialized.number().getClass());

    }

    @DataProvider(name = "recordInstances")
    public Object[][] recordInstances() {
        return new Object[][] {
            new Object[] { newRecordAssignableFrom(Byte.valueOf((byte)21))   },
            new Object[] { newRecordAssignableFrom(Short.valueOf((short)32)) },
            new Object[] { newRecordAssignableFrom(Integer.valueOf(43))      },
            new Object[] { newRecordAssignableFrom(Long.valueOf(54))         },
            new Object[] { newRecordAssignableFrom(BigDecimal.valueOf(65))   },
            new Object[] { newRecordAssignableFrom(BigInteger.valueOf(76))   },
        };
    }

    /** Serialize record instances, deserialize as non-record (plain). */
    @Test(dataProvider = "recordInstances")
    public void testRecordToPlain(AssignableFrom objToSerialize) throws Exception {
        assert objToSerialize.getClass().isRecord();
        out.println("serialize   : " + objToSerialize);
        byte[] bytes = serialize(objToSerialize);
        AssignableFrom objDeserialized = deserializeAsPlain(bytes);
        assert !objDeserialized.getClass().isRecord();
        out.println("deserialized: " + objDeserialized);

        assertEquals(objToSerialize.number(), objDeserialized.number());
        assertEquals(objDeserialized.number(), objToSerialize.number());
        assertEquals(objDeserialized.number().getClass(), objDeserialized.number().getClass());
    }

    static AssignableFrom newPlainAssignableFrom(Number number) {
        try {
            Class<?> c = Class.forName("AssignableFromImpl", true, plainLoader);
            var obj = (AssignableFrom) c.getConstructor(Number.class).newInstance(number);
            assert !obj.getClass().isRecord();
            return obj;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    static AssignableFrom newRecordAssignableFrom(Number number) {
        try {
            Class<?> c = Class.forName("AssignableFromImpl", true, recordLoader);
            var obj = (AssignableFrom) c.getConstructor(Number.class).newInstance(number);
            assert obj.getClass().isRecord();
            return obj;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
