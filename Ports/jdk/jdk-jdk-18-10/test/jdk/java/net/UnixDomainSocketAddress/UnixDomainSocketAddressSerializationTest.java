/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.net.UnixDomainSocketAddress;
import java.nio.file.Path;
import static java.io.ObjectStreamConstants.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/*
 * @test
 * @summary UnixDomainSocketAddress serialization test
 * @run testng/othervm UnixDomainSocketAddressSerializationTest
 */

@Test
public class UnixDomainSocketAddressSerializationTest {
    private static final UnixDomainSocketAddress addr =
            UnixDomainSocketAddress.of(Path.of("test.sock"));

    public static void test() throws Exception {
        assertTrue(addr instanceof Serializable);

        byte[] serialized = serialize(addr);
        assertTrue(serialized.length > 0);

        UnixDomainSocketAddress deserialized =
                deserialize(serialized, UnixDomainSocketAddress.class);
        assertEquals(deserialized.getPath(), addr.getPath());
        assertEquals(deserialized.toString(), addr.toString());
        assertEquals(deserialized.hashCode(), addr.hashCode());
        assertEquals(deserialized, addr);
    }

    static final Class<InvalidObjectException> IOE = InvalidObjectException.class;
    static final Class<NullPointerException> NPE = NullPointerException.class;

    /** Tests that UnixDomainSocketAddress in the byte-stream is disallowed. */
    public static void testUnixDomainSocketAddressInStream() throws Exception {
        long suid = ObjectStreamClass.lookup(UnixDomainSocketAddress.class).getSerialVersionUID();
        byte[] bytes = byteStreamFor(UnixDomainSocketAddress.class.getName(), suid);
        expectThrows(IOE, () -> deserialize(bytes, UnixDomainSocketAddress.class));
    }

    /** Tests that SerialProxy with a null/absent path value in the byte-stream is disallowed. */
    public static void testSerialProxyNoStreamValues() throws Exception {
        Class<?> c = Class.forName("java.net.UnixDomainSocketAddress$Ser");
        long suid = ObjectStreamClass.lookup(c).getSerialVersionUID();
        byte[] bytes = byteStreamFor(c.getName(), suid);
        expectThrows(NPE, () -> deserialize(bytes, UnixDomainSocketAddress.class));
    }

    private static <T extends Serializable> byte[] serialize(T t)
            throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(t);
        oos.flush();
        oos.close();
        return bos.toByteArray();
    }

    private static <T extends Serializable> T deserialize(byte[] b, Class<T> cl)
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois =
                     new ObjectInputStream(new ByteArrayInputStream(b))) {
            Object o = ois.readObject();
            return cl.cast(o);
        }
    }

    /**
     * Returns a stream with the given classname and suid. The stream will have
     * no stream field values.
     */
    static byte[] byteStreamFor(String classname, long suid) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeShort(STREAM_MAGIC);
        dos.writeShort(STREAM_VERSION);
        dos.writeByte(TC_OBJECT);
        dos.writeByte(TC_CLASSDESC);
        dos.writeUTF(classname);
        dos.writeLong(suid);
        dos.writeByte(SC_SERIALIZABLE);
        dos.writeShort(0);                // number of stream fields
        dos.writeByte(TC_ENDBLOCKDATA);   // no annotations
        dos.writeByte(TC_NULL);           // no superclasses
        dos.write(TC_ENDBLOCKDATA);       // end block - for SC_WRITE_METHOD
        dos.close();
        return baos.toByteArray();
    }
}
