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
 * @bug 4453723
 *
 * @clean Write2 Read2 Foo
 * @compile Write2.java
 * @run main Write2
 * @clean Write2 Read2 Foo
 * @compile Read2.java
 * @run main Read2
 *
 * @summary Verify that ObjectOutputStream.PutField.write() works for objects
 *          that do not define primitive serializable fields.
 */

import java.io.*;

class Foo implements Serializable {
    private static final long serialVersionUID = 0L;
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("s1", String.class),
        new ObjectStreamField("s2", String.class)
    };

    @SuppressWarnings("deprecation")
    private void writeObject(ObjectOutputStream out) throws IOException {
        ObjectOutputStream.PutField fields = out.putFields();
        fields.put("s1", "qwerty");
        fields.put("s2", "asdfg");
        fields.write(out);
    }
}

public class Write2 {
    public static void main(String[] args) throws Exception {
        ObjectOutputStream oout =
            new ObjectOutputStream(new FileOutputStream("tmp.ser"));
        oout.writeObject(new Foo());
        oout.close();
    }
}
