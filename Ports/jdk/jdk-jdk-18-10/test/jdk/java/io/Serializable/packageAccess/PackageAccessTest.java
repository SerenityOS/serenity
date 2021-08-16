/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4765255
 * @library /test/lib
 * @build jdk.test.lib.util.JarUtils A B C D PackageAccessTest
 * @run main PackageAccessTest
 * @summary Verify proper functioning of package equality checks used to
 *          determine accessibility of superclass constructor and inherited
 *          writeReplace/readResolve methods.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.InvalidClassException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.util.JarUtils;

public class PackageAccessTest {

    static Class<?> bcl;
    static Class<?> dcl;

    public static void main(String[] args) throws Exception {
        setup();

        try (URLClassLoader ldr =
            new URLClassLoader(new URL[]{ new URL("file:foo.jar") },
                    PackageAccessTest.class.getClassLoader())) {
            bcl = Class.forName("B", true, ldr);
            dcl = Class.forName("D", true, ldr);

            Object b = bcl.getConstructor().newInstance();
            try {
                swizzle(b);
                throw new Error("expected InvalidClassException for class B");
            } catch (InvalidClassException e) {
                System.out.println("caught " + e);
                e.printStackTrace();
            }
            if (A.packagePrivateConstructorInvoked) {
                throw new Error("package private constructor of A invoked");
            }

            Object d = dcl.getConstructor().newInstance();
            swizzle(d);
        }
    }

    static void swizzle(Object obj) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(obj);
        oout.close();
        ByteArrayInputStream bin =
            new ByteArrayInputStream(bout.toByteArray());
        new TestObjectInputStream(bin).readObject();
    }

    static void setup() throws Exception {
        Path classes = Paths.get(System.getProperty("test.classes", ""));
        JarUtils.createJarFile(Paths.get("foo.jar"), classes,
                classes.resolve("B.class"), classes.resolve("D.class"));
        Files.delete(classes.resolve("B.class"));
        Files.delete(classes.resolve("D.class"));
    }
}

class TestObjectInputStream extends ObjectInputStream {
    TestObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    protected Class<?> resolveClass(ObjectStreamClass desc)
        throws IOException, ClassNotFoundException
    {
        String n = desc.getName();
        if (n.equals("B")) {
            return PackageAccessTest.bcl;
        } else if (n.equals("D")) {
            return PackageAccessTest.dcl;
        } else {
            return super.resolveClass(desc);
        }
    }
}
