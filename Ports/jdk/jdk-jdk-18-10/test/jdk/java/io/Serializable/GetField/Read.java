/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4402830
 * @summary Verify that the ObjectInputStream.GetField API works properly for
 *          serialized fields which don't exist in the receiving object.
 */

import java.io.*;

class Foo implements Serializable {
    private static final long serialVersionUID = 0L;
    int blargh;

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        ObjectInputStream.GetField fields = in.readFields();
        if (! fields.defaulted("blargh")) {
            throw new Error();
        }
        try {
            fields.defaulted("nonexistant");
            throw new Error();
        } catch (IllegalArgumentException ex) {
        }
        if ((fields.get("z", false) != true) ||
            (fields.get("b", (byte) 0) != 5) ||
            (fields.get("c", '0') != '5') ||
            (fields.get("s", (short) 0) != 5) ||
            (fields.get("i", 0) != 5) ||
            (fields.get("j", 0l) != 5) ||
            (fields.get("f", 0.0f) != 5.0f) ||
            (fields.get("d", 0.0) != 5.0) ||
            (! fields.get("str", null).equals("5")))
        {
            throw new Error();
        }
    }
}

public class Read {
    public static void main(String[] args) throws Exception {
        ObjectInputStream oin =
            new ObjectInputStream(new FileInputStream("tmp.ser"));
        oin.readObject();
        oin.close();
    }
}
