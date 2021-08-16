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
 * @bug 4502808
 * @summary Verify that if the custom serialization method (i.e., readExternal,
 *          writeExternal, readObject or writeObject) of an object closes the
 *          stream it is passed, (de)serialization of that object can still
 *          complete.
 */

import java.io.*;

class A implements Externalizable {
    private static final long serialVersionUID = 1L;

    public A() {}

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(0);
        out.close();
    }

    public void readExternal(ObjectInput in)
        throws IOException, ClassNotFoundException
    {
        in.readInt();
        in.close();
    }
}

class B implements Serializable {
    private static final long serialVersionUID = 1L;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.close();
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        in.close();
    }
}

public class SurvivePrematureClose {

    public static void main(String[] args) throws Exception {
        writeRead(new A());
        writeRead(new B());
    }

    static void writeRead(Object obj) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(obj);
        oout.close();
        ObjectInputStream oin = new ObjectInputStream(
            new ByteArrayInputStream(bout.toByteArray()));
        oin.readObject();
    }
}
