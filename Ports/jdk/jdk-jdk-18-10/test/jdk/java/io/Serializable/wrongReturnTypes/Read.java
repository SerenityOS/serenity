/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4337857
 * @summary Verify that custom serialization methods declared with incorrect
 *          return types are not invoked.
 */

import java.io.*;

class A implements Serializable {
    private static final long serialVersionUID = 0L;

    static boolean readObjectNoDataCalled;

    @SuppressWarnings("serial") /* Incorrect use is being tested */
    private Object readObjectNoData() throws ObjectStreamException {
        readObjectNoDataCalled = true;
        return null;
    }
}

class B extends A {
    private static final long serialVersionUID = 0L;

    static boolean readObjectCalled;
    static boolean readResolveCalled;

    @SuppressWarnings("serial") /* Incorrect use is being tested */
    private Integer readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        readObjectCalled = true;
        in.defaultReadObject();
        return null;
    }

    @SuppressWarnings("serial") /* Incorrect use is being tested */
    private B readResolve() throws ObjectStreamException {
        readResolveCalled = true;
        return this;
    }
}

public class Read {
    public static void main(String[] args) throws Exception {
        FileInputStream in = new FileInputStream("tmp.ser");
        try {
            ObjectInputStream oin = new ObjectInputStream(in);
            B b = (B) oin.readObject();
            if (A.readObjectNoDataCalled) {
                throw new Error("readObjectNoData with wrong return type called");
            } else if (B.readObjectCalled) {
                throw new Error("readObject with wrong return type called");
            } else if (B.readResolveCalled) {
                throw new Error("readResolve with wrong return type called");
            }
        } finally {
            in.close();
        }
    }
}
