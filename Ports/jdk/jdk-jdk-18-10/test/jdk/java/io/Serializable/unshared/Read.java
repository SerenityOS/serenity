/*
 * Copyright (c) 2000, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4311991
 * @summary Test ObjectOutputStream.writeUnshared/readUnshared functionality.
 */

import java.io.*;

class Bar implements Serializable {
    private static final long serialVersionUID = 0L;
    private static final ObjectStreamField[] serialPersistentFields =
        new ObjectStreamField[] {
            new ObjectStreamField("obj", Object.class, true)
        };
    Object obj;

    Bar(Object obj) {
        this.obj = obj;
    }
}

public class Read {
    public static void main(String[] args) throws Exception {
        String str = "foo";
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);

        oout.writeObject(str);
        oout.writeObject(str);
        oout.close();

        byte[] buf = bout.toByteArray();
        ByteArrayInputStream bin = new ByteArrayInputStream(buf);
        ObjectInputStream oin = new ObjectInputStream(bin);
        oin.readUnshared();
        try {
            oin.readObject();
            throw new Error();
        } catch (ObjectStreamException ex) {
        }

        bin = new ByteArrayInputStream(buf);
        oin = new ObjectInputStream(bin);
        oin.readUnshared();
        try {
            oin.readUnshared();
            throw new Error();
        } catch (ObjectStreamException ex) {
        }

        bin = new ByteArrayInputStream(buf);
        oin = new ObjectInputStream(bin);
        oin.readObject();
        try {
            oin.readUnshared();
            throw new Error();
        } catch (ObjectStreamException ex) {
        }

        // read in objects written by Write.main()
        FileInputStream in = new FileInputStream("tmp.ser");
        try {
            oin = new ObjectInputStream(in);
            oin.readObject();
            try {
                oin.readObject();
                throw new Error();
            } catch (ObjectStreamException ex) {
            }
        } finally {
            in.close();
        }

        in = new FileInputStream("tmp.ser");
        try {
            oin = new ObjectInputStream(in);
            oin.readObject();
            try {
                oin.readUnshared();
                throw new Error();
            } catch (ObjectStreamException ex) {
            }
        } finally {
            in.close();
        }
    }
}
