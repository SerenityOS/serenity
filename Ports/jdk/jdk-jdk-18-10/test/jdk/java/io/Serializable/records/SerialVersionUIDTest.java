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
 * @summary Basic tests for SUID in the serial stream
 * @run testng SerialVersionUIDTest
 * @run testng/othervm/java.security.policy=empty_security.policy SerialVersionUIDTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.io.ObjectStreamConstants.*;
import static java.lang.System.out;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class SerialVersionUIDTest {

    record R1 () implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    record R2 (int x, int y) implements Serializable {
        private static final long serialVersionUID = 0L;
    }

    record R3 () implements Serializable { }

    record R4 (String s) implements Serializable { }

    record R5 (long l) implements Serializable {
        private static final long serialVersionUID = 5678L;
    }

    @DataProvider(name = "recordObjects")
    public Object[][] recordObjects() {
        return new Object[][] {
            new Object[] { new R1(),        1L    },
            new Object[] { new R2(1, 2),    0L    },
            new Object[] { new R3(),        0L    },
            new Object[] { new R4("s"),     0L    },
            new Object[] { new R5(7L),      5678L },
        };
    }

    /**
     * Tests that a declared SUID for a record class is inserted into the stream.
     */
    @Test(dataProvider = "recordObjects")
    public void testSerialize(Object objectToSerialize, long expectedUID)
        throws Exception
    {
        out.println("\n---");
        out.println("serializing : " + objectToSerialize);
        byte[] bytes = serialize(objectToSerialize);

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bais);

        // sanity
        assertEquals(dis.readShort(), STREAM_MAGIC);
        assertEquals(dis.readShort(), STREAM_VERSION);
        assertEquals(dis.readByte(), TC_OBJECT);
        assertEquals(dis.readByte(), TC_CLASSDESC);
        assertEquals(dis.readUTF(), objectToSerialize.getClass().getName());

        // verify that the UID is as expected
        assertEquals(dis.readLong(), expectedUID);
    }

    @DataProvider(name = "recordClasses")
    public Object[][] recordClasses() {
        List<Object[]> list = new ArrayList<>();
        List<Class<?>> recordClasses = List.of(R1.class, R2.class, R3.class, R4.class, R5.class);
        LongStream.of(0L, 1L, 100L, 10_000L, 1_000_000L).forEach(suid ->
                recordClasses.stream()
                             .map(cl -> new Object[] {cl, suid})
                             .forEach(list::add));
        return list.stream().toArray(Object[][]::new);
    }

    /**
     * Tests that matching of the serialVersionUID values ( stream value
     * and runtime class value ) is waived for record classes.
     */
    @Test(dataProvider = "recordClasses")
    public void testSerializeFromClass(Class<? extends Record> cl, long suid)
        throws Exception
    {
        out.println("\n---");
        byte[] bytes = byteStreamFor(cl.getName(), suid);
        Object obj = deserialize(bytes);
        assertEquals(obj.getClass(), cl);
        assertTrue(obj.getClass().isRecord());
    }

    // --- infra

    /**
     * Returns a stream of bytes for the given class and uid. The
     * stream will have no stream field values.
     */
    static byte[] byteStreamFor(String className, long uid)
        throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeShort(STREAM_MAGIC);
        dos.writeShort(STREAM_VERSION);
        dos.writeByte(TC_OBJECT);
        dos.writeByte(TC_CLASSDESC);
        dos.writeUTF(className);
        dos.writeLong(uid);
        dos.writeByte(SC_SERIALIZABLE);
        dos.writeShort(0);                // number of fields
        dos.writeByte(TC_ENDBLOCKDATA);   // no annotations
        dos.writeByte(TC_NULL);           // no superclasses
        dos.close();
        return baos.toByteArray();
    }

    static <T> byte[] serialize(T obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        return baos.toByteArray();
    }

    static <T> T deserialize(byte[] streamBytes)
        throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(streamBytes);
        ObjectInputStream ois  = new ObjectInputStream(bais);
        return (T) ois.readObject();
    }
}
