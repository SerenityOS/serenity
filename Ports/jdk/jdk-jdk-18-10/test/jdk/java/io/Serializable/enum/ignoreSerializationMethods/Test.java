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
 * @summary Verify that custom serialization methods defined by enum types are
 *          not invoked during serialization or deserialization.
 */

import java.io.*;

enum Foo {

    foo,
    bar {
        @SuppressWarnings("serial") /* Incorrect declarations are being tested */
        private void writeObject(ObjectOutputStream out) throws IOException {
            throw new Error("bar.writeObject invoked");
        }
        @SuppressWarnings("serial") /* Incorrect declarations are being tested */
        private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
            throw new Error("bar.readObject invoked");
        }
        @SuppressWarnings("serial") /* Incorrect declarations are being tested */
        Object writeReplace() throws ObjectStreamException {
            throw new Error("bar.writeReplace invoked");
        }
        // readResolve cannot be defined until Enum.readResolve is removed
        // Object readResolve() throws ObjectStreamException {
        //    throw new Error("bar.readResolve invoked");
        // }
    };

    @SuppressWarnings("serial") /* Incorrect use is being tested */
    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new Error("Foo.writeObject invoked");
    }
    @SuppressWarnings("serial") /* Incorrect use is being tested */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        throw new Error("Foo.readObject invoked");
    }
    @SuppressWarnings("serial") /* Incorrect use is being tested */
    Object writeReplace() throws ObjectStreamException {
        throw new Error("Foo.writeReplace invoked");
    }
    // readResolve cannot be defined until Enum.readResolve is removed
    // Object readResolve() throws ObjectStreamException {
    //    throw new Error("Foo.readResolve invoked");
    // }
}

public class Test {
    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        for (Foo f : Foo.values()) {
            oout.writeObject(f);
        }
        oout.close();
        ObjectInputStream oin = new ObjectInputStream(
            new ByteArrayInputStream(bout.toByteArray()));
        for (Foo f : Foo.values()) {
            Object obj = oin.readObject();
            if (obj != f) {
                throw new Error("expected " + f + ", got " + obj);
            }
        }
    }
}
