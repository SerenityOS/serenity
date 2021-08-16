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
 * @bug 4387368
 * @summary Verify that object whose class declared a serial persistent field
 *          that does not match any actual field cannot be serialized via
 *          default serialization.
 */

import java.io.*;

class A implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("nonexistent", int.class)
    };
}

class B implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("mismatched", int.class)
    };
    private float mismatched;
}

class C implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("existent", int.class)
    };
    private int existent;
}

public class BadSerialPersistentField {
    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout;

        oout = new ObjectOutputStream(bout);
        try {
            oout.writeObject(new A());
            throw new Error();
        } catch (InvalidClassException ex) {
        }

        oout = new ObjectOutputStream(bout);
        try {
            oout.writeObject(new B());
            throw new Error();
        } catch (InvalidClassException ex) {
        }

        oout = new ObjectOutputStream(bout);
        oout.writeObject(new C());
    }
}
