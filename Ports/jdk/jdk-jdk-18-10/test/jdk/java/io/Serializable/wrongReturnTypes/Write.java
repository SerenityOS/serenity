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

/* @test
 * @bug 4337857
 *
 * @clean Write Read A B
 * @compile Write.java
 * @run main Write
 * @clean Write Read A B
 * @compile Read.java
 * @run main Read
 *
 * @summary Verify that custom serialization methods declared with incorrect
 *          return types are not invoked.
 */

import java.io.*;

class B implements Serializable {
    private static final long serialVersionUID = 0L;

    static boolean writeObjectCalled;
    static boolean writeReplaceCalled;

    @SuppressWarnings("serial") /* Incorrect use is being tested */
    private Object writeObject(ObjectOutputStream out) throws IOException {
        writeObjectCalled = true;
        out.defaultWriteObject();
        return null;
    }

    @SuppressWarnings("serial") /* Incorrect use is being tested */
    private B writeReplace() throws ObjectStreamException {
        writeReplaceCalled = true;
        return this;
    }
}

public class Write {
    public static void main(String[] args) throws Exception {
        ObjectOutputStream oout =
            new ObjectOutputStream(new FileOutputStream("tmp.ser"));
        oout.writeObject(new B());
        oout.close();

        if (B.writeObjectCalled) {
            throw new Error("writeObject with wrong return type called");
        } else if (B.writeReplaceCalled) {
            throw new Error("writeReplace with wrong return type called");
        }
    }
}
