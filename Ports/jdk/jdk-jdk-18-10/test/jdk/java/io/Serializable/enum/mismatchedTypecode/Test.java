/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4838379
 * @summary Verify that TC_OBJECT followed by a class descriptor for an enum
 *          class results in an InvalidClassException, as does TC_ENUM followed
 *          by a class descriptor for a non-enum class.
 */

import java.io.*;

enum Foo { bar }

class TestObjectOutputStream extends ObjectOutputStream {

    static ObjectStreamClass fooDesc = ObjectStreamClass.lookup(Foo.class);
    static ObjectStreamClass integerDesc =
        ObjectStreamClass.lookup(Integer.class);

    TestObjectOutputStream(OutputStream out) throws IOException {
        super(out);
    }

    protected void writeClassDescriptor(ObjectStreamClass desc)
        throws IOException
    {
        if (desc == fooDesc) {
            super.writeClassDescriptor(integerDesc);
        } else if (desc == integerDesc) {
            super.writeClassDescriptor(fooDesc);
        } else {
            super.writeClassDescriptor(desc);
        }
    }
}

public class Test {
    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new TestObjectOutputStream(bout);
        oout.writeObject(Foo.bar);
        oout.close();
        ObjectInputStream oin = new ObjectInputStream(
            new ByteArrayInputStream(bout.toByteArray()));
        try {
            Object obj = oin.readObject();
            throw new Error("read of " + obj + " should not have succeeded");
        } catch (InvalidClassException e) {
            System.out.println("caught expected exception " + e);
        }

        oout = new TestObjectOutputStream(bout = new ByteArrayOutputStream());
        oout.writeObject(5);
        oout.close();
        oin = new ObjectInputStream(
            new ByteArrayInputStream(bout.toByteArray()));
        try {
            Object obj = oin.readObject();
            throw new Error("read of " + obj + " should not have succeeded");
        } catch (InvalidClassException e) {
            System.out.println("caught expected exception " + e);
        }
    }
}
