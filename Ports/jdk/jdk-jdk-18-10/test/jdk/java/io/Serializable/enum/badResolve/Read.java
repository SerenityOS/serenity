/*
 * Copyright (c) 2003, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4838379
 * @summary Verify that enum classes present in a serialization stream cannot
 *          be resolved by the receiver to non-enum classes, and vice-versa.
 */

import java.io.*;

class EnumToNonEnum implements Serializable {
    private static final long serialVersionUID = 0L;
}

enum NonEnumToEnum { foo }

public class Read {
    public static void main(String[] args) throws Exception {
        read("0.ser");
        read("1.ser");
        read("2.ser");
        read("3.ser");
    }

    static void read(String filename) throws Exception {
        FileInputStream in = new FileInputStream(filename);
        try {
            ObjectInputStream oin = new ObjectInputStream(in);
            Object obj = oin.readObject();
            throw new Error("read of " + obj + " should not have succeeded");
        } catch (InvalidClassException e) {
            System.out.println("caught expected exception " + e);
        } finally {
            in.close();
        }
    }
}
