/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.DataInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * A factory that generates a class with many methods.
 */
public class ClassWithManyMethodsClassLoader extends ClassLoader {
    /**
     * Used to enable/disable keeping the class files and java sources for
     * the generated classes.
     */
    private static boolean deleteFiles = Boolean.parseBoolean(
        System.getProperty("ClassWithManyMethodsClassLoader.deleteFiles", "true"));

    private JavaCompiler javac;

    public ClassWithManyMethodsClassLoader() {
        javac = ToolProvider.getSystemJavaCompiler();
    }

    private String generateSource(String className, String methodPrefix, int methodCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("public class ")
            .append(className)
            .append("{\n");

        for (int i = 0; i < methodCount; i++) {
            sb.append("public void ")
                .append(methodPrefix)
                .append(i)
                .append("() {}\n");
        }

        sb.append("\n}");

        return sb.toString();
    }

    private byte[] generateClassBytes(String className, String methodPrefix, int methodCount) throws IOException {
        String src = generateSource(className, methodPrefix, methodCount);
        File file = new File(className + ".java");
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.append(src);
            pw.flush();
        }
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exitcode = javac.run(null, null, err, file.getCanonicalPath());
        if (exitcode != 0) {
            // Print Error
            System.err.print(err);
            if (err.toString().contains("java.lang.OutOfMemoryError: Java heap space")) {
                throw new OutOfMemoryError("javac failed with resources exhausted");
            } else {
              throw new RuntimeException("javac failure when compiling: " +
                      file.getCanonicalPath());
            }
        } else {
            if (deleteFiles) {
                file.delete();
            }
        }

        File classFile = new File(className + ".class");
        byte[] bytes;
        try (DataInputStream dis = new DataInputStream(new FileInputStream(classFile))) {
            bytes = new byte[dis.available()];
            dis.readFully(bytes);
        }
        if (deleteFiles) {
            classFile.delete();
        }

        return bytes;
    }

    public Class<?> create(String className, String methodPrefix, int methodCount) throws IOException {
        byte[] bytes = generateClassBytes(className, methodPrefix, methodCount);
        return defineClass(className, bytes, 0, bytes.length);
    }
}
