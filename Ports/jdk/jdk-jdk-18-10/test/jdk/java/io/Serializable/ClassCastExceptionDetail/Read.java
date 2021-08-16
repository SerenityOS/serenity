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
 * @bug 4511532
 * @summary Verify that the message string of a ClassCastException thrown by
 *          ObjectInputStream when attempting to assign a value to a field of
 *          an incompatible type contains the names of the value's class, the
 *          field's declaring class, the field's type, and the field itself.
 */

import java.io.*;

class Foo implements Serializable {
    private static final long serialVersionUID = 0L;
    private Float bar;
}

class Gub extends Foo {
    private static final long serialVersionUID = 1L;
}

public class Read {
    public static void main(String[] args) throws Exception {
        FileInputStream in = new FileInputStream("tmp.ser");
        try {
            ObjectInputStream oin = new ObjectInputStream(in);
            oin.readObject();
            throw new Error("readObject should not succeed");
        } catch (ClassCastException e) {
            String s = e.getMessage();
            System.out.println("ClassCastException message: " + s);
            if (s == null ||
                s.indexOf(Foo.class.getName()) == -1 ||
                s.indexOf(Integer.class.getName()) == -1 ||
                s.indexOf(Float.class.getName()) == -1 ||
                s.indexOf(Gub.class.getName()) == -1 ||
                s.indexOf("bar") == -1)
            {
                throw new Error("ClassNotFoundException message incomplete");
            }
        } finally {
            in.close();
        }
    }
}
