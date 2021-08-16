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

/*
 * @test
 * @bug 8238763 8246774
 * @summary ObjectInputStream readUnshared method handling of Records
 * @run testng UnsharedTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/**
 * Tests OOS::writeUnshared and OIS::readUnshared to verify that records
 * are correctly unshared in the stream as appropriate.
 */
public class UnsharedTest {

    record Foo(int x) implements Serializable { }

    static final Class<InvalidObjectException> IOE = InvalidObjectException.class;

    @Test
    public void testReadUnshared() throws Exception {
        var foo = new Foo(6);
        {  // shared - sanity to ensure the second foo is a ref
            var byteStream = serialize(foo, foo);
            var foo1 = (Foo) deserializeOne(byteStream);
            assertEquals(foo1.x, foo.x);
            var foo2 = (Foo) deserializeOne(byteStream);
            assertEquals(foo2.x, foo.x);
            assertTrue(foo2 == foo1);
        }
        {  // unshared
            var byteStream = serialize(foo, foo);
            var foo1 = (Foo) deserializeOneUnshared(byteStream);
            assertEquals(foo1.x, foo.x);
            var expected = expectThrows(IOE, () -> deserializeOne(byteStream));
            assertTrue(expected.getMessage().contains("cannot read back reference to unshared object"));
        }
    }

    @Test
    public void testWriteUnshared() throws Exception {
        var foo = new Foo(7);
        {  // shared - sanity to ensure the second foo is NOT a ref
            var byteStream = serializeUnshared(foo, foo);
            var foo1 = (Foo) deserializeOne(byteStream);
            assertEquals(foo1.x, foo.x);
            var foo2 = (Foo) deserializeOne(byteStream);
            assertEquals(foo2.x, foo.x);
            assertTrue(foo2 != foo1);
        }
        {  // unshared
            var byteStream = serializeUnshared(foo, foo);
            var foo1 = (Foo) deserializeOneUnshared(byteStream);
            assertEquals(foo1.x, foo.x);
            var foo2 = (Foo) deserializeOneUnshared(byteStream);
            assertEquals(foo2.x, foo.x);
            assertTrue(foo2 != foo1);
        }
    }

    // ---

    static ObjectInputStream serialize(Object... objs) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        for (Object obj : objs)
            oos.writeObject(obj);
        oos.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        return new ObjectInputStream(bais);
    }

    static ObjectInputStream serializeUnshared(Object... objs) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        for (Object obj : objs)
            oos.writeUnshared(obj);
        oos.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        return new ObjectInputStream(bais);
    }

    @SuppressWarnings("unchecked")
    static Object deserializeOne(ObjectInputStream ois)
        throws IOException, ClassNotFoundException
    {
        return ois.readObject();
    }

    @SuppressWarnings("unchecked")
    static Object deserializeOneUnshared(ObjectInputStream ois)
        throws IOException, ClassNotFoundException
    {
        return ois.readUnshared();
    }
}
