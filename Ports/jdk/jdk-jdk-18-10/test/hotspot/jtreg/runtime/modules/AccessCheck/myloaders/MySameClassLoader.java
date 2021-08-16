/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
package myloaders;

import java.io.*;
import java.lang.module.ModuleReference;
import jdk.test.lib.classloader.ClassUnloadCommon;

// Declare a MySameClassLoader class to be used to map modules to the same
// class loader.  This class loader will also be used to load classes
// within modules.
public class MySameClassLoader extends ClassLoader
{
    public static MySameClassLoader loader1 = new MySameClassLoader();

    public Class loadClass(String name) throws ClassNotFoundException {
        // override all classloaders
        if (!name.equals("p1.c1") &&
            !name.equals("p1.c1ReadEdge") &&
            !name.equals("p1.c1Loose") &&
            !name.equals("p2.c2") &&
            !name.equals("p3.c3") &&
            !name.equals("p3.c3ReadEdge") &&
            !name.equals("c4") &&
            !name.equals("c5") &&
            !name.equals("p6.c6")) {
            return super.loadClass(name);
        }
        byte[] data = ClassUnloadCommon.getClassData(name);
        return defineClass(name, data, 0, data.length);
    }

    public void register(ModuleReference mref) { }
}
