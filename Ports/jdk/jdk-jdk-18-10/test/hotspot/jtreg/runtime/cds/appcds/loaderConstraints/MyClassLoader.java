/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.io.IOException;

public class MyClassLoader extends ClassLoader {
    ClassLoader parent;
    ClassLoader appLoader;
    public MyClassLoader(ClassLoader parent, ClassLoader appLoader) {
        super(parent);
        this.parent = parent;
        this.appLoader = appLoader;
    }

    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        System.out.println("MyClassLoader: loadClass(\"" + name + "\", " + resolve + ")");
        Class c;

        if (name.equals("MyHttpHandlerC")) {
            byte[] bytes;
            try (InputStream is = appLoader.getResourceAsStream("MyHttpHandlerC.class")) {
                bytes = is.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException("Unexpected", e);
            }
            c = defineClass(name, bytes, 0, bytes.length);
        } else {
            c = super.loadClass(name, resolve);
        }
        System.out.println("MyClassLoader: loaded " + name + " = " + c);
        return c;
    }
}

