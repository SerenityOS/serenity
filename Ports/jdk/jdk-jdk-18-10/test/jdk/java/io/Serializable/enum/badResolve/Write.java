/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4838379
 * @summary Verify that enum classes present in a serialization stream cannot
 *          be resolved by the receiver to non-enum classes, and vice-versa.
 *
 * @compile Write.java
 * @run main Write
 * @clean Write
 * @compile Read.java
 * @run main Read
 * @clean Read
 */

import java.io.*;

enum EnumToNonEnum { foo }

class NonEnumToEnum implements Serializable {
    private static final long serialVersionUID = 0L;
}

public class Write {
    public static void main(String[] args) throws Exception {
        write(EnumToNonEnum.class, "0.ser");
        write(NonEnumToEnum.class, "1.ser");
        write(EnumToNonEnum.foo, "2.ser");
        write(new NonEnumToEnum(), "3.ser");
    }

    static void write(Object obj, String filename) throws Exception {
        ObjectOutputStream oout =
            new ObjectOutputStream(new FileOutputStream(filename));
        oout.writeObject(obj);
        oout.close();
    }
}
