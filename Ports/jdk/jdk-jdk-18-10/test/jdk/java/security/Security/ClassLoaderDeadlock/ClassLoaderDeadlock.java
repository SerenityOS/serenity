/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

// See bug 5094825 / ClassLoadDeadlock.sh

import java.net.*;

import java.security.*;

public class ClassLoaderDeadlock {

    public static void main(String[] args) throws Exception {
        // create a new classloader
        URL url = new URL("file:provider/");
        final DelayClassLoader cl = new DelayClassLoader(url);

        // install a provider on the custom class loader as the most prefered provider
        Class clazz = cl.loadClass("HashProvider");
        Provider p = (Provider)clazz.newInstance();
        Security.insertProviderAt(p, 1);

        // add a delay in class loading for reproducability
        cl.delay = 1000;

        new Thread() {
            public void run() {
                try {
                    // load a random system class
                    // this locks the DelayClassLoader and after the delay the system class loader
                    Class c1 = cl.loadClass("java.lang.String");
                    System.out.println(c1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        // make sure the other thread got a chance to run and grab the lock
        Thread.sleep(200);

        // load a random class from a signed JAR file to trigger JAR signature verification
        // locks system class loader first and then tries to lock DelayClassLoader to load the hashes
        // DEADLOCK HAPPENS HERE
        Class c2 = Class.forName("com.abc.Tst1");

        // NOT REACHED
        System.out.println(c2);

        System.out.println("OK");
    }

    static class DelayClassLoader extends URLClassLoader {

        volatile int delay;

        DelayClassLoader(URL url) {
            super(new URL[] {url});
        }

        protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
            System.out.println("-loadClass(" + name + "," + resolve + ")");
            try {
                // delay so that we hold the lock for a longer period
                // makes it much easier to reproduce
                Thread.sleep(delay);
            } catch (InterruptedException e) { e.printStackTrace(); }
            return super.loadClass(name, resolve);
        }
    }

}
