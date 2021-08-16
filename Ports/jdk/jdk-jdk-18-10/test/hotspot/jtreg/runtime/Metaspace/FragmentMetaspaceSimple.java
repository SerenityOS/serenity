/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

// ClassFileInstaller is needed to place test.Empty into well-known place
/**
 * @test
 * @library /test/lib classes
 * @build test.Empty
 * @run driver jdk.test.lib.helpers.ClassFileInstaller test.Empty
 * @run main/othervm/timeout=200 FragmentMetaspaceSimple
 */

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Test that tries to fragment the native memory used by class loaders.
 * Keeps every other class loader alive in order to fragment the memory space
 * used to store classes and meta data. Since the memory is probably allocated in
 * chunks per class loader this will cause a lot of fragmentation if not handled
 * properly since every other chunk will be unused.
 */
public class FragmentMetaspaceSimple {
    public static void main(String... args) {
        runSimple(Long.valueOf(System.getProperty("time", "80000")));
        System.gc();
    }

    private static void runSimple(long time) {
        long startTime = System.currentTimeMillis();
        ArrayList<ClassLoader> cls = new ArrayList<>();
        char sep = File.separatorChar;
        String fileName = "test" + sep + "Empty.class";
        File file = new File(fileName);
        byte buff[] = read(file);

        int i = 0;
        for (i = 0; System.currentTimeMillis() < startTime + time; ++i) {
            ClassLoader ldr = new MyClassLoader(buff);
            if (i % 1000 == 0) {
                cls.clear();
            }
            // only keep every other class loader alive
            if (i % 2 == 1) {
                cls.add(ldr);
            }
            Class<?> c = null;
            try {
                c = ldr.loadClass("test.Empty");
                c.getClass().getClassLoader(); // make sure we have a valid class.
            } catch (ClassNotFoundException ex) {
                System.out.println("i=" + i + ", len" + buff.length);
                throw new RuntimeException(ex);
            }
            c = null;
        }
        cls = null;
        System.out.println("Finished " + i + " iterations in " +
                           (System.currentTimeMillis() - startTime) + " ms");
    }

    private static byte[] read(File file) {
        byte buff[] = new byte[(int)(file.length())];
        try {
            DataInputStream din = new DataInputStream(new FileInputStream(file));
            din.readFully(buff);
            din.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return buff;
    }

    static class MyClassLoader extends ClassLoader {
        byte buff[];
        MyClassLoader(byte buff[]) {
            this.buff = buff;
        }

        public Class<?> loadClass() throws ClassNotFoundException {
            String name = "test.Empty";
            try {
                return defineClass(name, buff, 0, buff.length);
            } catch (Throwable e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}
