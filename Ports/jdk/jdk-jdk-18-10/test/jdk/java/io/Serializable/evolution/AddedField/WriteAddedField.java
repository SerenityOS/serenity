/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary  Deserialize an evolved class with a new field, field type is new.
 *
 * @clean A B C NewExternField NewFieldClass ReadAddedField WriteAddedField
 * @compile WriteAddedField.java
 * @run main WriteAddedField
 * @clean A B C NewExternField NewFieldClass ReadAddedField WriteAddedField
 * @compile ReadAddedField.java
 * @run main ReadAddedField


 */
import java.io.*;

class NewFieldClass implements Serializable {
    private static final long serialVersionUID = 1L;

    int k;

    NewFieldClass(int value) {
        k = value;
    }
};

class IncompatibleFieldClass implements Serializable {
    private static final long serialVersionUID = 3L;
    int x = 5;
};

@SuppressWarnings("serial") /* Incorrect use is being tested */
class NewExternFieldClass implements Externalizable {
    private static final long serialVersionUID = 1L;

    byte l;

    public NewExternFieldClass(int value) {
        l = (byte)value;
    }

    public void readExternal(ObjectInput s)
        throws IOException, ClassNotFoundException
    {
        l = s.readByte();
    }

    public void writeExternal(ObjectOutput s) throws IOException
    {
        s.writeByte(l);
    }
}

class A implements Serializable  {
    // Version 1.1 of class A.  Added superclass NewSerializableSuper.
    private static final long serialVersionUID = 1L;
    NewFieldClass foo;
    NewFieldClass fooarray[];
    IncompatibleFieldClass boo;
    IncompatibleFieldClass booarray[];
    /* Excluded due to Bug 4089540
    NewExternFieldClass abc;
    NewExternFieldClass farray[];
    */
    int bar;
    A() {
        foo = new NewFieldClass(23);
        fooarray = new NewFieldClass[24];
        for (int i = 0; i < fooarray.length; i++)
            fooarray[i] = new NewFieldClass(i);
        boo = new IncompatibleFieldClass();
        booarray = new IncompatibleFieldClass[24];
        for (int i = 0; i < booarray.length; i++)
            booarray[i] = new IncompatibleFieldClass();
        bar = 4;
        /* Excluded due to Bug 4089540
        abc = new NewExternFieldClass(66);
        farray = new NewExternFieldClass[10];
        for (int k = 0; k < farray.length; k++)
            farray[k] = new NewExternFieldClass(k);
        */
    }
}

/** Test serial persistent fields w/o using Alternate API.
  */
class B implements Serializable {
    private static final long serialVersionUID = 2L;
    NewFieldClass       foo;
    NewFieldClass[]     array;
    IncompatibleFieldClass boo;
    IncompatibleFieldClass booarray[];
    transient NewExternFieldClass abc;
    transient NewExternFieldClass farray[];
    int                 bar;

    B() {
        foo = new NewFieldClass(12);
        array = new NewFieldClass[12];
        for (int i = 0; i < array.length; i++)
            array[i] = new NewFieldClass(i);
        bar = 4;
        abc = new NewExternFieldClass(66);
        farray = new NewExternFieldClass[10];
        for (int k = 0; k < farray.length; k++)
            farray[k] = new NewExternFieldClass(k);
    }
}

/** Test serial persistent fields using Alternate API.
  * Also make sure that optional data to non-existent classes can be skipped.
  */
class C implements Serializable {
    private static final long serialVersionUID = 3L;
    NewFieldClass       foo;
    NewFieldClass[]     array;
    int                 bar;
    transient NewExternFieldClass abc;
    transient NewExternFieldClass farray[];

    C() {
        foo = new NewFieldClass(12);
        array = new NewFieldClass[12];
        for (int i = 0; i < array.length; i++)
            array[i] = new NewFieldClass(i);
        bar = 4;
        abc = new NewExternFieldClass(3);
        farray = new NewExternFieldClass[10];
        for (int k = 0; k < farray.length; k++)
            farray[k] = new NewExternFieldClass(k);
    }

    private void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException
    {
        s.defaultReadObject();
        /* Excluded due to Bug 4089540
        abc  = (NewExternFieldClass)fields.get("abc", null);
        farray = (NewExternFieldClass[])fields.get("farray", null);
        */

        /* read optional data */
        NewFieldClass   tmpfoo  = (NewFieldClass)s.readObject();
        NewFieldClass[] tmparray= (NewFieldClass[])s.readObject();
        int tmpbar = s.readInt();
        /* Excluded due to Bug 4089540
        NewExternFieldClass tmpabc = (NewExternFieldClass)s.readObject();
        NewExternFieldClass[] tmpfarray = (NewExternFieldClass[])s.readObject();
        */
    }

    private void writeObject(ObjectOutputStream s)
        throws IOException
    {
        s.defaultWriteObject();

        /* write optional data */
        s.writeObject(foo);
        s.writeObject(array);
        s.writeInt(bar);

        /* Excluded due to Bug 4089540
        s.writeObject(abc);
        s.writeObject(farray);
        */
    }
}


public class WriteAddedField {
    public static void main(String args[]) throws IOException {
        A a = new A();
        B b = new B();
        C c = new C();
        File f = new File("tmp.ser");
        ObjectOutput out =
            new ObjectOutputStream(new FileOutputStream(f));
        out.writeObject(a);
        out.writeObject(b);
        out.writeObject(c);
        a = new A();
        b = new B();
        c = new C();
        out.writeObject(a);
        out.writeObject(b);
        out.writeObject(c);
        out.close();
    }
}
