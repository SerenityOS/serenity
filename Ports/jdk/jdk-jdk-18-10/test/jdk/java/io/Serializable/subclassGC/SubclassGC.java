/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 6232010
 * @summary this test checks that replacing SoftCache class with ConcurrentMap
 *          in ObjectInputStream/ObjectOutputStream gives an opportunity to
 *          classes which are inherited from OIS and OOS and loaded through
 *          separete ClassLoaders be available for garbage collection
 *
 * @author Andrey Ozerov
 * @run main/othervm/policy=security.policy SubclassGC
 */

import java.io.*;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;

public class SubclassGC {
        private static final long TIMEOUT = 1000;

        public static final void main(String[] args) throws Exception {
                System.err.println("\n Regression test for bug 6232010\n");
                if (System.getSecurityManager() == null) {
                        System.setSecurityManager(new SecurityManager());
                }

                ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
                URL testClassesURL = new File(System.getProperty("test.classes")).toURI().toURL();
                ClassLoader loader = new URLClassLoader(new URL[] { testClassesURL } ,
                                                        systemLoader.getParent());
                Class<? extends ObjectOutputStream> cl =
                        Class.forName(SubclassOfOOS.class.getName(), false,
                                                  loader).asSubclass(ObjectOutputStream.class);

                Constructor<? extends ObjectOutputStream> cons =
                        cl.getConstructor(OutputStream.class);

                OutputStream os = new ByteArrayOutputStream();
                ObjectOutputStream obj = cons.newInstance(os);

                final ReferenceQueue<Class<?>> queue = new ReferenceQueue<Class<?>>();
                WeakReference<Class<?>> ref = new WeakReference<Class<?>>(cl, queue);

                cl = null;
                obj = null;
                loader = null;
                cons = null;
                systemLoader = null;

                System.err.println("\nStart Garbage Collection right now");
                System.gc();

                Reference<? extends Class<?>> dequeued = queue.remove(TIMEOUT);
                if (dequeued == ref) {
                        System.err.println("\nTEST PASSED");
                } else {
                        throw new Error();
                }
        }
}
