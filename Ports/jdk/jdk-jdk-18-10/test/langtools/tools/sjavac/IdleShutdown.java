/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8044131
 * @summary Tests the hooks used for detecting idleness of the sjavac server.
 * @modules jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.sjavac.server
 * @build Wrapper
 * @run main Wrapper IdleShutdown
 */

import java.util.concurrent.atomic.AtomicLong;

import com.sun.tools.javac.main.Main.Result;
import com.sun.tools.sjavac.server.IdleResetSjavac;
import com.sun.tools.sjavac.server.Sjavac;
import com.sun.tools.sjavac.server.Terminable;


public class IdleShutdown {

    final static long TEST_START = System.currentTimeMillis();
    final static long TIMEOUT_MS = 3000;

    public static void main(String[] args) throws InterruptedException {

        final AtomicLong timeoutTimestamp = new AtomicLong(-1);

        log("Starting IdleCallbackJavacService with timeout: " + TIMEOUT_MS);
        Sjavac service = new IdleResetSjavac(
                new NoopJavacService(),
                new Terminable() {
                    public void shutdown(String msg) {
                        // Record the idle timeout time
                        log("Timeout detected");
                        timeoutTimestamp.set(System.currentTimeMillis());
                    }
                },
                TIMEOUT_MS);

        // Make sure it didn't timeout immediately
        if (timeoutTimestamp.get() != -1)
            throw new AssertionError("Premature timeout detected.");

        // Use Sjavac object and wait less than TIMEOUT_MS in between calls
        Thread.sleep(TIMEOUT_MS - 1000);
        log("Compiling");
        service.compile(new String[0]);

        Thread.sleep(TIMEOUT_MS - 1000);
        log("Compiling");
        service.compile(new String[0]);

        if (timeoutTimestamp.get() != -1)
            throw new AssertionError("Premature timeout detected.");

        long expectedTimeout = System.currentTimeMillis() + TIMEOUT_MS;

        // Wait for actual timeout
        log("Awaiting idle timeout");
        Thread.sleep(TIMEOUT_MS + 1000);

        // Check result
        if (timeoutTimestamp.get() == -1)
            throw new AssertionError("Timeout never occurred");

        long error = Math.abs(expectedTimeout - timeoutTimestamp.get());
        log("Timeout error: " + error + " ms");
        String timeoutFactorText = System.getProperty("test.timeout.factor", "1.0");
        double timeoutFactor = Double.parseDouble(timeoutFactorText);
        double allowedError = TIMEOUT_MS * 0.1 * timeoutFactor;
        if (error > allowedError) {
            throw new AssertionError("Timeout error too large, error is " + error +
                                     " milliseconds, we allowed " + allowedError + " milliseconds");
        }
        log("Shutting down");
        service.shutdown();
    }

    private static void log(String msg) {
        long logTime = System.currentTimeMillis() - TEST_START;
        System.out.printf("After %5d ms: %s%n", logTime, msg);
    }

    private static class NoopJavacService implements Sjavac {
        @Override
        public void shutdown() {
        }
        @Override
        public Result compile(String[] args) {
            // Attempt to trigger idle timeout during a call by sleeping
            try {
                Thread.sleep(TIMEOUT_MS + 1000);
            } catch (InterruptedException e) {
            }
            return Result.OK;
        }
    }
}
