/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.test.lib.classloader.ClassUnloadCommon;

public class MyDiffClassLoader extends ClassLoader {

    public String loaderName;
    public static boolean switchClassData = false;

    MyDiffClassLoader(String name) {
        this.loaderName = name;
    }

    public Class loadClass(String name) throws ClassNotFoundException {
        if (!name.contains("c1r") &&
            !name.contains("c1c") &&
            !name.contains("c1s") &&
            !name.equals("p2.c2")) {
                return super.loadClass(name);
        }

        // new loader loads p2.c2
        if  (name.equals("p2.c2") && !loaderName.equals("C2Loader")) {
            Class<?> c = new MyDiffClassLoader("C2Loader").loadClass(name);
            switchClassData = true;
            return c;
        }

        byte[] data = switchClassData ? getNewClassData(name) : ClassUnloadCommon.getClassData(name);
        System.out.println("name is " + name);
        return defineClass(name, data, 0, data.length);
    }

    // Return p2.c2 with everything removed
    byte[] getNewClassData(String name) {
        return InMemoryJavaCompiler.compile("p2.c2", "package p2; public class c2 { }");
    }
}
