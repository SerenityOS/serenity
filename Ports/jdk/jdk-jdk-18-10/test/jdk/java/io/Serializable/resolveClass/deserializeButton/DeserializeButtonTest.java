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

/*
 * @test
 * @bug 4413434
 * @library /test/lib
 * @build jdk.test.lib.util.JarUtils Foo
 * @run main DeserializeButtonTest
 * @summary Verify that class loaded outside of application class loader is
 *          correctly resolved during deserialization when read in by custom
 *          readObject() method of a bootstrap class (in this case,
 *          java.util.Vector).
 */

import java.net.URLClassLoader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.util.JarUtils;

public class DeserializeButtonTest {
    public static void main(String[] args) throws Exception {
        setup();

        try (URLClassLoader ldr =
            new URLClassLoader(new URL[]{ new URL("file:cb.jar") })) {
            Runnable r = (Runnable) Class.forName("Foo", true, ldr)
                    .getConstructor().newInstance();
            r.run();
        }
    }

    private static void setup() throws Exception {
        Path classes = Paths.get(System.getProperty("test.classes", ""));
        JarUtils.createJarFile(Paths.get("cb.jar"),
                               classes,
                               classes.resolve("Foo.class"),
                               classes.resolve("Foo$TestElement.class"));
    }
}
