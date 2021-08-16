/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * @test
 * @bug 8239013
 * @summary This test verifies that the Logger cache does
 * not keep a strong reference on dynamically loaded resource
 * bundles
 * @build BundleTest MyBundle LoggingApp
 * @run main/othervm -Xmx64M -Djava.util.logging.config.file=logging.properties BundleTest
 */
public class BundleTest {

    // My handler is used to get at the published LogRecords
    public static class MyHandler extends Handler {
        final List<LogRecord> records = new CopyOnWriteArrayList<>();
        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }
        @Override
        public void flush() { }
        @Override
        public void close() throws SecurityException { }
    }

    public static void main(String[] args) throws Exception {
        // copy classes and resource files
        List<URL> classes = setUp();

        // create an URL class loader that contains a copy of
        // LoggingApp and MyBundle classes
        URLClassLoader loader = new URLClassLoader(classes.toArray(new URL[0]), null);
        Class<?> appClass = Class.forName(LoggingApp.class.getName(), true, loader);
        Class<?> bundleClass = Class.forName(MyBundle.class.getName(), true, loader);
        if (bundleClass.getClassLoader() != loader) {
            throw new AssertionError("bundleClass not loaded by URL loader");
        }

        // Invoke LoggingApp.logger() which will create a logger
        // that uses a "MyBundle" bundle, and then log a message
        // to force the logger to load up an instance of the
        // MyBundle class in its catalog cache.
        Logger logger;
        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        Reference<Object> loaderRef = new PhantomReference<>(loader, queue);
        MyHandler handler = new MyHandler();
        Thread currentThread = Thread.currentThread();
        ClassLoader context = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(loader);
        try {
            logger = (Logger) appClass.getMethod("logger").invoke(null);
            logger.addHandler(handler);
            logger.fine("foo");
            ResourceBundle rb = handler.records.get(0).getResourceBundle();
            // verify that the class of the resource bundle is the
            // class loaded earlier by 'loader'
            if (rb.getClass() != bundleClass) {
                throw new AssertionError("unexpected loader for: " + rb.getClass());
            }
            // At this point the logger has a reference to an instance
            // of the MyBundle class loaded by 'loader' in its catalog cache.
            // This is demonstrated by the presence of that bundle in the
            // LogRecord.
        } finally {
            currentThread.setContextClassLoader(context);
        }

        // cleanup all things that might reference 'loader'
        appClass = bundleClass = null;
        loader = null;
        handler.records.clear();

        // now try to trigger a full GC to force the cleanup
        // of soft caches. If the logger has a strong reference
        // to MyBundle, this will eventually cause an
        // OutOfMemoryError, and the test will fail.
        Reference<?> ref;
        System.gc();
        List<byte[]> memory = new ArrayList<>();
        boolean stop = false;
        System.out.println("Waiting for URL loader to be GC'ed");
        long timeout = 100;
        while ((ref = queue.remove(timeout)) == null) {
            if (stop) break;
            try {
                // eat memory to trigger cleaning of SoftReference
                memory.add(new byte[1024*1024]);
                System.out.printf("Total memory added: %s Mb%n", memory.size());
            } catch (OutOfMemoryError oome) {
                stop = true;
                memory = null;
                timeout = 1000; // give more time for the last GC
            }
            System.gc();
        }
        memory = null;
        if (stop) {
            System.out.println("no more memory...");
        }

        // Verify that loader was GC'ed
        if (ref != loaderRef) {
            throw new AssertionError("Loader was not GC'ed");
        }
        System.out.println("Loader was GC'ed");
        Reference.reachabilityFence(logger);
    }


    static String file(Class<?> type) {
        return type.getSimpleName() + ".class";
    }

    public static List<URL> setUp() throws IOException {
        String classes = System.getProperty("test.classes", "build");
        String cwd = System.getProperty("user.dir", ".");
        String sources = System.getProperty("test.src", "src");
        for (var type : List.of(LoggingApp.class, MyBundle.class)) {
            var from = Path.of(classes, file(type));
            var to = Path.of(cwd, file(type));
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.copy(Path.of(sources, "logging.properties"),
                   Path.of(cwd, "logging.properties"),
                   StandardCopyOption.REPLACE_EXISTING);
        return List.of(Path.of(cwd).toUri().toURL());
    }
}
