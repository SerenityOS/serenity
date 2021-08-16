/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4088176
 *
 * @clean A D NewExternFieldClass ReadAddedField WriteAddedField
 * @compile WriteAddedField.java
 * @run main WriteAddedField
 * @clean A D NewExternFieldClass ReadAddedField WriteAddedField
 * @compile ReadAddedField.java
 * @run main ReadAddedField
 *
 * @summary Evolution: read evolved class with new field of a non-existing Externalizable class.
 */
import java.io.*;

class NewExternFieldClass implements Externalizable {
    private static final long serialVersionUID = 1L;

    byte l;

    public NewExternFieldClass() {
        l = 0;
    }

    public NewExternFieldClass(byte value) {
        l = value;
    }

    public void readExternal(ObjectInput s)
        throws IOException, ClassNotFoundException
    {
        l = s.readByte();
        System.out.println("readExternal read " + l);
    }

    public void writeExternal(ObjectOutput s) throws IOException
    {
        s.writeByte(l);
    }
}

class D implements Serializable {
    private static final long serialVersionUID = 1L;

    public int x;
    D(int y) {
        x = y;
    }
}

class A implements Serializable  {
    // Version 1.1 of class A.  Added superclass NewSerializableSuper.
    private static final long serialVersionUID = 1L;
    NewExternFieldClass foo;
    D zoo;

    int bar;
    A() {
        bar = 4;
        foo = new NewExternFieldClass((byte)66);
        zoo = new D(22);
    }
}

public class WriteAddedField {
    public static void main(String args[]) throws IOException {
        A a = new A();
        File f = new File("tmp.ser");
        ObjectOutput out =
            new ObjectOutputStream(new FileOutputStream(f));
        out.writeObject(a);
        out.writeObject(new A());
        out.close();
    }
}
