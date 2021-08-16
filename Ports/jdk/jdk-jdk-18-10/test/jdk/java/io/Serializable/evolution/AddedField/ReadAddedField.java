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

/*
 * @bug 4088176
 * @build WriteAddedField ReadAddedField
 * @run main ReadAddedField
 *
 * @summary Deserialize an evolved class with a new field, field type is new.
 *
 */

import java.io.*;

class IncompatibleFieldClass implements Serializable {
    private static final long serialVersionUID = 3L;
    int x = 5;
};

class A implements Serializable {
    // Version 1.0 of class A.
    private static final long serialVersionUID = 1L;
    public int bar;
}

/** Test serial persistent fields w/o using Alternate API.
  */
class B implements Serializable {
    private static final long serialVersionUID = 2L;
    int bar;

    B() {
        bar = 4;
    }
}

/** Test serial persistent fields using Alternate API.
  * Also make sure that optional data to non-existent classes can be skipped.
  */
class C implements Serializable {
    private static final long serialVersionUID = 3L;
    int bar;

    C() {
        bar = 4;
    }

    private void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException
    {
        s.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream s)
        throws IOException
    {
        s.defaultWriteObject();
    }
}

public class ReadAddedField {
    public static void main(String args[])
        throws IOException, ClassNotFoundException
    {
        File f = new File("tmp.ser");
        ObjectInput in =
            new ObjectInputStream(new FileInputStream(f));
        A a = (A)in.readObject();
        if (a.bar != 4)
            throw new RuntimeException("a.bar does not equal 4, it equals " +
                                       a.bar);
        B b = (B)in.readObject();
        if (b.bar != 4)
            throw new RuntimeException("b.bar does not equal 4, it equals " +
                                       b.bar);
        C c = (C)in.readObject();
        if (c.bar != 4)
            throw new RuntimeException("c.bar does not equal 4, it equals " +
                                       c.bar);
        A aa = (A)in.readObject();
        if (aa.bar != 4)
            throw new RuntimeException("a.bar does not equal 4, it equals " +
                                       aa.bar);
        B bb = (B)in.readObject();
        if (bb.bar != 4)
            throw new RuntimeException("b.bar does not equal 4, it equals " +
                                       bb.bar);
        C cc = (C)in.readObject();
        if (cc.bar != 4)
            throw new RuntimeException("c.bar does not equal 4, it equals " +
                                       cc.bar);
        in.close();
    }
}
