/*
 * Copyright (c) 2001, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4404696
 * @summary Verify that serialization does not require matching type strings
 *          for non-primitive fields.
 *
 * NOTE: This test should be removed if it is determined that serialization
 * *should* consider type strings when matching non-primitive fields.
 */

import java.io.*;

class Foo implements Serializable {
    private static final long serialVersionUID = 0L;
    String obj;         // writer defines this field as type Object
}

class Bar implements Serializable {
    private static final long serialVersionUID = 0L;
    short q;            // writer defines this field as type int
}

public class Read {
    public static void main(String[] args) throws Exception {
        FileInputStream in = new FileInputStream("foo.ser");
        try {
            ObjectInputStream oin = new ObjectInputStream(in);
            Foo foo = (Foo) oin.readObject();
            if (! foo.obj.equals("foo")) {
                throw new Error();
            }
            try {
                oin.readObject();
                throw new Error();
            } catch (ClassCastException ex) {
            }
        } finally {
            in.close();
        }

        in = new FileInputStream("bar.ser");
        try {
            ObjectInputStream oin = new ObjectInputStream(in);
            oin.readObject();
            throw new Error();
        } catch (InvalidClassException ex) {
        } finally {
            in.close();
        }
    }
}
