/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.test.lib.classloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;

public class ClassLoadUtils {

    private ClassLoadUtils() {
    }

    /**
     * Get filename of class file from classpath for given class name.
     *
     * @param className class name
     * @return filename or null if not found
     */
    public static String getClassPath(String className) {
        String fileName = className.replace(".", File.separator) + ".class";
        String[] classPath = System.getProperty("java.class.path").split(File.pathSeparator);
        File target = null;
        int i;
        for (i = 0; i < classPath.length; ++i) {
            target = new File(classPath[i] + File.separator + fileName);
            System.out.println("Try: " + target);
            if (target.exists()) {
                break;
            }
        }
        if (i != classPath.length) {
            return classPath[i];
        }
        return null;
    }

    /**
     * Get filename of class file from classpath for given class name.
     *
     * @param className class name
     * @return filename or null if not found
     */
    public static String getClassPathFileName(String className) {
        String fileName = className.replace(".", File.separator) + ".class";
        String[] classPath = System.getProperty("java.class.path").split(File.pathSeparator);
        File target = null;
        int i;
        for (i = 0; i < classPath.length; ++i) {
            target = new File(classPath[i] + File.separator + fileName);
            System.out.println("Try: " + target);
            if (target.exists()) {
                break;
            }
        }
        if (i != classPath.length) {
            try {
                return target.getCanonicalPath();
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    public static String getRedefineClassFileName(String dir, String className) {
        String fileName = getClassPathFileName(className);
        if (fileName == null) {
            return null;
        }
        if (fileName.contains("classes")) {
            return fileName.replace("classes", dir);
        } else {
            String classPath = getClassPath(className);
            if (classPath != null) {
                return classPath + File.separator + "newclass" + File.separator + className.replace(".", File.separator) + ".class";
            } else {
                return null;
            }
        }
    }

    /**
     * Get filename of class file which is to be redefined.
     */
    public static String getRedefineClassFileName(String className) {
        return getRedefineClassFileName("newclass", className);
    }

    /**
     * Read whole file.
     *
     * @param file file
     * @return contents of file as byte array
     */
    public static byte[] readFile(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        long countl = file.length();
        if (countl > Integer.MAX_VALUE) {
            throw new IOException("File is too huge");
        }
        int count = (int) countl;
        byte[] buffer = new byte[count];
        int n = 0;
        try {
            while (n < count) {
                int k = in.read(buffer, n, count - n);
                if (k < 0) {
                    throw new IOException("Unexpected EOF");
                }
                n += k;
            }
        } finally {
            in.close();
        }
        return buffer;
    }

    /**
     * Read whole file.
     *
     * @param name file name
     * @return contents of file as byte array
     */
    public static byte[] readFile(String name) throws IOException {
        return readFile(new File(name));
    }
}
