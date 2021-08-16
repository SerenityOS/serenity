/*
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Martin Buchholz and Jason Mehrens with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @summary Only one thread should be created when a thread needs to
 * be kept alive to service a delayed task waiting in the queue.
 * @library /test/lib
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import jdk.test.lib.Utils;

public class ThreadRestarts {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);
    static final long FAR_FUTURE_MS = 10 * LONG_DELAY_MS;

    public static void main(String[] args) throws Exception {
        test(false);
        test(true);
    }

    private static void test(boolean allowTimeout) throws Exception {
        CountingThreadFactory ctf = new CountingThreadFactory();
        ScheduledThreadPoolExecutor stpe
            = new ScheduledThreadPoolExecutor(10, ctf);
        try {
            // schedule a dummy task in the "far future"
            Runnable nop = new Runnable() { public void run() {}};
            stpe.schedule(nop, FAR_FUTURE_MS, MILLISECONDS);
            stpe.setKeepAliveTime(1L, MILLISECONDS);
            stpe.allowCoreThreadTimeOut(allowTimeout);
            MILLISECONDS.sleep(12L);
        } finally {
            stpe.shutdownNow();
            if (!stpe.awaitTermination(LONG_DELAY_MS, MILLISECONDS))
                throw new AssertionError("timed out");
        }
        if (ctf.count.get() > 1)
            throw new AssertionError(
                String.format("%d threads created, 1 expected",
                              ctf.count.get()));
    }

    static class CountingThreadFactory implements ThreadFactory {
        final AtomicLong count = new AtomicLong(0L);

        public Thread newThread(Runnable r) {
            count.getAndIncrement();
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    }
}
