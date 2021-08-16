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
 * @bug 4191941
 * @summary Ensure that original ClassNotFoundException thrown inside of
 *          ObjectInputStream.resolveClass() is preserved (and thrown).
 */

import java.io.*;

class BrokenObjectInputStream extends ObjectInputStream {

    static final String message = "bodega";

    BrokenObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    protected Class<?> resolveClass(ObjectStreamClass desc)
        throws ClassNotFoundException
    {
        throw new ClassNotFoundException(message);
    }
}

public class ResolveClassException {
    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream bout;
        ObjectOutputStream oout;
        ByteArrayInputStream bin;
        BrokenObjectInputStream oin;
        Object obj;

        // write and read an object
        obj = 5;
        bout = new ByteArrayOutputStream();
        oout = new ObjectOutputStream(bout);
        oout.writeObject(obj);
        bin = new ByteArrayInputStream(bout.toByteArray());
        oin = new BrokenObjectInputStream(bin);
        try {
            oin.readObject();
        } catch (ClassNotFoundException e) {
            if (! BrokenObjectInputStream.message.equals(e.getMessage()))
                throw new Error("Original exception not preserved");
        }

        // write and read an array of objects
        obj = new Integer[] { 5 };
        bout = new ByteArrayOutputStream();
        oout = new ObjectOutputStream(bout);
        oout.writeObject(obj);
        bin = new ByteArrayInputStream(bout.toByteArray());
        oin = new BrokenObjectInputStream(bin);
        try {
            oin.readObject();
        } catch (ClassNotFoundException e) {
            if (! BrokenObjectInputStream.message.equals(e.getMessage()))
                throw new Error("Original exception not preserved");
        }
    }
}
