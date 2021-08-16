/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4929170
 * @summary Tests that user-supplied IIOMetadata implementations
 *           is able to load correspnding IIOMetadataFormat implementations.
 */

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;


public class MetadataFormatThreadTest implements Runnable {

    String test_class;

    public static void main(String[] args) throws Exception {
        String codebase = args[0];
        String code = args[1];

        Thread t = createTest(codebase, code);
        try {
            t.start();
        } catch (IllegalStateException e) {
            System.out.println("Test failed.");
            e.printStackTrace();

            System.exit(1);
        }
    }

    public MetadataFormatThreadTest(String c) {
        test_class = c;
    }

    public void run() {
        try {
            ClassLoader loader = (ClassLoader)
                java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction() {
                            public Object run() {
                                return Thread.currentThread().getContextClassLoader();
                            }
                        });

            Class ct = loader.loadClass(test_class);

            MetadataTest t = (MetadataTest)ct.newInstance();

            t.doTest();
        } catch (Exception e) {
            System.out.println("Test failed.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    protected static Thread createTest(String codebase,
                                             String code) throws Exception {

        URL[] urls = { new File(codebase).toURL()};
        final ClassLoader loader = new URLClassLoader(urls);

        final Thread t = new Thread(new MetadataFormatThreadTest(code));
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction() {
                    public Object run() {
                        t.setContextClassLoader(loader);
                        return null;
                    }
                });

        return t;
    }

}
