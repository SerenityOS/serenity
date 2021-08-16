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
 * @summary Verify that ObjectInputStream can skip over unresolvable serialized
 *          proxy instances.
 */

import java.io.*;
import java.lang.reflect.*;

class A implements Serializable {
    private static final long serialVersionUID = 0L;
    String a;
    String z;
}

class B implements Serializable {
    private static final long serialVersionUID = 0L;
    String s;

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        // leave proxy object unconsumed
    }
}

public class Read {
    public static void main(String[] args) throws Exception {
        FileInputStream in = new FileInputStream("tmp.ser");
        try {
            ObjectInputStream oin = new ObjectInputStream(in);
            A a = (A) oin.readObject();
            if (! (a.a.equals("a") && a.z.equals("z"))) {
                throw new Error("A fields corrupted");
            }
            B b = (B) oin.readObject();
            if (! b.s.equals("s")) {
                throw new Error("B fields corrupted");
            }
            try {
                oin.readObject();
                throw new Error("proxy read should not succeed");
            } catch (ClassNotFoundException ex) {
            }
        } finally {
            in.close();
        }
    }
}
