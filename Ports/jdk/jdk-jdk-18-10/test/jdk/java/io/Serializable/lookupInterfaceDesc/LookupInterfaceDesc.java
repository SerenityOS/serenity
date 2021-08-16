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
 * @bug 4402227
 * @summary Verify that ObjectStreamClass.lookup() functions properly
 *          for interfaces.
 */

import java.io.*;

interface Foo extends Serializable {
    @SuppressWarnings("serial") /* Incorrect declarations are being tested */
    static final long serialVersionUID = 0xCAFE;
}

interface Bar extends Externalizable {
    @SuppressWarnings("serial") /* Incorrect declarations are being tested */
    static final long serialVersionUID = 0xBABE;
}

interface Gub {
}

public class LookupInterfaceDesc {
    public static void main(String[] args) throws Exception {
        ObjectStreamClass desc = ObjectStreamClass.lookup(Foo.class);
        if ((desc.getSerialVersionUID() != Foo.serialVersionUID) ||
            (desc.getFields().length != 0))
        {
            throw new Error();
        }

        desc = ObjectStreamClass.lookup(Bar.class);
        if ((desc.getSerialVersionUID() != Bar.serialVersionUID) ||
            (desc.getFields().length != 0))
        {
            throw new Error();
        }

        if (ObjectStreamClass.lookup(Gub.class) != null) {
            throw new Error();
        }
    }
}
