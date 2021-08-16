/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Verify that unshared write and read operations work properly with
 *          enum constants.
 */

import java.io.*;

enum Foo { foo, bar, baz }

abstract class WriteReadTest {

    abstract void write(ObjectOutputStream out) throws Exception;
    abstract void read(ObjectInputStream in) throws Exception;

    void run() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        write(oout);
        oout.close();
        read(new ObjectInputStream(
            new ByteArrayInputStream(bout.toByteArray())));
    }
}

public class Test {
    public static void main(String[] args) throws Exception {
        WriteReadTest[] tests = {
            new WriteReadTest() {
                void write(ObjectOutputStream out) throws Exception {
                    out.writeObject(Foo.foo);
                    out.writeObject(Foo.foo);
                }
                void read(ObjectInputStream in) throws Exception {
                    Object obj = in.readObject();
                    if (obj != Foo.foo) {
                        throw new Error(
                            "expected " + Foo.foo + " instead of " + obj);
                    }
                    try {
                        obj = in.readUnshared();
                        throw new Error(
                            "read of " + obj + " should not have succeeded");
                    } catch (ObjectStreamException e) {
                        System.out.println("caught expected exception " + e);
                    }
                }
            },
            new WriteReadTest() {
                void write(ObjectOutputStream out) throws Exception {
                    out.writeObject(Foo.foo);
                    out.writeObject(Foo.foo);
                }
                void read(ObjectInputStream in) throws Exception {
                    Object obj = in.readUnshared();
                    if (obj != Foo.foo) {
                        throw new Error(
                            "expected " + Foo.foo + " instead of " + obj);
                    }
                    try {
                        obj = in.readObject();
                        throw new Error(
                            "read of " + obj + " should not have succeeded");
                    } catch (ObjectStreamException e) {
                        System.out.println("caught expected exception " + e);
                    }
                }
            },
            new WriteReadTest() {
                void write(ObjectOutputStream out) throws Exception {
                    out.writeObject(Foo.foo);
                    out.writeUnshared(Foo.foo);
                }
                void read(ObjectInputStream in) throws Exception {
                    Object obj = in.readUnshared();
                    if (obj != Foo.foo) {
                        throw new Error(
                            "expected " + Foo.foo + " instead of " + obj);
                    }
                    obj = in.readUnshared();
                    if (obj != Foo.foo) {
                        throw new Error(
                            "expected " + Foo.foo + " instead of " + obj);
                    }
                }
            },
            new WriteReadTest() {
                void write(ObjectOutputStream out) throws Exception {
                    out.writeUnshared(Foo.foo);
                    out.writeObject(Foo.foo);
                }
                void read(ObjectInputStream in) throws Exception {
                    Object obj = in.readUnshared();
                    if (obj != Foo.foo) {
                        throw new Error(
                            "expected " + Foo.foo + " instead of " + obj);
                    }
                    obj = in.readUnshared();
                    if (obj != Foo.foo) {
                        throw new Error(
                            "expected " + Foo.foo + " instead of " + obj);
                    }
                }
            }
        };
        for (int i = 0; i < tests.length; i++) {
            tests[i].run();
        }
    }
}
