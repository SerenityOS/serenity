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
 * @bug 4461299
 * @summary Verify that serialization functions properly if
 *          ObjectInputStream.readClassDescriptor() returns a local class
 *          descriptor for which the serialVersionUID has not yet been
 *          calculated.
 */

import java.io.*;
import java.util.*;

class LoopbackOutputStream extends ObjectOutputStream {
    LinkedList<ObjectStreamClass> descs;

    LoopbackOutputStream(OutputStream out, LinkedList<ObjectStreamClass> descs)
        throws IOException
    {
        super(out);
        this.descs = descs;
    }

    protected void writeClassDescriptor(ObjectStreamClass desc)
        throws IOException
    {
        descs.add(desc);
    }
}

class LoopbackInputStream extends ObjectInputStream {
    LinkedList<ObjectStreamClass> descs;

    LoopbackInputStream(InputStream in, LinkedList<ObjectStreamClass> descs) throws IOException {
        super(in);
        this.descs = descs;
    }

    protected ObjectStreamClass readClassDescriptor()
    {
        return descs.removeFirst();
    }
}

public class Loopback implements Serializable {
    private static final long serialVersionUID = 1L;

    String str;

    Loopback(String str) {
        this.str = str;
    }

    public static void main(String[] args) throws Exception {
        Loopback lb = new Loopback("foo");
        LinkedList<ObjectStreamClass> descs = new LinkedList<>();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        LoopbackOutputStream lout = new LoopbackOutputStream(bout, descs);
        lout.writeObject(lb);
        lout.close();

        LoopbackInputStream lin = new LoopbackInputStream(
            new ByteArrayInputStream(bout.toByteArray()), descs);
        Loopback lbcopy = (Loopback) lin.readObject();
        if (!lb.str.equals(lbcopy.str)) {
            throw new Error();
        }
    }
}
