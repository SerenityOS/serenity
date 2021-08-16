/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BooleanSupplier;

import static java.net.http.HttpClient.Builder.NO_PROXY;
import static java.net.http.HttpClient.newBuilder;

/* Common infrastructure for tests that check pending operations */
public class PendingOperations {

    static final Class<IllegalStateException> ISE = IllegalStateException.class;
    static final Class<IOException> IOE = IOException.class;
    // Time after which we deem that the local send buffer and remote
    // receive buffer must be full. This has been heuristically determined.
    // At the time of writing, using anything <= 5s on Mac will make the
    // tests fail intermittently.
    static final long MAX_WAIT_SEC = 10; // seconds.

    DummyWebSocketServer server;
    WebSocket webSocket;

    protected HttpClient httpClient() {
        return newBuilder().proxy(NO_PROXY).build();
    }

    @AfterMethod
    public void cleanup() {
        // make sure we have a trace both on System.out and System.err
        // to help with diagnosis.
        System.out.println("cleanup: Closing server");
        System.err.println("cleanup: Closing server");
        server.close();
        webSocket.abort();
    }

    /* shortcut */
    static void assertHangs(CompletionStage<?> stage) {
        Support.assertHangs(stage);
    }

    /* shortcut */
    static void assertFails(Class<? extends Throwable> clazz,
                            CompletionStage<?> stage) {
        Support.assertCompletesExceptionally(clazz, stage);
    }

    static void assertNotDone(CompletableFuture<?> future) {
        Support.assertNotDone(future);
    }

    @DataProvider(name = "booleans")
    public Object[][] booleans() {
        return new Object[][]{{Boolean.TRUE}, {Boolean.FALSE}};
    }

    static boolean isMacOS() {
        return System.getProperty("os.name").contains("OS X");
    }
    static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("win");
    }

    private static final int ITERATIONS = 3;

    void repeatable(Callable<Void> callable,
                    BooleanSupplier repeatCondition)
        throws Exception
    {
        int iterations = 0;
        do {
            iterations++;
            System.out.println("--- iteration " + iterations + " ---");
            try {
                callable.call();
                break;
            } catch (AssertionError e) {
                var isMac = isMacOS();
                var isWindows = isWindows();
                var repeat = repeatCondition.getAsBoolean();
                System.out.printf("repeatable: isMac=%s, isWindows=%s, repeat=%s, iterations=%d%n",
                                  isMac, isWindows, repeat, iterations);
                if ((isMac || isWindows) && repeat) {
                    // ## This is loathsome, but necessary because of observed
                    // ## automagic socket buffer resizing on recent macOS platforms
                    try { cleanup(); } catch (Throwable x) {}
                    continue;
                } else {
                    throw e;
                }
            } finally {
                // gives some time to gc to cleanup any resource that might
                // be eligible for garbage collection
                System.gc();
                Thread.sleep(100);
            }
        } while (iterations <= ITERATIONS);
    }
}
