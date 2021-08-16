/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * A class loader that generates new classes.
 * The generated classes are made by first emitting java sources with nested
 * static classes, these are then compiled and the class files are read back.
 * Some efforts are made to make the class instances unique and of not insignificant
 * size.
 */
public class GeneratingCompilingClassLoader extends ClassLoader {
    /**
     * Holds a pair of class bytecodes and class name (for use with defineClass).
     */
    private static class GeneratedClass {
        public byte[] bytes;
        public String name;
        public GeneratedClass(byte[] bytes, String name) {
            this.bytes = bytes; this.name = name;
        }
    }

    /**
     * Used to uniquely name every class generated.
     */
    private static int count = 0;
    /**
     * Used to enable/disable keeping the class files and java sources for
     * the generated classes.
     */
    private static boolean deleteFiles = Boolean.parseBoolean(
        System.getProperty("GeneratingCompilingClassLoader.deleteFiles", "true"));

    private static String bigstr =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
        + "In facilisis scelerisque vehicula. Donec congue nisi a "
        + "leo posuere placerat lobortis felis ultrices. Pellentesque "
        + "habitant morbi tristique senectus et netus et malesuada "
        + "fames ac turpis egestas. Nam tristique velit at felis "
        + "iaculis at tempor sem vestibulum. Sed adipiscing lectus "
        + "non mi molestie sagittis. Morbi eu purus urna. Nam tempor "
        + "tristique massa eget semper. Mauris cursus, nulla et ornare "
        + "vehicula, leo dolor scelerisque metus, sit amet rutrum erat "
        + "sapien quis dui. Nullam eleifend risus et velit accumsan sed "
        + "suscipit felis pulvinar. Nullam faucibus suscipit gravida. "
        + "Pellentesque habitant morbi tristique senectus et netus et "
        + "malesuada fames ac turpis egestas. Nullam ut massa augue, "
        + "nec viverra mauris.";

    private static int getNextCount() {
        return count++;
    }

    ////// end statics

    private JavaCompiler javac;
    private String nameBase;

    public GeneratingCompilingClassLoader() {
        javac = ToolProvider.getSystemJavaCompiler();
        nameBase = "TestSimpleClass";
    }

    private long getBigValue(int which) {
        // > 65536 is too large to encode in the bytecode
        // so this will force us to emit a constant pool entry for this int
        return (long)which + 65537;
    }

    private String getBigString(int which) {
        return bigstr + which;
    }

    private String getClassName(int count) {
        return nameBase + count;
    }

    private String generateSource(int count, int sizeFactor, int numClasses) {
        StringBuilder sb = new StringBuilder();
        sb.append("public class ").append(getClassName(count)).append("{\n");
        for (int j = 0; j < numClasses; ++j) {
            sb.append("public static class ")
              .append("Class")
              .append(j)
              .append("{\n");
            for (int i = 0; i < sizeFactor; ++i) {
                int value = i;
                sb.append("private long field")
                  .append(i).append(" = ")
                  .append(getBigValue(value++))
                  .append(";\n");
                sb.append("public long method")
                  .append(i)
                  .append("() {\n");
                sb.append("return ")
                  .append(getBigValue(value++))
                  .append(";");
                sb.append("}\n");
                sb.append("private String str").append(i)
                  .append(" = \"")
                  .append(getBigString(i))
                  .append("\";");
            }
            sb.append("\n}");
        }
        sb.append("\n}");
        return sb.toString();
    }

    private GeneratedClass[] getGeneratedClass(int sizeFactor, int numClasses) throws IOException {
        int uniqueCount = getNextCount();
        String src = generateSource(uniqueCount, sizeFactor, numClasses);
        String className = getClassName(uniqueCount);
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
        GeneratedClass[] gc = new GeneratedClass[numClasses];
        for (int i = 0; i < numClasses; ++i) {
            String name = className + "$" + "Class" + i;
            File classFile = new File(name + ".class");
            byte[] bytes;
            try (DataInputStream dis = new DataInputStream(new FileInputStream(classFile))) {
                bytes = new byte[dis.available()];
                dis.readFully(bytes);
            }
            if (deleteFiles) {
                classFile.delete();
            }
            gc[i] = new GeneratedClass(bytes, name);
        }
        if (deleteFiles) {
            new File(className + ".class").delete();
        }
        return gc;
    }

    /**
     * Generate a single class, compile it and load it.
     * @param sizeFactor Fuzzy measure of how large the class should be.
     * @return the Class instance.
     * @throws IOException
     */
    public Class<?> generateClass(int sizeFactor) throws IOException {
        return getGeneratedClasses(sizeFactor, 1)[0];
    }

    /**
     * Generate several classes, compile and load them.
     * @param sizeFactor Fuzzy measure of how large each class should be.
     * @param numClasses The number of classes to create
     * @return an array of the Class instances.
     * @throws IOException
     */
    public Class<?>[] getGeneratedClasses(int sizeFactor, int numClasses) throws IOException {
        GeneratedClass[] gc = getGeneratedClass(sizeFactor, numClasses);
        Class<?>[] classes = new Class[numClasses];
        for (int i = 0; i < numClasses; ++i) {
            classes[i] = defineClass(gc[i].name, gc[i].bytes, 0 , gc[i].bytes.length);
        }
        return classes;
    }
}
