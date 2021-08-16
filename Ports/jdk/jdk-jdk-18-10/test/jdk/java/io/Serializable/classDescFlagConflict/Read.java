/*
 * Copyright (c) 2002, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4632671
 * @summary Verify that reading an object whose class descriptor has both
 *          SC_SERIALIZABLE and SC_EXTERNALIZABLE bits set results in an
 *          InvalidClassException.
 *
 * @build Foo
 * @run main/othervm Read
 */
import java.io.*;

public class Read {
    public static void main(String[] args) throws Exception {
        try {
            /*
             * Foo.ser contains a doctored serialized representation of an
             * instance of class Foo, in which both SC_SERIALIZABLE and
             * SC_EXTERNALIZABLE flags have been set for Foo's class
             * descriptor.
             */
            File f = new File(System.getProperty("test.src", "."), "Foo.ser");
            FileInputStream in = new FileInputStream(f);
            try {
                new ObjectInputStream(in).readObject();
                throw new Error(
                    "read succeeded for object whose class descriptor has " +
                    "both SC_SERIALIZABLE and SC_EXTERNALIZABLE flags set");
            } finally {
                in.close();
            }
        } catch (InvalidClassException e) {
        }
    }
}
