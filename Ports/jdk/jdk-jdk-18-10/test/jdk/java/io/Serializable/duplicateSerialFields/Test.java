/*
 * Copyright (c) 2002, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4764280
 *
 * @clean Setup Test A B
 * @compile Setup.java
 * @run main Setup
 * @clean Setup A B
 * @compile Test.java
 * @run main Test
 *
 * @summary Verify that if a serializable class declares multiple
 *          serialPersistentFields that share the same name, calling
 *          ObjectStreamClass.lookup() for that class will not result in an
 *          InternalError, and that attempts at default serialization or
 *          deserialization of such a class will result in
 *          InvalidClassExceptions.
 */

import java.io.*;

class A implements Serializable {
    private static final long serialVersionUID = 0L;
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("i", int.class),
        new ObjectStreamField("i", int.class)
    };
    int i;
}

class B implements Serializable {
    private static final long serialVersionUID = 0L;
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("i", int.class),
        new ObjectStreamField("i", String.class)
    };
    int i;
}

public class Test {
    public static void main(String[] args) throws Exception {

        ObjectStreamClass.lookup(A.class);
        ObjectStreamClass.lookup(B.class);

        ObjectOutputStream oout =
            new ObjectOutputStream(new ByteArrayOutputStream());
        try {
            oout.writeObject(new A());
            throw new Error(
                "write of A should fail with InvalidClassException");
        } catch (InvalidClassException e) {
        }

        oout = new ObjectOutputStream(new ByteArrayOutputStream());
        try {
            oout.writeObject(new B());
            throw new Error(
                "write of B should fail with InvalidClassException");
        } catch (InvalidClassException e) {
        }

        FileInputStream in = new FileInputStream("a.ser");
        try {
            ObjectInputStream oin = new ObjectInputStream(in);
            oin.readObject();
            throw new Error(
                "read of A should fail with InvalidClassException");
        } catch (InvalidClassException e) {
        } finally {
            in.close();
        }

        in = new FileInputStream("b.ser");
        try {
            ObjectInputStream oin = new ObjectInputStream(in);
            oin.readObject();
            throw new Error(
                "read of B should fail with InvalidClassException");
        } catch (InvalidClassException e) {
        } finally {
            in.close();
        }
    }
}
