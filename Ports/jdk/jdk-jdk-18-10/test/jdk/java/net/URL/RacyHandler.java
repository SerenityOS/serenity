/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.concurrent.CountDownLatch;

/*
 * @test
 * @bug 8213942
 * @summary URLStreamHandler initialization race
 * @modules java.base/java.net:open
 * @run main/othervm RacyHandler
 * @run main/othervm RacyHandler
 * @run main/othervm RacyHandler
 */

/*
 * This test makes reasonable effort to reproduce the race.
 * Run repeatedly to ensure correctness.
 */
public class RacyHandler {
    static volatile boolean factorySet = false;
    static int NUM_THREADS = 2;
    static CountDownLatch cdl = new CountDownLatch(NUM_THREADS + 1);

    public static void main(String[] args) {
        RacyHandler tester = new RacyHandler();
        tester.runTest();
    }

    public void runTest() {
        new Thread(() -> {
            try {
                cdl.await();
                URL.setURLStreamHandlerFactory(proto -> new CustomHttpHandler());
                factorySet = true;
            } catch (Exception ignore) { }
        }).start();
        cdl.countDown();

        for (int i = 0; i < NUM_THREADS; i++) {
            new Thread(() -> {
                try {
                    cdl.await();
                    while (!factorySet) {
                        // trigger URL class load
                        getURLStreamHandler();
                    }
                } catch (Exception ignore) { }
            }).start();
            cdl.countDown();
        }

        // wait for the factory to be set
        while (!factorySet) { }
        // The sleep seems to help trigger the failure
        try {
            Thread.sleep(500);
        } catch (InterruptedException ie) {
        }

        URLStreamHandler httpHandler = getURLStreamHandler();
        System.out.println("After setting factory URL handlers: http " + httpHandler);
        if (!(httpHandler instanceof CustomHttpHandler))
            throw new RuntimeException("FAILED: Incorrect handler type");
    }

    /*
     * This is just so we can see what we get for the URLStreamHandler back
     * from the factory to verify whether it really is using our Handler
     * or something else...
     */
    public URLStreamHandler getURLStreamHandler() {
        try {
            Method method = URL.class.getDeclaredMethod("getURLStreamHandler",
                    String.class);
            method.setAccessible(true);
            return (URLStreamHandler) method.invoke(null, "http");
        } catch (Exception e) {
            return null;
        }
    }

    class CustomHttpHandler extends URLStreamHandler {
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return null;
        }
    }
}
