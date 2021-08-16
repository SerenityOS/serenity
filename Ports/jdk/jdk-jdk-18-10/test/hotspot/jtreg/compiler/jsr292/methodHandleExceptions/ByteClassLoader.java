/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package compiler.jsr292.methodHandleExceptions;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * A ByteClassLoader is used to define classes from collections of bytes, as
 * well as loading classes in the usual way. It includes options to write the
 * classes to files in a jar, or to read the classes from jars in a later or
 * debugging run.
 *
 * If Boolean property byteclassloader.verbose is true, be chatty about jar
 * file operations.
 *
 */
public class ByteClassLoader extends URLClassLoader {

    final static boolean verbose
            = Boolean.getBoolean("byteclassloader.verbose");

    final boolean read;
    final JarOutputStream jos;
    final String jar_name;

    /**
     * Make a new ByteClassLoader.
     *
     * @param jar_name  Basename of jar file to be read/written by this classloader.
     * @param read      If true, read classes from jar file instead of from parameter.
     * @param write     If true, write classes to jar files for offline study/use.
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    public ByteClassLoader(String jar_name, boolean read, boolean write)
            throws FileNotFoundException, IOException {
        super(read
                ? new URL[]{new URL("file:" + jar_name + ".jar")}
                : new URL[0]);
        this.read = read;
        this.jar_name = jar_name;
        this.jos = write
                ? new JarOutputStream(
                new BufferedOutputStream(
                new FileOutputStream(jar_name + ".jar"))) : null;
        if (read && write) {
            throw new Error("At most one of read and write may be true.");
        }
    }

    private static void writeJarredFile(JarOutputStream jos, String file, String suffix, byte[] bytes) {
        String fileName = file.replace(".", "/") + "." + suffix;
        JarEntry ze = new JarEntry(fileName);
        try {
            ze.setSize(bytes.length);
            jos.putNextEntry(ze);
            jos.write(bytes);
            jos.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * (pre)load class name using classData for the definition.
     *
     * @param name
     * @param classData
     * @return
     */
    public Class<?> loadBytes(String name, byte[] classData) throws ClassNotFoundException {
        if (jos != null) {
            if (verbose) {
                System.out.println("ByteClassLoader: writing " + name);
            }
            writeJarredFile(jos, name, "class", classData);
        }

        Class<?> clazz = null;
        if (read) {
            if (verbose) {
                System.out.println("ByteClassLoader: reading " + name + " from " + jar_name);
            }
            clazz = loadClass(name);
        } else {
            clazz = defineClass(name, classData, 0, classData.length);
            resolveClass(clazz);
        }
        return clazz;
    }

    public void close() {
        if (jos != null) {
            try {
                if (verbose) {
                    System.out.println("ByteClassLoader: closing " + jar_name);
                }
                jos.close();
            } catch (IOException ex) {
            }
        }
    }
}
