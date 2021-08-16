/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @clean Write Read A B I Handler
 * @compile Write.java Handler.java
 * @run main Write
 * @clean Write Read A B I Handler
 * @compile Read.java Handler.java
 * @run main Read
 *
 * @summary Verify that ObjectInputStream can skip over unresolvable serialized
 *          proxy instances.
 */

import java.io.*;
import java.lang.reflect.*;

interface I {}          // interface present only on writing side

class A implements Serializable {
    private static final long serialVersionUID = 0L;
    String a = "a";
    @SuppressWarnings("serial") /* Incorrect declarations are being tested */
    Object proxy;
    String z = "z";

    A(Object proxy) { this.proxy = proxy; }
}

class B implements Serializable {
    private static final long serialVersionUID = 0L;
    String s = "s";
    transient Object proxy;

    B(Object proxy) { this.proxy = proxy; }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(proxy);
    }
}

public class Write {
    public static void main(String[] args) throws Exception {
        Object proxy = Proxy.newProxyInstance(
            Write.class.getClassLoader(),
            new Class<?>[] { I.class }, new Handler());
        ObjectOutputStream oout = new ObjectOutputStream(
            new FileOutputStream("tmp.ser"));
        oout.writeObject(new A(proxy));
        oout.reset();
        oout.writeObject(new B(proxy));
        oout.writeObject(proxy);
        oout.close();
    }
}
