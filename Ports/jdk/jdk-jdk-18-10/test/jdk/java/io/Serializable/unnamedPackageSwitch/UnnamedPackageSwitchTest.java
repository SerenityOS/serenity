/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 4348213
 * @build UnnamedPackageSwitchTest pkg.A
 * @run main UnnamedPackageSwitchTest
 * @summary Verify that deserialization allows an incoming class descriptor
 *          representing a class in the unnamed package to be resolved to a
 *          local class with the same name in a named package, and vice-versa.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;

class A implements Serializable {
    private static final long serialVersionUID = 0L;
}

class TestObjectInputStream extends ObjectInputStream {
    TestObjectInputStream(InputStream in) throws IOException { super(in); }
    protected Class<?> resolveClass(ObjectStreamClass desc)
        throws IOException, ClassNotFoundException
    {
        String name = desc.getName();
        if (name.equals("A")) {
            return pkg.A.class;
        } else if (name.equals("pkg.A")) {
            return A.class;
        } else {
            return super.resolveClass(desc);
        }
    }
}

public class UnnamedPackageSwitchTest {
    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(new A());
        oout.writeObject(new pkg.A());
        oout.close();

        ObjectInputStream oin = new TestObjectInputStream(
            new ByteArrayInputStream(bout.toByteArray()));
        oin.readObject();
        oin.readObject();
    }
}
