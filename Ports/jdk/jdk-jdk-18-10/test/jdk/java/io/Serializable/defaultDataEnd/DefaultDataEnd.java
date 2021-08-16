/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4360508
 * @summary Verify that a custom readObject() method reading in data written
 *          via default serialization cannot read past the end of the default
 *          data.
 */

import java.io.*;

class A implements Serializable {
    private static final long serialVersionUID = 1L;

    int i1 = 1, i2 = 2;
    String s1 = "foo", s2 = "bar";

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        if (in.read() != -1) {
            throw new Error();
        }
        try {
            in.readInt();
            throw new Error();
        } catch (EOFException ex) {
        }
        try {
            in.readObject();
            throw new Error();
        } catch (OptionalDataException ex) {
            if (!ex.eof) {
                throw new Error();
            }
        }
        try {
            in.readUnshared();
            throw new Error();
        } catch (OptionalDataException ex) {
            if (!ex.eof) {
                throw new Error();
            }
        }
    }
}

class B implements Serializable {
    private static final long serialVersionUID = 1L;

    int i1 = 1, i2 = 2;
    String s1 = "foo", s2 = "bar";

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.readFields();
        try {
            in.readObject();
            throw new Error();
        } catch (OptionalDataException ex) {
            if (!ex.eof) {
                throw new Error();
            }
        }
        try {
            in.readUnshared();
            throw new Error();
        } catch (OptionalDataException ex) {
            if (!ex.eof) {
                throw new Error();
            }
        }
        if (in.read() != -1) {
            throw new Error();
        }
        try {
            in.readInt();
            throw new Error();
        } catch (EOFException ex) {
        }
    }
}

class C implements Serializable {
    private static final long serialVersionUID = 1L;

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        try {
            in.readObject();
            throw new Error();
        } catch (OptionalDataException ex) {
            if (!ex.eof) {
                throw new Error();
            }
        }
        try {
            in.readUnshared();
            throw new Error();
        } catch (OptionalDataException ex) {
            if (!ex.eof) {
                throw new Error();
            }
        }
        if (in.read() != -1) {
            throw new Error();
        }
        try {
            in.readInt();
            throw new Error();
        } catch (EOFException ex) {
        }
    }
}

public class DefaultDataEnd {
    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(new A());
        oout.writeObject(new B());
        oout.writeObject(new C());
        oout.close();
        ObjectInputStream oin = new ObjectInputStream(
            new ByteArrayInputStream(bout.toByteArray()));
        oin.readObject();
        oin.readObject();
        oin.readObject();
    }
}
