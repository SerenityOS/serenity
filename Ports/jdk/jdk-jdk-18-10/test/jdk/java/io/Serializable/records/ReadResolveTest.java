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
 * @summary Basic tests for readResolve
 * @run testng ReadResolveTest
 * @run testng/othervm/java.security.policy=empty_security.policy ReadResolveTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.String.format;
import static java.lang.System.out;
import static org.testng.Assert.assertEquals;

/**
 * Tests records being used as a serial proxy.
 */
public class ReadResolveTest {

    static class C1 implements Serializable {
        private final int x;
        private final int y;
        C1(int x, int y) { this.x = x; this.y = y; }
        private record SerProxy1(int x, int y) implements Serializable {
            @Serial
            private Object readResolve() { return new C1(x, y); }
        }
        @Serial
        private Object writeReplace() {
            return new SerProxy1(x, y);
        }
        @Override
        public boolean equals(Object obj) {
            return obj != null && obj instanceof C1 && ((C1)obj).x == this.x && ((C1)obj).y == y;
        }
        @Override
        public String toString() { return format("C1[x=%x, y=%d]", x, y); }
    }

    static class C2 implements Serializable {
        private record SerProxy2() implements Serializable {
            @Serial
            private Object readResolve() { return new C2(); }
        }
        @Serial
        private Object writeReplace() {
            return new SerProxy2();
        }
        @Override
        public boolean equals(Object obj) {
            return obj != null && obj instanceof C2;
        }
        @Override
        public String toString() { return "C2[]"; }
    }

    record R1 (int x, int y, String s) implements Serializable {
        private record SerProxy3(int a, int b, String c) implements Serializable {
            @Serial
            private Object readResolve() { return new R1(a, b, c); }
        }
        @Serial
        private Object writeReplace() {
            return new SerProxy3(x, y, s);
        }
    }

    @DataProvider(name = "objectsToSerialize")
    public Object[][] objectsToSerialize() {
        return new Object[][] {
                new Object[] { new C1(3,4)        },
                new Object[] { new C2()           },
                new Object[] { new R1(5, 6, "c")  }
        };
    }

    @Test(dataProvider = "objectsToSerialize")
    public void testSerialize(Object objectToSerialize) throws Exception {
        out.println("\n---");
        out.println("serializing : " + objectToSerialize);
        Object deserializedObj = serializeDeserialize(objectToSerialize);
        out.println("deserialized: " + deserializedObj);
        assertEquals(deserializedObj, objectToSerialize);
    }

    // -- null replacement

    record R10 () implements Serializable {
        @Serial
        private Object readResolve() {
            return null;
        }
    }

    @Test
    public void testNull() throws Exception {
        out.println("\n---");
        Object objectToSerialize = new R10();
        out.println("serializing : " + objectToSerialize);
        Object deserializedObj = serializeDeserialize(objectToSerialize);
        out.println("deserialized: " + deserializedObj);
        assertEquals(deserializedObj, null);
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

