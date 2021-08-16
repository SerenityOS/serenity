/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @clean A WriteObjectMemory
 * @run main WriteObjectMemory
 * @bug 4146453 5011410
 * @summary Test that regrow of object/handle table of ObjectOutputStream works.
 */

import java.io.*;
import java.util.HashSet;
import java.util.Iterator;

class A implements Serializable {
    private static final long serialVersionUID = 1L;

    static HashSet<A>writeObjectExtent = new HashSet<>();

    private void writeObject(ObjectOutputStream out) throws IOException {
        if (writeObjectExtent.contains(this)) {
            throw new InvalidObjectException("writeObject: object " +
                                             this.toString() + " has already "
                                             + "been serialized and should " +
                                             "have be serialized by reference.");
        } else {
            writeObjectExtent.add(this);
        }
        out.defaultWriteObject();
    }

    A() {
    }
}

public class WriteObjectMemory {
    public static void main(String args[])
        throws IOException
    {
        ObjectOutputStream out =
            new ObjectOutputStream(new ByteArrayOutputStream(3000));
        for (int i = 0; i < 1000; i++) {
            out.writeObject(new A());
        }

        // Make sure that serialization subsystem does not
        // allow writeObject to be called on any objects that
        // have already been serialized. These objects should be
        // written out by reference.
        Iterator<A> iter = A.writeObjectExtent.iterator();
        while (iter.hasNext()) {
            out.writeObject(iter.next());
        }

        out.close();
    }
}
