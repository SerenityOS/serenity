/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test ArraysNewInstanceBug
 * @bug 8182397
 * @summary race in setting array_klass field for component mirror with mirror update for klass
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -Xcomp ArraysNewInstanceBug
 */

// This test crashes in compiled code with race, because the compiler generates code that assumes this ordering.
import java.lang.reflect.Array;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

public class ArraysNewInstanceBug implements Runnable {
    static Class<?>[] classes;

    int start;

    ArraysNewInstanceBug(int start) {
        this.start = start;
    }

    String[] result;

    public void run() {
        result = new String[classes.length];
        System.err.print('.');
        for (int i = start; i < classes.length; i++) {
            result[i] = Array.newInstance(classes[i], 0).getClass().getName();
        }
    }

    public static void main(String[] args) throws Throwable {
        Class<?> c = ArraysNewInstanceBug.class;
        ClassLoader apploader = c.getClassLoader();
        File testClasses = new File(System.getProperty("test.classes"));
        for (int iter = 0; iter < 10 ; iter++) {  // 10 is enough to get it to crash on my machine.
            System.err.print('[');
            classes = new Class<?>[1000];
            for (int i = 0; i < classes.length; i++) {
                ClassLoader loader = new URLClassLoader(new URL[] { testClasses.toURI().toURL() }, apploader.getParent());
                classes[i] = loader.loadClass(c.getSimpleName());
            }
            System.err.print(']');
            System.err.print('(');
            int threadCount = 64;
            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(new ArraysNewInstanceBug(i));
            }
            for (int i = 0; i < threads.length; i++) {
                threads[i].start();
            }
            for (int i = 0; i < threads.length; i++) {
                threads[i].join();
            }
            System.err.print(')');
        }
    }
}
