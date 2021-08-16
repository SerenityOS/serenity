/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4180735
 * @summary Make sure that fields that are defaulted can be of primitive and
 *          object type.
 *
 */

import java.io.*;
class TestClass implements Serializable {
    public static final Integer DEFAULT_OBJECT_I = 99;
    public static final Foo DEFAULT_OBJECT_F = new Foo();
    private static final long serialVersionUID = 5748652654655279289L;

    // Fields to be serialized.
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("objectI", Integer.class),
        new ObjectStreamField("primitiveI", Integer.TYPE),
        new ObjectStreamField("foo", Foo.class)
    };

    Integer objectI;
    int     primitiveI;
    Foo foo;

    public TestClass(Foo f, Integer I, int i) {
        foo = f;
        objectI = I;
        primitiveI = i;
    }

    /**
     * Verify GetField.defaulted("fieldName") works for primitive and
     * object fields.
     */
    private void readObject(ObjectInputStream in)
        throws ClassNotFoundException, IOException
    {
        ObjectInputStream.GetField pfields = in.readFields();
        primitiveI = pfields.get("primitiveI", 99);
        System.out.println("The primitiveI : " + primitiveI);

        objectI = (Integer)pfields.get("objectI", DEFAULT_OBJECT_I);
        System.out.println("The ObjectI : " + objectI);

        foo = (Foo)pfields.get("foo", DEFAULT_OBJECT_F);
        System.out.println("The foo : " + foo);

        try {
            boolean b = pfields.defaulted("primitiveI");
            System.out.println("Defaulted prim : " + b);
            if (b == false) {
                throw new Error("Bad return value for defaulted() with " +
                    "primitive type fields");
            }

            b = pfields.defaulted("objectI");
            System.out.println("Defaulted ObjectI : " + b);
            if (b == true) {
                throw new Error("Bad return value for defaulted() with " +
                    "object type fields");
            }

            b = pfields.defaulted("foo");
            System.out.println("Defaulted Foo : " + b);
            if (b == false) {
                throw new Error("Bad return value for defaulted() with " +
                    "object type fields");
            }

        } catch (IllegalArgumentException e) {
            System.out.println("Exception " + e.getMessage() +
                   ": handled calling " +
                   "GetField.defaulted(\"fieldName referring to an object\")");
            throw e;
        }
    }
};

public class GetFieldRead {
    public static void main(String[] args)
        throws ClassNotFoundException, IOException
    {
        FileInputStream fis = new FileInputStream("data.ser");
        ObjectInputStream in = new ObjectInputStream(fis);
        TestClass obj = (TestClass) in.readObject();
        in.close();
    }
};
