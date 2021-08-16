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
 * @summary Basic test that serializes and deserializes a number of records
 * @run testng BasicRecordSer
 * @run testng/othervm/java.security.policy=empty_security.policy BasicRecordSer
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.String.format;
import static java.lang.System.out;
import static java.net.InetAddress.getLoopbackAddress;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;
import static org.testng.Assert.fail;

/**
 * Basic test that serializes and deserializes a number of simple records.
 */
public class BasicRecordSer {

    // a mix of a few record and non-record classes

    record Empty () implements Serializable { }

    record Foo (int i) implements Serializable { }

    static class Bar implements Serializable {
        final Foo foo;
        final long along;
        Bar(Foo foo, long along) { this.foo = foo; this.along = along; }
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Bar))
                return false;
            Bar other = (Bar)obj;
            if (this.foo.equals(other.foo) && this.along == other.along)
                return true;
            return false;
        }
        @Override
        public String toString() {
            return format("Bar[foo=%s, along=%d]", foo, along);
        }
    }

    record Baz (Bar bar, float afloat, Foo foo) implements Serializable { }

    record Bat (Empty e1, Foo foo1, Bar bar1, float afloat, Foo foo2, Empty e2, Bar bar2)
        implements Serializable { }

    record Cheese<A, B>(A a, B b) implements Serializable { }

    interface ThrowingExternalizable extends Externalizable {
        default void writeExternal(ObjectOutput out) {
            fail("should not reach here");
        }
        default void readExternal(ObjectInput in) {
            fail("should not reach here");
        }
    }

    record Wibble () implements ThrowingExternalizable { }

    record Wobble (Foo foo) implements ThrowingExternalizable { }

    record Wubble (Wobble wobble, Wibble wibble, String s) implements ThrowingExternalizable { }

    @DataProvider(name = "serializable")
    public Object[][] serializable() {
        Foo foo = new Foo(23);
        return new Object[][] {
            new Object[] { new Empty()                                                       },
            new Object[] { new Foo(22)                                                       },
            new Object[] { new Foo[] { new Foo(24), new Foo(25) }                            },
            new Object[] { new Foo[] { foo, foo, foo, foo, foo  }                            },
            new Object[] { new Bar(new Foo(33), 1_234_567L)                                  },
            new Object[] { new Baz(new Bar(new Foo(44), 4_444L), 5.5f, new Foo(55))          },
            new Object[] { new Bat(new Empty(), new Foo(57), new Bar(new Foo(44), 4_444L),
                                   5.5f, new Foo(55), new Empty(), new Bar(new Foo(23), 1L)) },
            new Object[] { new Cheese(getLoopbackAddress(), BigInteger.valueOf(78))          },
            new Object[] { new Wibble()                                                      },
            new Object[] { new Wobble(new Foo(65))                                           },
            new Object[] { new Wubble(new Wobble(new Foo(6)), new Wibble(), "xxzzzyy")       },
        };
    }

    /** Tests serializing and deserializing a number of records. */
    @Test(dataProvider = "serializable")
    public void testSerializable(Object objToSerialize) throws Exception {
        out.println("\n---");
        out.println("serializing : " + objToSerialize);
        var objDeserialized = serializeDeserialize(objToSerialize);
        out.println("deserialized: " + objDeserialized);
        assertEquals(objToSerialize, objDeserialized);
        assertEquals(objDeserialized, objToSerialize);
    }

    /** Tests serializing and deserializing of local records. */
    @Test
    public void testLocalRecord() throws Exception {
        out.println("\n---");
        record Point(int x, int y) implements Serializable { }
        record Rectangle(Point bottomLeft, Point topRight) implements Serializable { }
        var objToSerialize = new Rectangle(new Point(0, 1), new Point (5, 6));
        out.println("serializing : " + objToSerialize);
        var objDeserialized = serializeDeserialize(objToSerialize);
        out.println("deserialized: " + objDeserialized);
        assertEquals(objToSerialize, objDeserialized);
        assertEquals(objDeserialized, objToSerialize);
    }

    /** Tests back references of Serializable record objects in the stream. */
    @Test
    public void testSerializableBackRefs() throws Exception {
        out.println("\n---");
        Foo foo = new Foo(32);
        Foo[] objToSerialize = new Foo[] { foo, foo, foo, foo, foo };
        out.println("serializing : " + objToSerialize);
        Foo[] objDeserialized = (Foo[])serializeDeserialize(objToSerialize);
        out.println("deserialized: " + objDeserialized);
        assertEquals(objToSerialize, objDeserialized);
        assertEquals(objDeserialized, objToSerialize);

        for (Foo f : objDeserialized)
            assertTrue(objDeserialized[0] == f);
    }

    /** Tests back references of Externalizable record objects in the stream. */
    @Test
    public void testExternalizableBackRefs() throws Exception {
        out.println("\n---");
        Foo foo = new Foo(33);
        Wobble wobble = new Wobble(foo);
        Wobble[] objToSerialize = new Wobble[] { wobble, wobble, wobble, wobble };
        out.println("serializing : " + objToSerialize);
        Wobble[] objDeserialized = (Wobble[])serializeDeserialize(objToSerialize);
        out.println("deserialized: " + objDeserialized);
        assertEquals(objToSerialize, objDeserialized);
        assertEquals(objDeserialized, objToSerialize);

        for (Wobble w : objDeserialized) {
            assertTrue(objDeserialized[0] == w);
            assertTrue(objDeserialized[0].foo() == w.foo());
        }
    }

    // --- Not Serializable

    record NotSerEmpty () { }
    record NotSer (int x) { }
    record NotSerA (int x, int y) {
        private static final long serialVersionUID = 5L;
    }
    static class A implements Serializable {
        final int y = -1;
        final NotSer notSer = new NotSer(7);
    }

    @DataProvider(name = "notSerializable")
    public Object[][] notSerializable() {
        return new Object[][] {
            new Object[] { new NotSerEmpty()                       },
            new Object[] { new NotSerEmpty[] { new NotSerEmpty() } },
            new Object[] { new Object[] { new NotSerEmpty() }      },
            new Object[] { new NotSer(6)                           },
            new Object[] { new NotSer[] { new NotSer(7) }          },
            new Object[] { new NotSerA(6, 8)                       },
            new Object[] { new A()                                 },
            new Object[] { new A[] { new A() }                     },
        };
    }

    static final Class<NotSerializableException> NSE = NotSerializableException.class;

    /** Tests that non-Serializable record objects throw NotSerializableException. */
    @Test(dataProvider = "notSerializable")
    public void testNotSerializable(Object objToSerialize) throws Exception {
        out.println("\n---");
        out.println("serializing : " + objToSerialize);
        NotSerializableException expected = expectThrows(NSE, () -> serialize(objToSerialize));
        out.println("caught expected NSE:" + expected);

    }

    // --- constructor invocation counting

    static volatile int e_ctrInvocationCount;

    record E () implements Serializable {
        public E() {  e_ctrInvocationCount++; }
    }

    /** Tests that the record's constructor is invoke exactly once per deserialization. */
    @Test
    public void testCtrCalledOnlyOnce() throws Exception {
        out.println("\n---");
        var objToSerialize = new E();
        e_ctrInvocationCount = 0;  // reset
        out.println("serializing : " + objToSerialize);
        var objDeserialized = serializeDeserialize(objToSerialize);
        out.println("deserialized: " + objDeserialized);
        assertEquals(objToSerialize, objDeserialized);
        assertEquals(objDeserialized, objToSerialize);
        assertEquals(e_ctrInvocationCount, 1);
    }

    // ---

    static volatile int g_ctrInvocationCount;

    record F (int x){
        public F(int x) {  this.x = x; g_ctrInvocationCount++; }
    }
    static class G implements Serializable {
        F f = new F(89);
    }

    /** Tests that the record's constructor is NOT invoke during failed deserialization. */
    @Test
    public void testCtrNotCalled() {
        out.println("\n---");
        var objToSerialize = new G();
        g_ctrInvocationCount = 0;  // reset
        out.println("serializing : " + objToSerialize);
        NotSerializableException expected = expectThrows(NSE, () -> serialize(objToSerialize));
        out.println("caught expected NSE:" + expected);
        assertEquals(g_ctrInvocationCount, 0);
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
