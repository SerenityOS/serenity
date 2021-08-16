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
 * @bug 4407956
 * @summary Verify that ClassNotFoundExceptions explicitly constructed and
 *          thrown from with custom readObject/readExternal methods are
 *          propagated properly.
 */

import java.io.*;

class A implements Serializable {
    private static final long serialVersionUID = 1L;

    private void readObject(ObjectInputStream in)
        throws ClassNotFoundException, IOException
    {
        throw new ClassNotFoundException();
    }
}

class B implements Externalizable {
    private static final long serialVersionUID = 1L;

    public B() {}

    public void writeExternal(ObjectOutput out) throws IOException {}

    public void readExternal(ObjectInput in)
        throws ClassNotFoundException, IOException
    {
        throw new ClassNotFoundException();
    }
}

public class ExplicitCNFException {
    public static void main(String[] args) throws Exception {
        test(new A());
        test(new Object[]{ new A() });
        test(new B());
        test(new Object[]{ new B() });
    }

    static void test(Object obj) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.useProtocolVersion(ObjectStreamConstants.PROTOCOL_VERSION_2);
        oout.writeObject(obj);
        oout.close();
        ObjectInputStream oin = new ObjectInputStream(
            new ByteArrayInputStream(bout.toByteArray()));
        try {
            oin.readObject();
            throw new Error();  // should not succeed
        } catch (ClassNotFoundException ex) {
        }
    }
}
