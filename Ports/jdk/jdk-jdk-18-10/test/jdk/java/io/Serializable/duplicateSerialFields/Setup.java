/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4764280
 * @summary Verify that if a serializable class declares multiple
 *          serialPersistentFields that share the same name, calling
 *          ObjectStreamClass.lookup() for that class will not result in an
 *          InternalError, and that attempts at default serialization or
 *          deserialization of such a class will result in
 *          InvalidClassExceptions.
 */

import java.io.*;

class A implements Serializable {
    private static final long serialVersionUID = 0L;
    int i;
}

class B implements Serializable {
    private static final long serialVersionUID = 0L;
    int i;
}

public class Setup {
    public static void main(String[] args) throws Exception {
        ObjectOutputStream oout =
            new ObjectOutputStream(new FileOutputStream("a.ser"));
        oout.writeObject(new A());
        oout.close();

        oout = new ObjectOutputStream(new FileOutputStream("b.ser"));
        oout.writeObject(new B());
        oout.close();
    }
}
