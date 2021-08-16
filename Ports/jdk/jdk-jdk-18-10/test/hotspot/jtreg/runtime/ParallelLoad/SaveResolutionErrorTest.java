/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8262046
 * @summary JVMS 5.4.3 If an attempt by the Java Virtual Machine to resolve a symbolic reference fails
 *          because an error is thrown that is an instance of LinkageError (or a subclass), then subsequent
 *          attempts to resolve the reference always fail with the same error that was thrown as a result of
 *          the initial resolution attempt.
 * @run main/othervm SaveResolutionErrorTest
 */

public class SaveResolutionErrorTest {
    static byte classfile_for_Tester[];
    static byte classfile_for_Loadee[];

    public static void main(java.lang.String[] args) throws Exception {
        ClassLoader appLoader = SaveResolutionErrorTest.class.getClassLoader();
        classfile_for_Tester = appLoader.getResourceAsStream("SaveResolutionErrorTest$Tester.class").readAllBytes();
        classfile_for_Loadee = appLoader.getResourceAsStream("SaveResolutionErrorTest$Loadee.class").readAllBytes();

        long started = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            System.out.println("Test: " + i);
            MyLoader loader = new MyLoader(appLoader);
            loader.doTest();

            if (System.currentTimeMillis() - started > 100000) {
                break;
            }
        }
        System.out.println("Succeed");
    }

    public static class Tester extends Thread {
        static int errorCount = 0;
        synchronized static void incrError() {
            ++ errorCount;
        }

        public volatile static boolean go = false;

        public static void doit() throws Exception {
            System.out.println(Tester.class.getClassLoader());

            Thread t1 = new Tester();
            Thread t2 = new Tester();

            t1.start();
            t2.start();

            go = true;

            t1.join();
            t2.join();


            System.out.println("errorCount = " + errorCount);

            if (errorCount != 0 && errorCount != 2) {
                throw new RuntimeException("errorCount should be 0 or 2 but is " + errorCount);
            }
        }

        static int foobar;
        static boolean debug = false;

        public void run() {
            while (!go) { Thread.onSpinWait(); }

            try {
                // The first thread who tries to resolve the CP entry for the "Loadee" class
                // should (most likely) result in a resolution error. This error, if it has
                // happened, must also be reported by the second thread.
                //
                // In some rare conditions, the resolution may succeed. In this case, both
                // threads must succeed. The reasons is:
                //
                //     The first thread who tries to resolve the CP entry gets a bad class, but
                //     when it tries to update the CP entry, the second thread has already succeeded in
                //     loading the class and has set the entry's tag to JVM_CONSTANT_Class.

                foobar += Loadee.value;
            } catch (Throwable t) {
                Throwable cause = t.getCause();
                if (cause != null) {
                    String s = cause.toString();
                    if (s.equals(t.toString())) { // wrong cause
                        t.printStackTrace();
                        throw new RuntimeException("wrong cause");
                    }
                }
                if (debug) {
                    System.out.println(t);
                } else {
                    synchronized (Tester.class) {
                        // Enable this block to see the stack trace
                        System.out.println("------");
                        t.printStackTrace();
                        System.out.println("");
                    }
                }
                incrError();
            }
        }
    }

    public static class Loadee {
        static int value = 1234;
    }

    static class MyLoader extends ClassLoader {
        static int count;
        static byte[] badclass = {1, 2, 3, 4, 5, 6, 7, 8};

        static {
            registerAsParallelCapable();
        }

        ClassLoader parent;

        MyLoader(ClassLoader parent) {
            this.parent = parent;
        }

        synchronized boolean hack() {
            ++ count;
            if ((count % 2) == 1) {
                return true;
            } else {
                return false;
            }
        }

        public Class loadClass(String name) throws ClassNotFoundException {
            if (name.equals("SaveResolutionErrorTest$Loadee")) {
                if (hack()) {
                    return defineClass(name, badclass, 0, badclass.length);
                } else {
                    return defineClass(name, classfile_for_Loadee, 0, classfile_for_Loadee.length);
                }
            }
            if (name.equals("SaveResolutionErrorTest$Tester")) {
                return defineClass(name, classfile_for_Tester, 0, classfile_for_Tester.length);
            }
            return parent.loadClass(name);
        }

        void doTest() throws Exception {
            Class c = Class.forName("SaveResolutionErrorTest$Tester", /*init=*/true, this);
            java.lang.reflect.Method m = c.getMethod("doit");
            m.invoke(null);
        }
    }
}
