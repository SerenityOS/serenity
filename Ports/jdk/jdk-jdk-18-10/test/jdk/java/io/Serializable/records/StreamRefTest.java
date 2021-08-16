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
 * @summary Tests for stream references
 * @run testng StreamRefTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/**
 * Tests for stream references.
 */
public class StreamRefTest {

    record A (int x) implements Serializable {
        public A(int x) {
            if (x < 0)
                throw new IllegalArgumentException("negative value for x:" + x);
            this.x = x;
        }
    }

    static class B implements Serializable {
        final A a ;
        B(A a) { this.a = a; }
    }

    record C (B b) implements Serializable {
        public C(B b) { this.b = b; }
    }

    static class D implements Serializable {
        final C c ;
        D(C c) { this.c = c; }
    }

    @Test
    public void basicRef() throws Exception {
        out.println("\n---");
        var a = new A(6);
        var b = new B(a);
        var c = new C(b);
        var d = new D(c);

        var bytes = serialize(a, b, c, d);

        A a1 = (A)deserializeOne(bytes);
        B b1 = (B)deserializeOne(bytes);
        C c1 = (C)deserializeOne(bytes);
        D d1 = (D)deserializeOne(bytes);

        assertTrue(a1.x == a.x);
        assertTrue(a1 == b1.a);
        assertTrue(b1 == c1.b);
        assertTrue(c1 == d1.c);
    }

    @Test
    public void reverseBasicRef() throws Exception {
        out.println("\n---");
        var a = new A(7);
        var b = new B(a);
        var c = new C(b);
        var d = new D(c);

        var bytes = serialize(d, c, b, a);

        D d1 = (D)deserializeOne(bytes);
        C c1 = (C)deserializeOne(bytes);
        B b1 = (B)deserializeOne(bytes);
        A a1 = (A)deserializeOne(bytes);

        assertTrue(a1 == b1.a);
        assertTrue(b1 == c1.b);
        assertTrue(c1 == d1.c);
    }

    static final Class<InvalidObjectException> IOE = InvalidObjectException.class;

    @Test
    public void basicRefWithInvalidA() throws Exception {
        out.println("\n---");
        var a = new A(3);
        var b = new B(a);

        var bytes = serializeToBytes(a, b);
        // injects a bad (negative) value for field x (of record A), in the stream
        updateIntValue(3, -3, bytes, 40);
        var byteStream = new ObjectInputStream(new ByteArrayInputStream(bytes));

        InvalidObjectException ioe = expectThrows(IOE, () -> deserializeOne(byteStream));
        out.println("caught expected IOE: " + ioe);
        Throwable t = ioe.getCause();
        assertTrue(t instanceof IllegalArgumentException, "Expected IAE, got:" + t);
        out.println("expected cause IAE: " + t);

        B b1 = (B)deserializeOne(byteStream);
        assertEquals(b1.a, null);
    }

    @Test
    public void reverseBasicRefWithInvalidA() throws Exception {
        out.println("\n---");
        var a = new A(3);
        var b = new B(a);

        var bytes = serializeToBytes(b, a);
        // injects a bad (negative) value for field x (of record A), in the stream
        updateIntValue(3, -3, bytes, 96);
        var byteStream = new ObjectInputStream(new ByteArrayInputStream(bytes));

        InvalidObjectException ioe = expectThrows(IOE, () -> deserializeOne(byteStream));
        out.println("caught expected IOE: " + ioe);
        Throwable t = ioe.getCause();
        assertTrue(t instanceof IllegalArgumentException, "Expected IAE, got:" + t);
        out.println("expected cause IAE: " + t);

        A a1 = (A)deserializeOne(byteStream);
        assertEquals(a1, null);
    }

    // ---

//    static class Y implements Serializable {
//        final int i = 10;
//        private void readObject(ObjectInputStream in)
//            throws IOException, ClassNotFoundException
//        {
//            in.defaultReadObject();
//            throw new IllegalArgumentException("dunno");
//        }
//    }
//
//    static class Z implements Serializable {
//        final Y y ;
//        Z(Y y) { this.y = y; }
//    }
//
//    static final Class<IllegalArgumentException> IAE = IllegalArgumentException.class;
//
//    @Test
//    public void whatDoesPlainDeserializationDo() throws Exception {
//        out.println("\n---");
//        var y = new Y();
//        var z = new Z(y);
//
//        var byteStream = serialize(z, y);
//
//        IllegalArgumentException iae = expectThrows(IAE, () -> deserializeOne(byteStream));
//        out.println("caught expected IAE: " + iae);
//        iae.printStackTrace();
//
//        Y y1 = (Y)deserializeOne(byteStream);
//        assertEquals(y1.i, 0);
//    }
//
//    @Test
//    public void reverseWhatDoesPlainDeserializationDo() throws Exception {
//        out.println("\n---");
//        var y = new Y();
//        var z = new Z(y);
//
//        var byteStream = serialize(y, z);
//
//        IllegalArgumentException iae = expectThrows(IAE, () -> deserializeOne(byteStream));
//        out.println("caught expected IAE: " + iae);
//        //iae.printStackTrace();
//
//        Z z1 = (Z)deserializeOne(byteStream);
//        assertEquals(z1.y, null);
//    }

    // ---

    static void assertExpectedIntValue(int expectedValue, byte[] bytes, int offset)
        throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes, offset, 4);
        DataInputStream dis = new DataInputStream(bais);
        assertEquals(dis.readInt(), expectedValue);
    }

    static void updateIntValue(int expectedValue, int newValue, byte[] bytes, int offset)
        throws IOException
    {
        assertExpectedIntValue(expectedValue, bytes, offset);
        bytes[offset + 0] = (byte)((newValue >>> 24) & 0xFF);
        bytes[offset + 1] = (byte)((newValue >>> 16) & 0xFF);
        bytes[offset + 2] = (byte)((newValue >>>  8) & 0xFF);
        bytes[offset + 3] = (byte)((newValue >>>  0) & 0xFF);
        assertExpectedIntValue(newValue, bytes, offset);
    }

    static ObjectInputStream serialize(Object... objs) throws IOException {
        return new ObjectInputStream(new ByteArrayInputStream(serializeToBytes(objs)));
    }

    static byte[] serializeToBytes(Object... objs) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        for (Object obj : objs)
            oos.writeObject(obj);
        oos.close();
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    static Object deserializeOne(ObjectInputStream ois)
        throws IOException, ClassNotFoundException
    {
        return ois.readObject();
    }
}
