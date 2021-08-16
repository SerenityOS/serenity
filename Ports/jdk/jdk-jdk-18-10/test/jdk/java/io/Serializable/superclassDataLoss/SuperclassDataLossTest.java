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
 * @bug 4325590
 * @library /test/lib
 * @build jdk.test.lib.util.JarUtils A B
 * @run main SuperclassDataLossTest
 * @summary Verify that superclass data is not lost when incoming superclass
 *          descriptor is matched with local class that is not a superclass of
 *          the deserialized instance's class.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import jdk.test.lib.util.JarUtils;

class MixedSuperclassStream extends ObjectInputStream {
    private boolean ldr12A;
    private URLClassLoader ldr1;
    private URLClassLoader ldr2;

    MixedSuperclassStream(InputStream in, URLClassLoader ldr1,
            URLClassLoader ldr2, boolean ldr1First) throws IOException {
        super(in);
        this.ldr1 = ldr1;
        this.ldr2 = ldr2;
        this.ldr12A = ldr12A;
    }

    protected Class<?> resolveClass(ObjectStreamClass desc)
        throws IOException, ClassNotFoundException
    {
        // resolve A's classdesc to class != B's superclass
        String name = desc.getName();
        if (ldr12A) {
            if (name.equals("A")) {
                return Class.forName(name, true, ldr1);
            } else if (name.equals("B")) {
                return Class.forName(name, true, ldr2);
            }
        } else {
            if (name.equals("B")) {
                return Class.forName(name, true, ldr1);
            } else if (name.equals("A")) {
                return Class.forName(name, true, ldr2);
            }
        }
        return super.resolveClass(desc);
    }
}

public class SuperclassDataLossTest {

    public static void main(String[] args) throws Exception {
        try (URLClassLoader ldr1 = new URLClassLoader(new URL[] { new URL("file:cb1.jar") });
             URLClassLoader ldr2 = new URLClassLoader(new URL[] { new URL("file:cb2.jar") })) {
            setup();

            Runnable a = (Runnable) Class.forName("B", true, ldr1)
                    .getConstructor().newInstance();
            a.run();

            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(bout);
            oout.writeObject(a);
            oout.close();

            test(bout, ldr1, ldr2, true);
            test(bout, ldr1, ldr2, false);
        }
    }

    private static void test(ByteArrayOutputStream bout, URLClassLoader ldr1,
                             URLClassLoader ldr2, boolean ldr12A) throws Exception {
        ByteArrayInputStream bin =
            new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream oin = new MixedSuperclassStream(bin, ldr1, ldr2, ldr12A);
        Runnable a = (Runnable) oin.readObject();
        a.run();
    }

    private static void setup() throws Exception {
        Path classes = Paths.get(System.getProperty("test.classes", ""));
        JarUtils.createJarFile(Paths.get("cb1.jar"), classes,
                classes.resolve("A.class"), classes.resolve("B.class"));
        Files.copy(Paths.get("cb1.jar"), Paths.get("cb2.jar"),
                   StandardCopyOption.REPLACE_EXISTING);
    }
}
