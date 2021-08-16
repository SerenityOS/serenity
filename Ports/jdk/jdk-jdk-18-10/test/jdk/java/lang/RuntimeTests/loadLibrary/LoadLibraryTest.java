/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 * Copyright (c) 2019, Azul Systems, Inc. All rights reserved.
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

/**
 * @test
 * @bug 8231584
 * @library /test/lib
 * @run main/othervm LoadLibraryTest
 */

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.net.URL;

import jdk.test.lib.compiler.CompilerUtils;

public class LoadLibraryTest {
    static Thread thread1 = null;
    static Thread thread2 = null;

    static volatile boolean thread1Ready = false;

    private static final String TEST_SRC = System.getProperty("test.src");
    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path CLS_DIR = Paths.get("classes");

    static TestClassLoader loader;
    static void someLibLoad() {
        try {
/*
            FileSystems.getDefault();

            // jdk/jdk: loads directly from Bootstrap Classloader (doesn't take lock on Runtime)
            java.net.NetworkInterface.getNetworkInterfaces();

*/
            Class c = Class.forName("Target2", true, loader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static class TestClassLoader extends URLClassLoader {
        boolean passed = false;

        public boolean passed() {
            return passed;
        }

        TestClassLoader() throws MalformedURLException {
            super(new URL[] { new URL("file://" + CLS_DIR.toAbsolutePath().toString() + '/') });
        }

        public String findLibrary(String name) {
            System.out.println("findLibrary " + name);

            if ("someLibrary".equals(name)) {
                try {
                    synchronized(thread1) {
                        while(!thread1Ready) {
                            thread1.wait();
                        }
                        thread1.notifyAll();
                    }

                    Thread.sleep(10000);

                    System.out.println("Thread2 load");
                    someLibLoad();

                    // no deadlock happened
                    passed = true;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return null;
            }

            return super.findLibrary(name);
        }
    }


    public static void main(String[] args) throws Exception {
        loader = new TestClassLoader();

        if (!CompilerUtils.compile(SRC_DIR, CLS_DIR)) {
            throw new Exception("Can't compile");
        }

        thread1 = new Thread() {
            public void run() {
                try {
                    synchronized(this) {
                        thread1Ready = true;
                        thread1.notifyAll();
                        thread1.wait();
                    }
                } catch(InterruptedException e) {
                    throw new RuntimeException(e);
                }

                System.out.println("Thread1 load");
                someLibLoad();
            };
        };

        thread2 = new Thread() {
            public void run() {
                try {
                    Class c = Class.forName("Target", true, loader);
                    System.out.println(c);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        };

        thread1.setDaemon(true);
        thread2.setDaemon(true);

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        if (!loader.passed()) {
            throw new RuntimeException("FAIL");
        }
    }
}
