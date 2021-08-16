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
 * @bug 4402830 4427881
 *
 * @clean Write Read Read2 Foo
 * @compile Write.java
 * @run main Write
 * @clean Write Foo
 * @compile Read.java
 * @run main Read
 * @clean Read Foo
 * @compile Read2.java
 * @run main Read2
 * @clean Read2 Foo
 *
 * @summary Verify proper basic functionality of the
 *          ObjectInputStream.GetField API
 */

import java.io.*;

class Foo implements Serializable {
    private static final long serialVersionUID = 0L;

    boolean z = true;
    byte b = 5;
    char c = '5';
    short s = 5;
    int i = 5;
    long j = 5;
    float f = 5.0f;
    double d = 5.0;
    String str = "5";
}

public class Write {
    public static void main(String[] args) throws Exception {
        ObjectOutputStream oout =
            new ObjectOutputStream(new FileOutputStream("tmp.ser"));
        oout.writeObject(new Foo());
        oout.close();
    }
}
