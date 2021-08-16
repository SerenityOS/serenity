/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8165000 8172547
 * @summary Verify no IOException on OS X for large timeout value in select()
 * and that timeout does not occur too early on Windows.
 * @requires (os.family == "mac" | os.family == "windows")
 */
import java.io.IOException;
import java.nio.channels.Selector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SelectTimeout {
    private static final long BIG_TIMEOUT    = 100_000_001_000L; // 8165000
    private static final long BIGGER_TIMEOUT = 850_000_000_000_000L; // 8172547
    private static final long SLEEP_MILLIS   = 10_000;

    public static void main(String[] args)
        throws IOException, InterruptedException {
        int failures = 0;
        long[] timeouts =
            new long[] {1, BIG_TIMEOUT/2, BIG_TIMEOUT - 1, BIG_TIMEOUT,
                BIGGER_TIMEOUT};
        for (long t : timeouts) {
            if (!test(t)) {
                failures++;
            }
        }
        if (failures > 0) {
            throw new RuntimeException("Test failed!");
        } else {
            System.out.println("Test succeeded");
        }
    }

    private static boolean test(final long timeout)
        throws InterruptedException, IOException {
        AtomicReference<Exception> theException =
            new AtomicReference<>();
        AtomicBoolean isTimedOut = new AtomicBoolean();

        Selector selector = Selector.open();

        Thread t = new Thread(() -> {
            try {
                selector.select(timeout);
                isTimedOut.set(true);
            } catch (IOException ioe) {
                theException.set(ioe);
            }
        });
        t.start();

        t.join(SLEEP_MILLIS);

        boolean result;
        if (theException.get() == null) {
            if (timeout > SLEEP_MILLIS && isTimedOut.get()) {
                System.err.printf("Test timed out early with timeout %d%n",
                    timeout);
                result = false;
            } else {
                System.out.printf("Test succeeded with timeout %d%n", timeout);
                result = true;
            }
        } else {
            System.err.printf("Test failed with timeout %d%n", timeout);
            theException.get().printStackTrace();
            result = false;
        }

        t.interrupt();
        selector.close();

        return result;
    }
}
