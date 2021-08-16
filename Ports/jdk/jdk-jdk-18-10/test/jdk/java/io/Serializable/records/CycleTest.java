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
 * @summary Ensures basic behavior of cycles from record components
 * @run testng CycleTest
 * @run testng/othervm/java.security.policy=empty_security.policy CycleTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CycleTest {

    record R (int x, int y, C c) implements Serializable { }

    static class C implements Serializable {
        Object obj;  // mutable
    }

    /**
     * Deserialization of a record object, r, does not support references to r,
     * from any of r's components. All references will be null.
     */
    @Test
    public void testCycle1() throws Exception  {
        out.println("\n---");
        C c = new C();
        R r = new R(1, 2, c);
        c.obj = r;  // cycle, targeting record r

        out.println("serializing : " + r);
        R deserializedObj = serializeDeserialize(r);
        out.println("deserialized: " + deserializedObj);
        assertEquals(deserializedObj.x(), 1);           // sanity
        assertEquals(deserializedObj.y(), 2);           // sanity
        assertTrue(deserializedObj.c() instanceof C);   // sanity
        assertEquals(deserializedObj.c().obj, null);    // cycle, expect null
    }

    /**
     * An object, c, reconstructed before the record, r, can be referenced from
     * a record component. It's a cycle, from within the record, but the record,
     * r, is not the target.
     */
    @Test
    public void testCycle2() throws Exception  {
        out.println("\n---");
        C c = new C();
        R r = new R(3, 4, c);
        c.obj = r;  // cycle, serializing c first should preserve the cycle

        out.println("serializing : " + c);
        C deserializedObj = serializeDeserialize(c);
        out.println("deserialized: " + deserializedObj);
        assertTrue(deserializedObj instanceof C);         // sanity
        assertTrue(deserializedObj.obj != null);          // expect non-null, r
        assertEquals(((R)deserializedObj.obj).x(), 3);    // sanity
        assertEquals(((R)deserializedObj.obj).y(), 4);    // sanity
    }

    record R2 (int x, int y, C c1, C c2) implements Serializable { }

    /**
     * Cycles, of non-record objects, within record components should be fine.
     */
    @Test
    public void testCycle3() throws Exception {
        out.println("\n---");
        C c1 = new C();
        C c2 = new C();
        c1.obj = c2;  // --\-- cycle
        c2.obj = c1;  //    \- cycle
        R2 r = new R2(5, 6, c1, c2);

        out.println("serializing : " + r);
        R2 deserializedObj = serializeDeserialize(r);
        out.println("deserialized: " + deserializedObj);
        assertEquals(deserializedObj.x(), 5);         // sanity
        assertEquals(deserializedObj.y(), 6);         // sanity

        c1 = deserializedObj.c1();
        c2 = deserializedObj.c2();
        assertTrue(c1.obj == c2);
        assertTrue(c2.obj == c1);
    }

    record R3 (long l, R r) implements Serializable { }

    /**
     * Deserialization of a record object, r, ( with a cycle ), should still
     * deserialize from within a record, r3.
     */
    @Test
    public void testCycle4() throws Exception  {
        out.println("\n---");
        C c = new C();
        R r = new R(7, 8, c);
        c.obj = r;  // cycle, targeting record r
        R3 r3 = new R3(9, r); // record within a record

        out.println("serializing : " + r3);
        R3 deserializedObj = serializeDeserialize(r3);
        out.println("deserialized: " + deserializedObj);
        assertTrue(deserializedObj.r() != null);
        assertEquals(deserializedObj.l(), 9);              // sanity
        assertEquals(deserializedObj.r().x(), 7);          // sanity
        assertEquals(deserializedObj.r().y(), 8);          // sanity
        assertTrue(deserializedObj.r().c() instanceof C);  // sanity
        assertEquals(deserializedObj.r().c().obj, null);   // cycle, expect null
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
