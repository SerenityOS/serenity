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
 * @bug 4663191
 * @summary Verify that readObject and readObjectNoData methods will not be
 *          called on an object being deserialized if that object is already
 *          tagged with a ClassNotFoundException.
 */

import java.io.*;

class A implements Serializable {
    private static final long serialVersionUID = 0L;
    @SuppressWarnings("serial") /* Incorrect declarations are being tested */
    Object x;
}

class B extends A {
    private static final long serialVersionUID = 0L;
    private void readObjectNoData() throws ObjectStreamException {
        throw new Error("readObjectNoData called");
    }
}

class C extends B {
    private static final long serialVersionUID = 0L;
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        throw new Error("readObject called");
    }
}

public class Read {
    public static void main(String[] args) throws Exception {
        FileInputStream in = new FileInputStream("tmp.ser");
        try {
            ObjectInputStream oin = new ObjectInputStream(in);
            try {
                oin.readObject();
                throw new Error("readObject should not succeed");
            } catch (ClassNotFoundException e) {
                // expected
            }
            if (!oin.readObject().equals("after")) {
                throw new Error("subsequent object corrupted");
            }
        } finally {
            in.close();
        }
    }
}
