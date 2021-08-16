/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.classload;

import java.io.*;
import java.util.*;

import nsk.share.FileUtils;

/**
 * Custom classloader that does not delegate to it's parent.
 *
 * It can load classes from classpath that have name containing
 * "Class" (any package).
 */
public class ClassPathNonDelegatingClassLoader extends ClassLoader {
        private String [] classPath;

        public ClassPathNonDelegatingClassLoader() {
                classPath = System.getProperty("java.class.path").split(File.pathSeparator);
        }

        public synchronized Class loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
                Class c = findLoadedClass(name);
                if (c != null) {
                        System.out.println("Found class: " + name);
                        return c;
                }
                if (name.contains("Class")) {
                        String newName = name.replace('.', '/');
                        return loadClassFromFile(name, newName + ".class", resolve);
                } else {
                        return findSystemClass(name);
                }
        }

        private Class loadClassFromFile(String name, String fname, boolean resolve)
                throws ClassNotFoundException {
                try {
                        File target = new File("");

                        for(int i = 0; i < classPath.length; ++i) {
                                target = new File(classPath[i] + File.separator + fname);
                                if (target.exists())
                                        break;
                        }
                        if (!target.exists())
                                throw new java.io.FileNotFoundException();
                        byte[] buffer = FileUtils.readFile(target);
                        Class c = defineClass(name, buffer, 0, buffer.length);
                        if (resolve)
                                resolveClass(c);
                        return c;
                } catch (IOException e) {
                        throw new ClassNotFoundException("Exception while reading classfile data", e);
                }
        }
}
