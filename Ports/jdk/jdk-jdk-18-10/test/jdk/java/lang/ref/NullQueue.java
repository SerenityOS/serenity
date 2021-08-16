/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4178693
 * @summary Ensure that null queue arguments don't crash the Reference handler
 * @author Mark Reinhold
 */

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;


public class NullQueue {

    private static Reference r = null;

    private static Thread findThread(String name) {
        /* Find reference-handler thread */
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        for (ThreadGroup tgn = tg;
             tgn != null;
             tg = tgn, tgn = tg.getParent());
        int nt = tg.activeCount();
        Thread[] ts = new Thread[nt];
        tg.enumerate(ts);
        Thread refHandler = null;
        for (int i = 0; i < ts.length; i++) {
            if (ts[i].getName().equals(name)) return ts[i];
        }
        return null;
    }

    private static void fork(Runnable proc) throws InterruptedException {
        Thread t = new Thread(proc);
        t.start();
        t.join();
    }

    public static void main(String[] args) throws Exception {

        Thread refHandler = findThread("Reference Handler");
        if (refHandler == null)
            throw new Exception("Couldn't find Reference-handler thread");
        if (!refHandler.isAlive())
            throw new Exception("Reference-handler thread is not alive");

        /* Invoke a Reference constructor, passing null for the queue */
        fork(new Runnable() {
            public void run() {
                r = new WeakReference(new Object(), null);
            }});

        /* Force the reference to be cleared and enqueued by the GC */
        for (int i = 0;; i++) {
            Thread.sleep(10);
            System.gc();
            if (r.get() == null) break;
            if (i >= 10)
                throw new Exception("Couldn't cause weak ref to be cleared");
        }

        /* Check that the handler is still alive */
        if (!refHandler.isAlive())
            throw new Exception("Reference-handler thread died");

    }

}
