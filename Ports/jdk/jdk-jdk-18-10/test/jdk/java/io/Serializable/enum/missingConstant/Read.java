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
 * @summary Verify that deserialization of an enum constant that does not exist
 *          on the receiving side results in an InvalidObjectException.
 */

import java.io.*;

enum Foo { foo, bar }

public class Read {
    public static void main(String[] args) throws Exception {
        FileInputStream in = new FileInputStream("foo.ser");
        try {
            ObjectInputStream oin = new ObjectInputStream(in);
            for (Foo f : Foo.values()) {
                Object obj = oin.readObject();
                if (obj != f) {
                    throw new Error("expected " + f + ", got " + obj);
                }
            }
            try {
                Object obj = oin.readObject();
                throw new Error("read of " + obj + " should not succeed");
            } catch (InvalidObjectException e) {
                System.out.println("caught expected exception: " + e);
            }
        } finally {
            in.close();
        }
    }
}
