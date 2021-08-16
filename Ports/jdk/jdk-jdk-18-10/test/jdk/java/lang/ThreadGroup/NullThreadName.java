/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6576763
 * @summary (thread) Thread constructors throw undocumented NPE for null name
 */

/*
 * Verify that threads constructed with a null thread name do not get added
 * to the list of unstarted thread for a thread group. We can do this by
 * checking that a daemon threadGroup is desroyed after its final valid thread
 * has completed.
 */

import java.util.concurrent.CountDownLatch;
import static java.lang.System.out;

public class NullThreadName
{
    static CountDownLatch done = new CountDownLatch(1);

    public static void main(String args[]) throws Exception {
        ThreadGroup tg = new ThreadGroup("chegar-threads");
        Thread goodThread = new Thread(tg, new GoodThread(), "goodThread");
        try {
            Thread badThread = new Thread(tg, new Runnable(){
                @Override
                public void run() {} }, null);
        } catch (NullPointerException npe) {
            out.println("OK, caught expected " + npe);
        }
        tg.setDaemon(true);
        goodThread.start();

        done.await();

        int count = 0;
        while (goodThread.isAlive()) {
            /* Hold off a little to allow the thread to complete */
            out.println("GoodThread still alive, sleeping...");
            try { Thread.sleep(2000); }
            catch (InterruptedException unused) {}

            /* do not wait forever - allow 120 seconds same as jtreg default timeout. */
            if (count++ > 60)
                throw new AssertionError("GoodThread is still alive!");
        }

        if (!tg.isDestroyed()) {
            throw new AssertionError("Failed: Thread group is not destroyed.");
        }
    }

    static class GoodThread implements Runnable
    {
        @Override
        public void run() {
            out.println("Good Thread started...");
            out.println("Good Thread finishing");
            done.countDown();
        }
    }
}
