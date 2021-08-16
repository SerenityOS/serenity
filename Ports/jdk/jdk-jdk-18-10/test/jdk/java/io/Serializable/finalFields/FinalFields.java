/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4174797
 * @summary Ensure that ObjectInputStream can set final fields.
 */

import java.io.*;

class Foo implements Serializable {
    private static final long serialVersionUID = 1L;

    final int i;

    Foo(int i) {
        this.i = i;
    }

    public boolean equals(Object obj) {
        if (! (obj instanceof Foo))
            return false;
        Foo f = (Foo) obj;
        return (i == f.i);
    }

    public int hashCode() {
        return i;
    }
}

public class FinalFields {
    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream bout;
        ByteArrayInputStream bin;
        ObjectOutputStream oout;
        ObjectInputStream oin;
        Foo f1, f2, f1copy, f2copy;

        bout = new ByteArrayOutputStream();
        oout = new ObjectOutputStream(bout);
        f1 = new Foo(1);
        f2 = new Foo(2);

        oout.writeObject(f1);
        oout.writeObject(f2);
        oout.flush();

        bin = new ByteArrayInputStream(bout.toByteArray());
        oin = new ObjectInputStream(bin);
        f1copy = (Foo) oin.readObject();
        f2copy = (Foo) oin.readObject();

        if (! (f1.equals(f1copy) && f2.equals(f2copy)))
            throw new Error("copies don't match originals");
    }

}
