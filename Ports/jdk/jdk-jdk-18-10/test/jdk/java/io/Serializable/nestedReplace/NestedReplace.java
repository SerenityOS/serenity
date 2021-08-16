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
 * @bug 4217737
 * @clean NestedReplace A B C D
 * @build NestedReplace
 * @run main NestedReplace
 * @summary Ensure that replacement objects can nominate their own replacements,
 *          so long as the replacement is not the same class as the
 *          just-replaced object.
 *
 */

import java.io.*;

class A implements Serializable {
    private static final long serialVersionUID = 1L;

    Object writeReplace() throws ObjectStreamException {
        return new B();
    }
}

class B implements Serializable {
    private static final long serialVersionUID = 1L;

    Object writeReplace() throws ObjectStreamException {
        return new C();
    }
}

class C implements Serializable {
    private static final long serialVersionUID = 1L;

    static int writeReplaceCalled = 0;

    Object writeReplace() throws ObjectStreamException {
        writeReplaceCalled++;
        return new C();
    }

    Object readResolve() throws ObjectStreamException {
        return new D();
    }
}

class D implements Serializable {
    private static final long serialVersionUID = 1L;

    Object readResolve() throws ObjectStreamException {
        throw new Error("readResolve() called more than once");
    }
}

public class NestedReplace {
    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream bout;
        ObjectOutputStream oout;
        ByteArrayInputStream bin;
        ObjectInputStream oin;
        Object obj;

        bout = new ByteArrayOutputStream();
        oout = new ObjectOutputStream(bout);
        oout.writeObject(new A());
        oout.flush();
        bin = new ByteArrayInputStream(bout.toByteArray());
        oin = new ObjectInputStream(bin);
        obj = oin.readObject();

        if (! (obj instanceof D))
            throw new Error("Deserialized object is of wrong class");
        if (C.writeReplaceCalled != 1)
            throw new Error("C.writeReplace() should only get called once");
    }
}
