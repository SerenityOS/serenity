/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.*;
import java.util.jar.*;

public class Util {
    /**
     * Define the class as stored in clsFile with the provided lookup instance,
     * with the following modification:
     * <ul>
     *  <li> All ASCII strings in the class file bytes that matches fromString will be replaced with toString.
     *       NOTE: the two strings must be the exact same length.
     * </ul>
     */
    public static Class<?> defineModifiedClass(Lookup lookup, File clsFile, String fromString, String toString)
        throws FileNotFoundException, IOException, NoSuchMethodException, IllegalAccessException,
               InvocationTargetException
    {
      try (DataInputStream dis = new DataInputStream(new FileInputStream(clsFile))) {
        byte[] buff = new byte[(int)clsFile.length()];
        dis.readFully(buff);
        replace(buff, fromString, toString);

        System.out.println("Loading from: " + clsFile + " (" + buff.length + " bytes)");


        // We directly call into Lookup.defineClass() to define the "Super" class. Also,
        // rewrite its classfile so that it returns ___yyy___ instead of ___xxx___. Changing the
        // classfile will guarantee that this class will NOT be loaded from the CDS archive.
        Class<?> cls = lookup.defineClass(buff);
        System.out.println("Loaded : " + cls);

        return cls;
      }
    }

    /**
     * @return the number of occurrences of the <code>from</code> string that
     * have been replaced.
     */
    public static int replace(byte buff[], String from, String to) {
        if (to.length() != from.length()) {
            throw new RuntimeException("bad strings");
        }
        byte f[] = asciibytes(from);
        byte t[] = asciibytes(to);
        byte f0 = f[0];

        int numReplaced = 0;
        int max = buff.length - f.length;
        for (int i=0; i<max; ) {
            if (buff[i] == f0 && replace(buff, f, t, i)) {
                i += f.length;
                numReplaced ++;
            } else {
                i++;
            }
        }
        return numReplaced;
    }

    public static boolean replace(byte buff[], byte f[], byte t[], int i) {
        for (int x=0; x<f.length; x++) {
            if (buff[x+i] != f[x]) {
                return false;
            }
        }
        for (int x=0; x<f.length; x++) {
            buff[x+i] = t[x];
        }
        return true;
    }

    static byte[] asciibytes(String s) {
        byte b[] = new byte[s.length()];
        for (int i=0; i<b.length; i++) {
            b[i] = (byte)s.charAt(i);
        }
        return b;
    }

    public static byte[] getClassFileFromJar(File jarFile, String className) throws FileNotFoundException, IOException {
        JarFile jf = new JarFile(jarFile);
        JarEntry ent = jf.getJarEntry(className.replace('.', '/') + ".class");

        try (DataInputStream dis = new DataInputStream(jf.getInputStream(ent))) {
            byte[] buff = new byte[(int)ent.getSize()];
            dis.readFully(buff);
            return buff;
        }
    }
}
