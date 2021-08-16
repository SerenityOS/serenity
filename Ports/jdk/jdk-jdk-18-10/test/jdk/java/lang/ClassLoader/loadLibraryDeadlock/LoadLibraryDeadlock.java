/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, BELLSOFT. All rights reserved.
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
 * LoadLibraryDeadlock class triggers the deadlock between the two
 * lock objects - ZipFile object and ClassLoader.loadedLibraryNames hashmap.
 * Thread #2 loads a signed jar which leads to acquiring the lock objects in
 * natural order (ZipFile then HashMap) - loading a signed jar may involve
 * Providers initialization. Providers may load native libraries.
 * Thread #1 acquires the locks in reverse order, first entering loadLibrary
 * called from Class1, then acquiring ZipFile during the search for a class
 * triggered from JNI.
 */
import java.lang.*;

public class LoadLibraryDeadlock {

    public static void main(String[] args) {
        Thread t1 = new Thread() {
            public void run() {
                try {
                    // an instance of unsigned class that loads a native library
                    Class<?> c1 = Class.forName("Class1");
                    Object o = c1.newInstance();
                } catch (ClassNotFoundException |
                         InstantiationException |
                         IllegalAccessException e) {
                    System.out.println("Class Class1 not found.");
                    throw new RuntimeException(e);
                }
            }
        };
        Thread t2 = new Thread() {
            public void run() {
                try {
                    // load a class from a signed jar, which locks the JarFile
                    Class<?> c2 = Class.forName("p.Class2");
                    System.out.println("Signed jar loaded.");
                } catch (ClassNotFoundException e) {
                    System.out.println("Class Class2 not found.");
                    throw new RuntimeException(e);
                }
            }
        };
        t2.start();
        t1.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException ignore) {
        }
    }
}
