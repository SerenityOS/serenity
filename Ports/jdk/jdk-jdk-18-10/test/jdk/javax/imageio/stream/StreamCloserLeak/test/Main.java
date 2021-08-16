/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

package test;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import javax.imageio.stream.ImageInputStream;
import sun.awt.AppContext;
import sun.awt.SunToolkit;

public class Main {

    private static ThreadGroup appsThreadGroup;

    private static WeakHashMap<MyClassLoader, String> refs =
            new WeakHashMap<MyClassLoader, String>();

    /** Collection to simulate forgrotten streams **/
    private static HashMap<String, ImageInputStream> strongRefs =
            new HashMap<String, ImageInputStream>();

    private static ConcurrentLinkedQueue<Throwable> problems =
            new ConcurrentLinkedQueue<Throwable>();

    private static AppContext mainAppContext = null;

    private static CountDownLatch doneSignal;

    private static final int gcTimeout =
        Integer.getInteger("gcTimeout", 10).intValue();

    private static boolean forgetSomeStreams =
            Boolean.getBoolean("forgetSomeStreams");

    public static void main(String[] args) throws IOException {
        mainAppContext = SunToolkit.createNewAppContext();
        System.out.println("Current context class loader: " +
                Thread.currentThread().getContextClassLoader());

        appsThreadGroup = new ThreadGroup("MyAppsThreadGroup");

        File jar = new File("TestApp.jar");
        if (!jar.exists()) {
            System.out.println(jar.getAbsolutePath() + " was not found!\n" +
                    "Please install the jar with test application correctly!");
            throw new RuntimeException("Test failed: no TestApp.jar");
        }

        URL[] urls = new URL[]{jar.toURL()};

        int numApps = Integer.getInteger("numApps", 20).intValue();

        doneSignal = new CountDownLatch(numApps);
        int cnt = 0;
        while (cnt++ < numApps) {
            launch(urls, "testapp.Main", "launch");

            checkErrors();
        }

        System.out.println("Wait for apps completion....");

        try {
            doneSignal.await();
        } catch (InterruptedException e) {
        }

        System.out.println("All apps finished.");

        System.gc();

        System.out.flush();

        System.out.println("Enumerate strong refs:");
        for (String is : strongRefs.keySet()) {
            System.out.println("-> " + is);
        }

        System.out.println("=======================");

        // wait few seconds
        waitAndGC(gcTimeout);

        doneSignal = new CountDownLatch(1);

        Runnable workaround = new Runnable() {

            public void run() {
                AppContext ctx = null;
                try {
                    ctx = SunToolkit.createNewAppContext();
                } catch (Throwable e) {
                    // ignore...
                } finally {
                    doneSignal.countDown();
                }
            }
        };

        Thread wt = new Thread(appsThreadGroup, workaround, "Workaround");
        wt.setContextClassLoader(new MyClassLoader(urls, "workaround"));
        wt.start();
        wt = null;
        workaround = null;

        System.out.println("Wait for workaround completion...");

        try {
            doneSignal.await();
        } catch (InterruptedException e) {
        }

        // give a chance to GC
        waitAndGC(gcTimeout);

        if (!refs.isEmpty()) {
            System.out.println("Classloaders still alive:");

            for (MyClassLoader l : refs.keySet()) {
                String val = refs.get(l);

                if (val == null) {
                    throw new RuntimeException("Test FAILED: Invalid classloader name");
                }
                System.out.println("->" + val + (strongRefs.get(val) != null ?
                                    " (has strong ref)" : ""));
                if (strongRefs.get(val) == null) {
                    throw new RuntimeException("Test FAILED: exta class loader is detected! ");
                }
            }
        } else {
            System.out.println("No alive class loaders!!");
        }
        System.out.println("Test PASSED.");
    }

    private static void waitAndGC(int sec) {
        int cnt = sec;
        System.out.print("Wait ");
        while (cnt-- > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            // do GC every 3 seconds
            if (cnt % 3 == 2) {
                System.gc();
                System.out.print("+");
            } else {
                System.out.print(".");
            }
            checkErrors();
        }
        System.out.println("");
    }

    private static void checkErrors() {
        while (!problems.isEmpty()) {
            Throwable theProblem = problems.poll();
            System.out.println("Test FAILED!");
            do {
                theProblem.printStackTrace(System.out);
                theProblem = theProblem.getCause();
            } while (theProblem != null);
            throw new RuntimeException("Test FAILED");
        }
    }
    static int counter = 0;

    private static void launch(URL[] urls, final String className,
                               final String methodName)
    {
        final String uniqClassName = "testapp/Uniq" + counter;
        final boolean saveStrongRef = forgetSomeStreams ? (counter % 5 == 4) : false;

        System.out.printf("%s: launch the app\n", uniqClassName);
        Runnable launchIt = new Runnable() {
            public void run() {
                AppContext ctx = SunToolkit.createNewAppContext();

                try {
                    Class appMain =
                        ctx.getContextClassLoader().loadClass(className);
                    Method launch = appMain.getDeclaredMethod(methodName,
                                strongRefs.getClass());

                    Constructor c = appMain.getConstructor(String.class,
                                                           problems.getClass());

                    Object o = c.newInstance(uniqClassName, problems);

                    if (saveStrongRef) {
                        System.out.printf("%s: force strong ref\n",
                                          uniqClassName);
                        launch.invoke(o, strongRefs);
                    } else {
                        HashMap<String, ImageInputStream> empty = null;
                        launch.invoke(o, empty);
                    }

                    ctx = null;
                } catch (Throwable e) {
                    problems.add(e);
                } finally {
                    doneSignal.countDown();
                }
            }
        };

        MyClassLoader appClassLoader = new MyClassLoader(urls, uniqClassName);

        refs.put(appClassLoader, uniqClassName);

        Thread appThread = new Thread(appsThreadGroup, launchIt,
                                      "AppThread" + counter++);
        appThread.setContextClassLoader(appClassLoader);

        appThread.start();
        launchIt = null;
        appThread = null;
        appClassLoader = null;
    }

    private static class MyClassLoader extends URLClassLoader {

        private static boolean verbose =
            Boolean.getBoolean("verboseClassLoading");
        private String uniqClassName;

        public MyClassLoader(URL[] urls, String uniq) {
            super(urls);

            uniqClassName = uniq;
        }

        public Class loadClass(String name) throws ClassNotFoundException {
            if (verbose) {
                System.out.printf("%s: load class %s\n", uniqClassName, name);
            }
            if (uniqClassName.equals(name)) {
                return Object.class;
            }
            return super.loadClass(name);
        }

        public String toString() {
            return "MyClassLoader(" + uniqClassName + ")";
        }
    }
}
