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
 * @summary Basic tests for serializing and deserializing record classes
 * @run testng RecordClassTest
 * @run testng/othervm/java.security.policy=empty_security.policy RecordClassTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * Serializes and deserializes record classes. Ensures that the SUID is 0.
 */
public class RecordClassTest {

    record Foo () implements Serializable { }

    record Bar (int x) implements Serializable {
        private static final long serialVersionUID = 987654321L;
    }

    record Baz (Foo foo, Bar bar, int i) implements Serializable {  }

    interface ThrowingExternalizable extends Externalizable {
        default void writeExternal(ObjectOutput out) {
            fail("should not reach here");
        }
        default void readExternal(ObjectInput in) {
            fail("should not reach here");
        }
    }

    record Wibble () implements ThrowingExternalizable {
        private static final long serialVersionUID = 12345678L;
    }

    record Wobble (long l) implements ThrowingExternalizable { }

    record Wubble (Wobble wobble, Wibble wibble, String s) implements ThrowingExternalizable { }

    @DataProvider(name = "recordClasses")
    public Object[][] recordClasses() {
        return new Object[][] {
            new Object[] { Foo.class    , 0L         },
            new Object[] { Bar.class    , 987654321L },
            new Object[] { Baz.class    , 0L         },
            new Object[] { Wibble.class , 12345678L  },
            new Object[] { Wobble.class , 0L         },
            new Object[] { Wubble.class , 0L         },
        };
    }

    /** Tests that the serialized and deserialized instances are equal. */
    @Test(dataProvider = "recordClasses")
    public void testClassSerialization(Class<?> recordClass, long unused)
        throws Exception
    {
        out.println("\n---");
        out.println("serializing : " + recordClass);
        var deserializedClass = serializeDeserialize(recordClass);
        out.println("deserialized: " + deserializedClass);
        assertEquals(recordClass, deserializedClass);
        assertEquals(deserializedClass, recordClass);
    }

    /** Tests that the SUID is always 0 unless explicitly declared. */
    @Test(dataProvider = "recordClasses")
    public void testSerialVersionUID(Class<?> recordClass, long expectedUID) {
        out.println("\n---");
        ObjectStreamClass osc = ObjectStreamClass.lookup(recordClass);
        out.println("ObjectStreamClass::lookup  : " + osc);
        assertEquals(osc.getSerialVersionUID(), expectedUID);

        osc = ObjectStreamClass.lookupAny(recordClass);
        out.println("ObjectStreamClass::lookupAny: " + osc);
        assertEquals(osc.getSerialVersionUID(), expectedUID);
    }

    // --- not Serializable

    record NotSerializable1() { }

    record NotSerializable2(int x) { }

    record NotSerializable3<T>(T t) { }

    @DataProvider(name = "notSerRecordClasses")
    public Object[][] notSerRecordClasses() {
        return new Object[][] {
            new Object[] { NotSerializable1.class },
            new Object[] { NotSerializable2.class },
            new Object[] { NotSerializable3.class },
        };
    }

    /** Tests that the generated SUID is always 0 for all non-Serializable record classes. */
    @Test(dataProvider = "notSerRecordClasses")
    public void testSerialVersionUIDNonSer(Class<?> recordClass) {
        out.println("\n---");
        ObjectStreamClass osc = ObjectStreamClass.lookup(recordClass);
        out.println("ObjectStreamClass::lookup  : " + osc);
        assertEquals(osc, null);

        osc = ObjectStreamClass.lookupAny(recordClass);
        out.println("ObjectStreamClass::lookupAny: " + osc);
        assertEquals(osc.getSerialVersionUID(), 0L);
    }

    // --- infra

    static <T> byte[] serialize(T obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    static <T> T deserialize(byte[] streamBytes)
        throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(streamBytes);
        ObjectInputStream ois  = new ObjectInputStream(bais);
        return (T) ois.readObject();
    }

    static <T> T serializeDeserialize(T obj)
        throws IOException, ClassNotFoundException
    {
        return deserialize(serialize(obj));
    }
}
