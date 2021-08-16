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

/* @test
 *
 * @clean Write Read Foo
 * @compile Write.java
 * @run main Write
 * @clean Write Read Foo
 * @compile Read.java
 * @run main Read
 *
 * @summary Verify that the ObjectOutputStream.PutField API works as
 *          advertised.
 */

import java.io.*;

class Foo implements Serializable {
    private static final long serialVersionUID = 0L;
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("z", boolean.class),
        new ObjectStreamField("b", byte.class),
        new ObjectStreamField("c", char.class),
        new ObjectStreamField("s", short.class),
        new ObjectStreamField("i", int.class),
        new ObjectStreamField("j", long.class),
        new ObjectStreamField("f", float.class),
        new ObjectStreamField("d", double.class),
        new ObjectStreamField("str", String.class),
    };

    private void writeObject(ObjectOutputStream out) throws IOException {
        ObjectOutputStream.PutField fields = out.putFields();
        fields.put("z", true);
        fields.put("b", (byte) 5);
        fields.put("c", '5');
        fields.put("s", (short) 5);
        fields.put("i", 5);
        fields.put("j", 5l);
        fields.put("f", 5.0f);
        fields.put("d", 5.0);
        fields.put("str", "5");
        out.writeFields();
    }
}

public class Write {
    public static void main(String[] args) throws Exception {
        ObjectOutputStream oout =
            new ObjectOutputStream(new FileOutputStream("tmp.ser"));
        oout.writeObject(new Foo());
        oout.close();
    }
}
