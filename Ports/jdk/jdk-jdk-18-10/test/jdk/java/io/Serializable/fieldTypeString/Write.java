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
 * @bug 4404696
 *
 * @clean Write Read Foo Bar
 * @compile Write.java
 * @run main Write
 * @clean Write Read Foo Bar
 * @compile Read.java
 * @run main Read
 *
 * @summary Verify that serialization does not require matching type strings
 *          for non-primitive fields.
 *
 * NOTE: This test should be removed if it is determined that serialization
 * *should* consider type strings when matching non-primitive fields.
 */

import java.io.*;

class Foo implements Serializable {
    private static final long serialVersionUID = 0L;
    @SuppressWarnings("serial") /* Incorrect declarations are being tested */
    Object obj;

    Foo(Object obj) {
        this.obj = obj;
    }
}

class Bar implements Serializable {
    private static final long serialVersionUID = 0L;
    int q;
}

public class Write {
    public static void main(String[] args) throws Exception {
        ObjectOutputStream oout =
            new ObjectOutputStream(new FileOutputStream("foo.ser"));
        oout.writeObject(new Foo("foo"));
        oout.writeObject(new Foo(0));
        oout.close();

        oout = new ObjectOutputStream(new FileOutputStream("bar.ser"));
        oout.writeObject(new Bar());
        oout.close();
    }
}
