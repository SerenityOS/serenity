/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4662327
 * @summary Verify that ObjectInputStream.defaultReadObject() throws a
 *          ClassNotFoundException if any of the non-primitive field values it
 *          reads in are tagged with ClassNotFoundExceptions.
 */

import java.io.*;

class Foo implements Serializable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("serial") /* Incorrect use is being tested */
    Object obj = new Bar();

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        if (obj == null) {
            throw new Error(
                "ClassNotFoundException masked by defaultReadObject()");
        }
    }
}

class Bar implements Serializable {
    private static final long serialVersionUID = 1L;
}

class TestObjectInputStream extends ObjectInputStream {
    TestObjectInputStream(InputStream in) throws IOException { super(in); }

    protected Class<?> resolveClass(ObjectStreamClass desc)
        throws IOException, ClassNotFoundException
    {
        if (desc.getName().equals(Bar.class.getName())) {
            throw new ClassNotFoundException();
        }
        return super.resolveClass(desc);
    }
}

public class DefaultReadObjectCNFException {
    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(new Foo());
        oout.writeObject("after");
        oout.close();
        ObjectInputStream oin = new TestObjectInputStream(
            new ByteArrayInputStream(bout.toByteArray()));
        try {
            oin.readObject();
        } catch (ClassNotFoundException e) {
            // expected
        }
        if (!oin.readObject().equals("after")) {
            throw new Error("subsequent object corrupted");
        }
    }
}
