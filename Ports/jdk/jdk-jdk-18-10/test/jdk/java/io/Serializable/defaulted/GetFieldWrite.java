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

/* @test
 * @bug 4180735
 *
 * @clean GetFieldWrite Foo TestClass
 * @build GetFieldWrite
 * @run main GetFieldWrite
 * @clean GetFieldRead TestClass
 * @build GetFieldRead
 * @run main GetFieldRead
 *
 * @summary Make sure that fields that are defaulted can be of primitive and
 *          object type.
 *
 */

import java.io.*;
class TestClass implements Serializable {

    private static final long serialVersionUID = 5748652654655279289L;

    // Fields to be serialized.
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("objectI", Integer.class)};

    Integer objectI;
    int     primitiveI;
    Foo foo;

    public TestClass(Foo f, Integer I, int i) {
        foo = f;
        objectI = I;
        primitiveI = i;
    }
};

public class GetFieldWrite {
    public static void main(String[] args)
        throws ClassNotFoundException, IOException
    {
        FileOutputStream fos = new FileOutputStream("data.ser");
        ObjectOutput out = new ObjectOutputStream(fos);
        out.writeObject(new TestClass(new Foo(100, 200), 100, 200));
        out.close();
    }
};

/*
 * Test class to be used as data field
 */
class Foo implements Serializable{
    private static final long serialVersionUID = 1L;

    int a;
    int b;
    public Foo() {
        a = 10; b= 20;
    }

    public Foo(int a1, int b1)
    {
        a = a1; b = b1;
    }

    public String toString() {
        return new String("a = " + a + " b = " + b);
    }
}
