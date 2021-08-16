/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4838379
 * @summary Verify that serialVersionUID and serialPersistentFields
 *          declarations made by enum types and constants are ignored.
 */

import java.io.*;
import java.util.Arrays;

enum Foo {

    foo,
    bar {
        @SuppressWarnings("serial") /* Incorrect declarations are being tested */
        private static final long serialVersionUID = 2L;
        // bar is implemented as an inner class instance, so the following
        // declaration would cause a compile-time error
        // private static final ObjectStreamField[] serialPersistentFields = {
        //    new ObjectStreamField("gub", Float.TYPE)
        // };
    };

    @SuppressWarnings("serial") /* Incorrect declarations are being tested */
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("serial") /* Incorrect declarations are being tested */
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("blargh", Integer.TYPE)
    };
}

public class Test {
    public static void main(String[] args) throws Exception {
        Class<?>[] classes =
            { Foo.class, Foo.foo.getClass(), Foo.bar.getClass() };
        for (int i = 0; i < classes.length; i++) {
            ObjectStreamClass desc = ObjectStreamClass.lookup(classes[i]);
            if (desc.getSerialVersionUID() != 0L) {
                throw new Error(
                    classes[i] + " has non-zero serialVersionUID: " +
                    desc.getSerialVersionUID());
            }
            ObjectStreamField[] fields = desc.getFields();
            if (fields.length > 0) {
                throw new Error(
                    classes[i] + " has non-empty list of fields: " +
                    Arrays.asList(fields));
            }
        }
    }
}
