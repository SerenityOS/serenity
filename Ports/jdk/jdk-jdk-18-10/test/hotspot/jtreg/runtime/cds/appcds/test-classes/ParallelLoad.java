/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.io.*;
import java.net.*;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;


// This test helper is parameterized by:
// - class transformation mode: property "appcds.parallel.transform.mode"
// - class loader test types
//
// In the case of transformMode == "cflh", the transformation is performed
// by AppCDS/jvmti/TransformerAgent.java. The classes to be transformed, such as
// ParallelClassTr0, are defined in ./jvmti/parallelLoad/ParallelClasses.java

public class ParallelLoad {
    public static int MAX_CLASSES = 40;
    public static int NUM_THREADS = 4;

    public final static int SYSTEM_LOADER = 0;
    public final static int SINGLE_CUSTOM_LOADER = 1;
    public final static int MULTI_CUSTOM_LOADER = 2;

    public static final int FINGERPRINT_MODE = 1;
    public static final int API_MODE         = 2;

    public static int loaderType = SYSTEM_LOADER;
    public static ClassLoader classLoaders[];
    public static int mode = FINGERPRINT_MODE;

    public static float timeoutFactor =
        Float.parseFloat(System.getProperty("test.timeout.factor", "1.0"));

    public static void main(String args[]) throws Throwable {
        run(args, null);
    }
    public static void run(String args[], ClassLoader loaders[]) throws Throwable {
        String customJar = null;
        System.out.println("ParallelLoad: timeoutFactor = " + timeoutFactor);

        if (args.length >= 1) {
            if ("SINGLE_CUSTOM_LOADER".equals(args[0])) {
                loaderType = SINGLE_CUSTOM_LOADER;
                customJar = args[2];
            } else if ("MULTI_CUSTOM_LOADER".equals(args[0])) {
                loaderType = MULTI_CUSTOM_LOADER;
                customJar = args[2];
            } else if ("SYSTEM_LOADER".equals(args[0])) {
                loaderType = SYSTEM_LOADER;
            } else {
                throw new RuntimeException("Unexpected loaderType" + args[0]);
            }
        }

        if (customJar != null) {
            if ("FINGERPRINT_MODE".equals(args[1])) {
                mode = FINGERPRINT_MODE;
                classLoaders = new ClassLoader[NUM_THREADS];
                for (int i = 0; i < NUM_THREADS; i++) {
                    URL url = new File(customJar).toURI().toURL();
                    URL[] urls = new URL[] {url};
                    classLoaders[i] = new URLClassLoader(urls);
                }
            } else {
                // Loaders must be supplied by caller of the run() method
                mode = API_MODE;
                classLoaders = loaders;
            }
        }

        System.out.println("Start Parallel Load ...");

        Thread thread[] = new Thread[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            Thread t = new ParallelLoadThread(i);
            t.start();
            thread[i] = t;
        }

        Thread watchdog = new ParallelLoadWatchdog();
        watchdog.setDaemon(true);
        watchdog.start();

        for (int i = 0; i < NUM_THREADS; i++) {
            thread[i].join();
        }
        System.out.println("Parallel Load ... done");
        System.exit(0);
    }
}


class ParallelLoadWatchdog extends Thread {
    public void run() {
        try {
            long timeout = (long) (20 * 1000 * ParallelLoad.timeoutFactor);
            Thread.sleep(timeout);
            System.out.println("ParallelLoadWatchdog: Timeout reached: timeout(ms) = " + timeout);
            System.exit(1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }
};


class ParallelLoadThread extends Thread {
    static AtomicInteger num_ready[];
    static {
        num_ready = new AtomicInteger[ParallelLoad.MAX_CLASSES];
        for (int i = 0; i < ParallelLoad.MAX_CLASSES; i++) {
            num_ready[i] = new AtomicInteger();
        }
    }
    static String transformMode =
        System.getProperty("appcds.parallel.transform.mode", "none");

    int thread_id;
    ParallelLoadThread(int thread_id) {
        this.thread_id = thread_id;
    }

    public void run() {
        try {
            run0();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void log(String msg, Object... args) {
        String msg0 = "ParallelLoadThread: " + String.format(msg, args);
        System.out.println(msg0);
    }

    private void run0() throws Throwable {
        for (int i = 0;  i < ParallelLoad.MAX_CLASSES; i++) {
            String className = "ParallelClass" + i;
            if (transformMode.equals("cflh")) {
                className = "ParallelClassTr" + i;
            }
            Class clazz = null;

            // Spin until every thread is ready to proceed
            num_ready[i].incrementAndGet();
            while (num_ready[i].intValue() < ParallelLoad.NUM_THREADS) {
                ;
            }

            {   // Avoid logging in this block so the threads can proceed without
                // waiting for the stdout lock, etc.
                switch (ParallelLoad.loaderType) {
                case ParallelLoad.SYSTEM_LOADER:
                    clazz = Class.forName(className);
                    break;
                case ParallelLoad.SINGLE_CUSTOM_LOADER:
                    clazz = ParallelLoad.classLoaders[0].loadClass(className);
                    break;
                case ParallelLoad.MULTI_CUSTOM_LOADER:
                    clazz = ParallelLoad.classLoaders[thread_id].loadClass(className);
                    break;
                }
                testTransformation(clazz);
            }

            log("thread[%d] t = %s, c = %s, l = %s", thread_id, this, clazz, clazz.getClassLoader());
        }
    }

    private void testTransformation(Class c) throws Exception {
        if (transformMode.equals("none"))
            return;

        // currently only cflh transform mode is supported
        if (!transformMode.equals("cflh")) {
            String msg = "wrong transform mode: " + transformMode;
            throw new IllegalArgumentException(msg);
        }

        Field[] fields = c.getFields();
        boolean fieldFound = false;
        for (Field f : fields) {
            if (f.getName().equals("testString")) {
                checkTransformationString(c, (String) f.get(null));
                fieldFound = true;
            }
        }

        if (!fieldFound)
            throw new RuntimeException ("Expected field 'testString' not found");
    }

    private void checkTransformationString(Class c, String actual) throws Exception {
        String expected = "class-transform-check: this-has-been--transformed";
        if (!actual.equals(expected)) {
            String msg1 = "Transformation failed for class" + c.getName();
            String msg2 = String.format("Expected: %s, actual: %s", expected, actual);
            throw new RuntimeException(msg1 + "\n" + msg2);
        }
    }
}

