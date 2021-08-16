/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4894899 7054428
 * @summary Test various cases of passing java.nio.ByteBuffers
 * to defineClass().
 *
 * @build DefineClassByteBuffer TestClass
 * @run main DefineClassByteBuffer
 */

import java.security.*;
import java.nio.*;
import java.nio.channels.*;
import java.io.*;

public class DefineClassByteBuffer {

    static void test(ClassLoader cl) throws Exception {
        Class c = Class.forName("TestClass", true, cl);
        if (!"TestClass".equals(c.getName())) {
            throw new RuntimeException("Got wrong class: " + c);
        }
    }

    public static void main(String arg[]) throws Exception {

        // Rename the compiled TestClass.class file to something else,
        // otherwise it would be loaded by the parent class loader and
        // DummyClassLoader will never be used, especially in /othervm mode.

        File oldFile = new File(System.getProperty("test.classes", "."),
                                  "TestClass.class");
        File newFile = new File(System.getProperty("test.classes", "."),
                                  "CLAZZ");
        oldFile.renameTo(newFile);

        ClassLoader[] cls = new ClassLoader[DummyClassLoader.MAX_TYPE];
        for (int i = 0; i < cls.length; i++) {
            cls[i] = new DummyClassLoader(i);
        }

        /* Create several instances of the class using different classloaders,
           which are using different types of ByteBuffer. */
        for (int i = 0; i < cls.length; i++) {
            test(cls[i]);
        }

        if (DummyClassLoader.count != cls.length) {
             throw new Exception("DummyClassLoader not always used");
        }
    }

    /** Always loads the same class, using various types of ByteBuffers */
    public static class DummyClassLoader extends SecureClassLoader {

        public static final String CLASS_NAME = "TestClass";

        public static final int MAPPED_BUFFER = 0;
        public static final int DIRECT_BUFFER = 1;
        public static final int ARRAY_BUFFER = 2;
        public static final int WRAPPED_BUFFER = 3;
        public static final int READ_ONLY_ARRAY_BUFFER = 4;
        public static final int READ_ONLY_DIRECT_BUFFER = 5;
        public static final int DUP_ARRAY_BUFFER = 6;
        public static final int DUP_DIRECT_BUFFER = 7;
        public static final int MAX_TYPE = 7;

        int loaderType;

        static int count = 0;

        DummyClassLoader(int loaderType) {
            this.loaderType = loaderType;
        }

        static ByteBuffer[] buffers = new ByteBuffer[MAX_TYPE + 1];

        static ByteBuffer readClassFile(String name) {
            try {
                File f = new File(System.getProperty("test.classes", "."),
                                  "CLAZZ");
                try (FileInputStream fin = new FileInputStream(f);
                        FileChannel fc = fin.getChannel()) {
                    return fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Can't open file: " + name, e);
            } catch (IOException e) {
                throw new RuntimeException("Can't open file: " + name, e);
            }
        }

        static {
            /* create a bunch of different ByteBuffers, starting with a mapped
               buffer from a class file, and create various duplicate and wrapped
               buffers. */
            buffers[MAPPED_BUFFER] = readClassFile(CLASS_NAME + ".class");
            byte[] array = new byte[buffers[MAPPED_BUFFER].limit()];
            buffers[MAPPED_BUFFER].get(array);
            buffers[MAPPED_BUFFER].flip();

            buffers[DIRECT_BUFFER] = ByteBuffer.allocateDirect(array.length);
            buffers[DIRECT_BUFFER].put(array);
            buffers[DIRECT_BUFFER].flip();

            buffers[ARRAY_BUFFER] = ByteBuffer.allocate(array.length);
            buffers[ARRAY_BUFFER].put(array);
            buffers[ARRAY_BUFFER].flip();

            buffers[WRAPPED_BUFFER] = ByteBuffer.wrap(array);

            buffers[READ_ONLY_ARRAY_BUFFER] = buffers[ARRAY_BUFFER].asReadOnlyBuffer();

            buffers[READ_ONLY_DIRECT_BUFFER] = buffers[DIRECT_BUFFER].asReadOnlyBuffer();

            buffers[DUP_ARRAY_BUFFER] = buffers[ARRAY_BUFFER].duplicate();

            buffers[DUP_DIRECT_BUFFER] = buffers[DIRECT_BUFFER].duplicate();
        }

         public Class findClass(String name) {
             CodeSource cs = null;
             count++;
             return defineClass(name, buffers[loaderType], cs);
         }
    } /* DummyClassLoader */

} /* DefineClassByteBuffer */
